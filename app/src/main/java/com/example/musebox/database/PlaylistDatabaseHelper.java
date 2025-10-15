package com.example.musebox.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.musebox.models.Playlist;
import com.example.musebox.models.Song;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlaylistDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "musebox.db";
    // Keep the same version as SongDatabaseHelper to avoid DB upgrade conflicts
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_PLAYLISTS = "playlists";
    private static final String KEY_PLAYLIST_ID = "playlist_id";
    private static final String KEY_PLAYLIST_NAME = "name";
    private static final String KEY_PLAYLIST_DESCRIPTION = "description";
    private static final String KEY_PLAYLIST_CREATED = "created_time";

    private static final String TABLE_PLAYLIST_SONGS = "playlist_songs";
    private static final String KEY_SONG_ID = "song_id";
    private static final String KEY_POSITION = "position"; // order in playlist

    public PlaylistDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create playlists table
        String CREATE_PLAYLISTS = "CREATE TABLE IF NOT EXISTS " + TABLE_PLAYLISTS + " ("
                + KEY_PLAYLIST_ID + " TEXT PRIMARY KEY,"
                + KEY_PLAYLIST_NAME + " TEXT,"
                + KEY_PLAYLIST_DESCRIPTION + " TEXT,"
                + KEY_PLAYLIST_CREATED + " INTEGER"
                + ")";
        db.execSQL(CREATE_PLAYLISTS);

        // Create playlist_songs mapping table
        String CREATE_PLAYLIST_SONGS = "CREATE TABLE IF NOT EXISTS " + TABLE_PLAYLIST_SONGS + " ("
                + KEY_PLAYLIST_ID + " TEXT,"
                + KEY_SONG_ID + " TEXT,"
                + KEY_POSITION + " INTEGER," 
                + "PRIMARY KEY (" + KEY_PLAYLIST_ID + ", " + KEY_SONG_ID + ")"
                + ")";
        db.execSQL(CREATE_PLAYLIST_SONGS);

        // Index for faster lookups
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_playlist_name ON " + TABLE_PLAYLISTS + "(" + KEY_PLAYLIST_NAME + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_playlist_songs_pos ON " + TABLE_PLAYLIST_SONGS + "(" + KEY_PLAYLIST_ID + ", " + KEY_POSITION + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Ensure tables exist when upgrading from older versions
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PLAYLISTS + " ("
                + KEY_PLAYLIST_ID + " TEXT PRIMARY KEY,"
                + KEY_PLAYLIST_NAME + " TEXT,"
                + KEY_PLAYLIST_DESCRIPTION + " TEXT,"
                + KEY_PLAYLIST_CREATED + " INTEGER"
                + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PLAYLIST_SONGS + " ("
                + KEY_PLAYLIST_ID + " TEXT,"
                + KEY_SONG_ID + " TEXT,"
                + KEY_POSITION + " INTEGER,"
                + "PRIMARY KEY (" + KEY_PLAYLIST_ID + ", " + KEY_SONG_ID + ")"
                + ")");

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_playlist_name ON " + TABLE_PLAYLISTS + "(" + KEY_PLAYLIST_NAME + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_playlist_songs_pos ON " + TABLE_PLAYLIST_SONGS + "(" + KEY_PLAYLIST_ID + ", " + KEY_POSITION + ")");
    }

    // Ensure tables exist for existing databases where onCreate won't be called
    public void ensureTablesExist() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PLAYLISTS + " ("
                + KEY_PLAYLIST_ID + " TEXT PRIMARY KEY,"
                + KEY_PLAYLIST_NAME + " TEXT,"
                + KEY_PLAYLIST_DESCRIPTION + " TEXT,"
                + KEY_PLAYLIST_CREATED + " INTEGER"
                + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PLAYLIST_SONGS + " ("
                + KEY_PLAYLIST_ID + " TEXT,"
                + KEY_SONG_ID + " TEXT,"
                + KEY_POSITION + " INTEGER,"
                + "PRIMARY KEY (" + KEY_PLAYLIST_ID + ", " + KEY_SONG_ID + ")"
                + ")");

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_playlist_name ON " + TABLE_PLAYLISTS + "(" + KEY_PLAYLIST_NAME + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_playlist_songs_pos ON " + TABLE_PLAYLIST_SONGS + "(" + KEY_PLAYLIST_ID + ", " + KEY_POSITION + ")");
    }

    /** Create a new playlist and return its generated id */
    public String createPlaylist(String name, String description) {
        String id = UUID.randomUUID().toString();
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PLAYLIST_ID, id);
        values.put(KEY_PLAYLIST_NAME, name);
        values.put(KEY_PLAYLIST_DESCRIPTION, description);
        values.put(KEY_PLAYLIST_CREATED, System.currentTimeMillis());

        long res = db.insert(TABLE_PLAYLISTS, null, values);
        return res == -1 ? null : id;
    }

    public boolean renamePlaylist(String playlistId, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PLAYLIST_NAME, newName);
        int rows = db.update(TABLE_PLAYLISTS, values, KEY_PLAYLIST_ID + " = ?", new String[]{playlistId});
        return rows > 0;
    }

    public boolean updatePlaylistDescription(String playlistId, String description) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PLAYLIST_DESCRIPTION, description);
        int rows = db.update(TABLE_PLAYLISTS, values, KEY_PLAYLIST_ID + " = ?", new String[]{playlistId});
        return rows > 0;
    }

    public boolean deletePlaylist(String playlistId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete mappings first
        db.delete(TABLE_PLAYLIST_SONGS, KEY_PLAYLIST_ID + " = ?", new String[]{playlistId});
        int rows = db.delete(TABLE_PLAYLISTS, KEY_PLAYLIST_ID + " = ?", new String[]{playlistId});
        return rows > 0;
    }

    public List<Playlist> getAllPlaylists() {
        List<Playlist> playlists = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_PLAYLIST_ID + ", " + KEY_PLAYLIST_NAME + ", " + KEY_PLAYLIST_DESCRIPTION + " FROM " + TABLE_PLAYLISTS + " ORDER BY " + KEY_PLAYLIST_NAME + " COLLATE NOCASE ASC", null);

        if (cursor.moveToFirst()) {
            do {
                Playlist playlist = new Playlist(cursor.getString(0), cursor.getString(1), cursor.getString(2));
                playlists.add(playlist);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return playlists;
    }

    public Playlist getPlaylistById(String playlistId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_PLAYLIST_ID + ", " + KEY_PLAYLIST_NAME + ", " + KEY_PLAYLIST_DESCRIPTION + " FROM " + TABLE_PLAYLISTS + " WHERE " + KEY_PLAYLIST_ID + " = ? LIMIT 1", new String[]{playlistId});
        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        Playlist playlist = new Playlist(cursor.getString(0), cursor.getString(1), cursor.getString(2));
        cursor.close();
        return playlist;
    }

    /** Add a song to a playlist (appends to the end) */
    public boolean addSongToPlaylist(String playlistId, Song song) {
        if (playlistId == null || song == null) return false;
        SQLiteDatabase db = this.getWritableDatabase();

        // Check for existing mapping
        Cursor check = db.rawQuery("SELECT 1 FROM " + TABLE_PLAYLIST_SONGS + " WHERE " + KEY_PLAYLIST_ID + " = ? AND " + KEY_SONG_ID + " = ? LIMIT 1", new String[]{playlistId, song.getId()});
        boolean exists = check.moveToFirst();
        check.close();
        if (exists) return false;

        // Determine next position
        Cursor posCursor = db.rawQuery("SELECT MAX(" + KEY_POSITION + ") FROM " + TABLE_PLAYLIST_SONGS + " WHERE " + KEY_PLAYLIST_ID + " = ?", new String[]{playlistId});
        int nextPos = 0;
        if (posCursor.moveToFirst()) {
            int max = posCursor.isNull(0) ? -1 : posCursor.getInt(0);
            nextPos = max + 1;
        }
        posCursor.close();

        ContentValues values = new ContentValues();
        values.put(KEY_PLAYLIST_ID, playlistId);
        values.put(KEY_SONG_ID, song.getId());
        values.put(KEY_POSITION, nextPos);

        long res = db.insert(TABLE_PLAYLIST_SONGS, null, values);
        return res != -1;
    }

    public boolean removeSongFromPlaylist(String playlistId, String songId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_PLAYLIST_SONGS, KEY_PLAYLIST_ID + " = ? AND " + KEY_SONG_ID + " = ?", new String[]{playlistId, songId});
        return rows > 0;
    }

    /** Get songs for a playlist in order */
    public List<Song> getSongsForPlaylist(String playlistId) {
        List<Song> songs = new ArrayList<>();
        if (playlistId == null) return songs;

        SQLiteDatabase db = this.getReadableDatabase();
        // Join with songs table to get song metadata; assume songs table exists (SongDatabaseHelper)
        String query = "SELECT s.* FROM playlist_songs ps INNER JOIN songs s ON ps.song_id = s.id "
                + "WHERE ps.playlist_id = ? ORDER BY ps." + KEY_POSITION + " ASC";

        Cursor cursor = db.rawQuery(query, new String[]{playlistId});
        if (cursor.moveToFirst()) {
            do {
                Song song = new Song(
                        cursor.getString(0), // id
                        cursor.getString(1), // title
                        cursor.getString(2), // artist
                        cursor.getString(3), // uri
                        cursor.getLong(4),   // duration
                        cursor.getString(5)  // album_cover_path
                );
                songs.add(song);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }

    public int getPlaylistCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PLAYLISTS, null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public boolean playlistExists(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_PLAYLISTS + " WHERE " + KEY_PLAYLIST_NAME + " = ? LIMIT 1", new String[]{name});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    /** Clear all songs from a playlist but keep the playlist itself */
    public void clearPlaylist(String playlistId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PLAYLIST_SONGS, KEY_PLAYLIST_ID + " = ?", new String[]{playlistId});
    }

    /** Reorder songs inside a playlist: provide list of songIds in desired order */
    public boolean reorderPlaylist(String playlistId, List<String> orderedSongIds) {
        if (playlistId == null || orderedSongIds == null) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            int pos = 0;
            for (String songId : orderedSongIds) {
                ContentValues values = new ContentValues();
                values.put(KEY_POSITION, pos);
                db.update(TABLE_PLAYLIST_SONGS, values, KEY_PLAYLIST_ID + " = ? AND " + KEY_SONG_ID + " = ?", new String[]{playlistId, songId});
                pos++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return true;
    }

    /** Add multiple songs to playlist preserving order of passed list. Skips duplicates. */
    public int addSongsToPlaylistBatch(String playlistId, List<Song> songsList) {
        if (playlistId == null || songsList == null || songsList.isEmpty()) return 0;
        SQLiteDatabase db = this.getWritableDatabase();
        int added = 0;
        db.beginTransaction();
        try {
            // Get current max position
            Cursor posCursor = db.rawQuery("SELECT MAX(" + KEY_POSITION + ") FROM " + TABLE_PLAYLIST_SONGS + " WHERE " + KEY_PLAYLIST_ID + " = ?", new String[]{playlistId});
            int nextPos = 0;
            if (posCursor.moveToFirst()) {
                int max = posCursor.isNull(0) ? -1 : posCursor.getInt(0);
                nextPos = max + 1;
            }
            posCursor.close();

            for (Song song : songsList) {
                Cursor check = db.rawQuery("SELECT 1 FROM " + TABLE_PLAYLIST_SONGS + " WHERE " + KEY_PLAYLIST_ID + " = ? AND " + KEY_SONG_ID + " = ? LIMIT 1", new String[]{playlistId, song.getId()});
                boolean exists = check.moveToFirst();
                check.close();
                if (exists) continue;

                ContentValues values = new ContentValues();
                values.put(KEY_PLAYLIST_ID, playlistId);
                values.put(KEY_SONG_ID, song.getId());
                values.put(KEY_POSITION, nextPos);
                long res = db.insert(TABLE_PLAYLIST_SONGS, null, values);
                if (res != -1) {
                    added++;
                    nextPos++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return added;
    }

    /**
     * Get a Playlist object with its songs list populated.
     */
    public Playlist getPlaylistWithSongs(String playlistId) {
        Playlist playlist = getPlaylistById(playlistId);
        if (playlist == null) return null;
        List<Song> songs = getSongsForPlaylist(playlistId);
        playlist.setSongs(songs);
        return playlist;
    }
}
