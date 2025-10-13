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

public class PlaylistDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "musebox.db";
    private static final int DATABASE_VERSION = 3; // Incremented for playlists feature

    // Table names
    private static final String TABLE_PLAYLISTS = "playlists";
    private static final String TABLE_PLAYLIST_SONGS = "playlist_songs";

    // Playlist columns
    private static final String KEY_PLAYLIST_ID = "id";
    private static final String KEY_PLAYLIST_NAME = "name";
    private static final String KEY_CREATED_AT = "created_at";

    // Playlist-songs mapping columns
    private static final String KEY_PS_PLAYLIST_ID = "playlist_id";
    private static final String KEY_PS_SONG_ID = "song_id";
    private static final String KEY_POSITION = "position";

    public PlaylistDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_PLAYLISTS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_PLAYLISTS + " ("
                + KEY_PLAYLIST_ID + " TEXT PRIMARY KEY,"
                + KEY_PLAYLIST_NAME + " TEXT UNIQUE,"
                + KEY_CREATED_AT + " INTEGER"
                + ")";
        db.execSQL(CREATE_PLAYLISTS_TABLE);

        String CREATE_PLAYLIST_SONGS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_PLAYLIST_SONGS + " ("
                + KEY_PS_PLAYLIST_ID + " TEXT,"
                + KEY_PS_SONG_ID + " TEXT,"
                + KEY_POSITION + " INTEGER,"
                + "PRIMARY KEY(" + KEY_PS_PLAYLIST_ID + ", " + KEY_PS_SONG_ID + "),"
                + "FOREIGN KEY(" + KEY_PS_PLAYLIST_ID + ") REFERENCES " + TABLE_PLAYLISTS + "(" + KEY_PLAYLIST_ID + ") ON DELETE CASCADE,"
                + "FOREIGN KEY(" + KEY_PS_SONG_ID + ") REFERENCES songs(id) ON DELETE CASCADE"
                + ")";
        db.execSQL(CREATE_PLAYLIST_SONGS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            onCreate(db);
        }
    }

    // CREATE NEW PLAYLIST
    public boolean createPlaylist(Playlist playlist) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PLAYLIST_ID, playlist.getId());
        values.put(KEY_PLAYLIST_NAME, playlist.getName());
        values.put(KEY_CREATED_AT, playlist.getCreatedAt());

        long result = db.insert(TABLE_PLAYLISTS, null, values);
        return result != -1;
    }

    // DELETE PLAYLIST
    public boolean deletePlaylist(String playlistId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_PLAYLISTS, KEY_PLAYLIST_ID + " = ?", new String[]{playlistId});
        return rows > 0;
    }

    // RENAME PLAYLIST
    public boolean renamePlaylist(String playlistId, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PLAYLIST_NAME, newName);
        int rows = db.update(TABLE_PLAYLISTS, values, KEY_PLAYLIST_ID + " = ?", new String[]{playlistId});
        return rows > 0;
    }

    // GET ALL PLAYLISTS
    public List<Playlist> getAllPlaylists() {
        List<Playlist> playlists = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PLAYLISTS + " ORDER BY " + KEY_CREATED_AT + " DESC", null);
        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PLAYLIST_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PLAYLIST_NAME));
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_CREATED_AT));

                playlists.add(new Playlist(id, name, new ArrayList<>(), createdAt));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return playlists;
    }

    // GET PLAYLIST WITH THEIR SONGS
    public Playlist getPlaylistWithSongs(String playlistId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Playlist playlist = null;

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PLAYLISTS + " WHERE " + KEY_PLAYLIST_ID + " = ?",
                new String[]{playlistId});

        if (cursor.moveToFirst()) {
            String id = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PLAYLIST_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PLAYLIST_NAME));
            long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_CREATED_AT));
            List<Song> songs = getSongsInPlaylist(id);
            playlist = new Playlist(id, name, songs, createdAt);
        }

        cursor.close();
        return playlist;
    }

    //SONG LINK METHODS

    //ADD SONGS TO PLAYLIST
    public boolean addSongToPlaylist(String playlistId, Song song) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Prevent duplicates
        Cursor cursor = db.rawQuery(
                "SELECT 1 FROM " + TABLE_PLAYLIST_SONGS + " WHERE " + KEY_PS_PLAYLIST_ID + "=? AND " + KEY_PS_SONG_ID + "=?",
                new String[]{playlistId, song.getId()}
        );
        boolean exists = cursor.moveToFirst();
        cursor.close();
        if (exists) return false;

        ContentValues values = new ContentValues();
        values.put(KEY_PS_PLAYLIST_ID, playlistId);
        values.put(KEY_PS_SONG_ID, song.getId());
        values.put(KEY_POSITION, getNextSongPosition(playlistId));

        long result = db.insert(TABLE_PLAYLIST_SONGS, null, values);
        return result != -1;
    }

    // REMOVE SONGS FROM PLAYLIST
    public boolean removeSongFromPlaylist(String playlistId, String songId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_PLAYLIST_SONGS,
                KEY_PS_PLAYLIST_ID + "=? AND " + KEY_PS_SONG_ID + "=?",
                new String[]{playlistId, songId});
        return rows > 0;
    }

    // GET ALL SONGS IN THE PLAYLIST
    public List<Song> getSongsInPlaylist(String playlistId) {
        List<Song> songs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT s.* FROM songs s " +
                "INNER JOIN " + TABLE_PLAYLIST_SONGS + " ps ON s.id = ps." + KEY_PS_SONG_ID +
                " WHERE ps." + KEY_PS_PLAYLIST_ID + "=? " +
                " ORDER BY ps." + KEY_POSITION + " ASC";

        Cursor cursor = db.rawQuery(query, new String[]{playlistId});
        if (cursor.moveToFirst()) {
            do {
                Song song = new Song(
                        cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        cursor.getString(cursor.getColumnIndexOrThrow("artist")),
                        cursor.getString(cursor.getColumnIndexOrThrow("uri")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("duration"))
                );
                songs.add(song);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }

    // COUNT SONGS IN THE PLAYLIST
    public int getPlaylistSongCount(String playlistId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_PLAYLIST_SONGS + " WHERE " + KEY_PS_PLAYLIST_ID + "=?",
                new String[]{playlistId});
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    // GET NEXT SONG POSITION IN PLAYLIST
    private int getNextSongPosition(String playlistId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT MAX(" + KEY_POSITION + ") FROM " + TABLE_PLAYLIST_SONGS + " WHERE " + KEY_PS_PLAYLIST_ID + "=?",
                new String[]{playlistId});
        int pos = 0;
        if (cursor.moveToFirst()) pos = cursor.getInt(0);
        cursor.close();
        return pos + 1;
    }
}
