package com.example.musebox.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.musebox.models.Song;

import java.util.ArrayList;
import java.util.List;

public class SongDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "musebox.db";
    private static final int DATABASE_VERSION = 3; // Incremented for album cover column

    private static final String TABLE_SONGS = "songs";
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_URI = "uri";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_ALBUM_COVER_PATH = "album_cover_path";

    // Favorites table
    private static final String TABLE_FAVORITES = "favorites";
    private static final String KEY_SONG_ID = "song_id";
    private static final String KEY_ADDED_TIME = "added_time";

    public SongDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_SONGS_TABLE = "CREATE TABLE " + TABLE_SONGS + "("
                + KEY_ID + " TEXT PRIMARY KEY,"
                + KEY_TITLE + " TEXT,"
                + KEY_ARTIST + " TEXT,"
                + KEY_URI + " TEXT,"
                + KEY_DURATION + " INTEGER,"
                + KEY_ALBUM_COVER_PATH + " TEXT"
                + ")";
        db.execSQL(CREATE_SONGS_TABLE);

        // Create index on title column for faster sorting
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_song_title ON " + TABLE_SONGS + "(" + KEY_TITLE + ")");

        // Create favorites table
        String CREATE_FAVORITES_TABLE = "CREATE TABLE " + TABLE_FAVORITES + "("
                + KEY_SONG_ID + " TEXT PRIMARY KEY,"
                + KEY_ADDED_TIME + " INTEGER,"
                + "FOREIGN KEY(" + KEY_SONG_ID + ") REFERENCES " + TABLE_SONGS + "(" + KEY_ID + ")"
                + ")";
        db.execSQL(CREATE_FAVORITES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Add index if upgrading
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_song_title ON " + TABLE_SONGS + "(" + KEY_TITLE + ")");

        // Add favorites table if upgrading from version 1
        if (oldVersion < 2) {
            String CREATE_FAVORITES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_FAVORITES + "("
                    + KEY_SONG_ID + " TEXT PRIMARY KEY,"
                    + KEY_ADDED_TIME + " INTEGER,"
                    + "FOREIGN KEY(" + KEY_SONG_ID + ") REFERENCES " + TABLE_SONGS + "(" + KEY_ID + ")"
                    + ")";
            db.execSQL(CREATE_FAVORITES_TABLE);
        }

        // Add album cover path column if upgrading from version 2
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_SONGS + " ADD COLUMN " + KEY_ALBUM_COVER_PATH + " TEXT");
        }
    }

    // Method to ensure index exists even for existing databases
    public void ensureIndexExists() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_song_title ON " + TABLE_SONGS + "(" + KEY_TITLE + ")");
    }

    public void addSong(Song song) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ID, song.getId());
        values.put(KEY_TITLE, song.getTitle());
        values.put(KEY_ARTIST, song.getArtist());
        values.put(KEY_URI, song.getUri());
        values.put(KEY_DURATION, song.getDuration());
        values.put(KEY_ALBUM_COVER_PATH, song.getAlbumCoverPath());
        db.insert(TABLE_SONGS, null, values);
        // Don't close db - let SQLiteOpenHelper manage the connection pool
    }

    // Check if song already exists by URI (path)
    public boolean songExists(String uri) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_SONGS + " WHERE " + KEY_URI + " = ?",
                new String[] { uri });
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        // Don't close db - let SQLiteOpenHelper manage the connection pool
        return exists;
    }

    // Add song only if it doesn't exist, returns true if added, also updates album
    // art for existing songs
    public boolean addSongIfNotExists(Song song) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + KEY_ALBUM_COVER_PATH + " FROM " + TABLE_SONGS + " WHERE " + KEY_URI + " = ? LIMIT 1",
                new String[] { song.getUri() });

        boolean exists = cursor.moveToFirst();
        String existingAlbumArt = exists ? cursor.getString(0) : null;
        cursor.close();

        if (!exists) {
            addSong(song);
            return true; // Song was added
        } else {
            // Song exists - check if we should update album art
            if ((existingAlbumArt == null || existingAlbumArt.isEmpty()) &&
                    song.getAlbumCoverPath() != null && !song.getAlbumCoverPath().isEmpty()) {
                // Update existing song with new album art
                updateSong(song);
                android.util.Log.d("DatabaseUpdate", "Updated album art for existing song: " + song.getTitle());
            }
            return false; // Song already existed (but may have been updated)
        }
    }

    // Optimized batch insert - much faster for importing many songs
    // Returns array: [newCount, duplicateCount]
    public int[] addSongsIfNotExistBatch(List<Song> songs) {
        int newCount = 0;
        int duplicateCount = 0;

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Song song : songs) {
                // Check if exists and get current album art path
                Cursor cursor = db.rawQuery(
                        "SELECT " + KEY_ALBUM_COVER_PATH + " FROM " + TABLE_SONGS + " WHERE " + KEY_URI
                                + " = ? LIMIT 1",
                        new String[] { song.getUri() });

                boolean exists = cursor.moveToFirst();
                String existingAlbumArt = exists ? cursor.getString(0) : null;
                cursor.close();

                if (!exists) {
                    // Insert the song
                    ContentValues values = new ContentValues();
                    values.put(KEY_ID, song.getId());
                    values.put(KEY_TITLE, song.getTitle());
                    values.put(KEY_ARTIST, song.getArtist());
                    values.put(KEY_URI, song.getUri());
                    values.put(KEY_DURATION, song.getDuration());
                    values.put(KEY_ALBUM_COVER_PATH, song.getAlbumCoverPath());
                    db.insert(TABLE_SONGS, null, values);
                    newCount++;
                } else {
                    // Song exists - check if we should update album art
                    if ((existingAlbumArt == null || existingAlbumArt.isEmpty()) &&
                            song.getAlbumCoverPath() != null && !song.getAlbumCoverPath().isEmpty()) {
                        // Update existing song with new album art
                        ContentValues values = new ContentValues();
                        values.put(KEY_ALBUM_COVER_PATH, song.getAlbumCoverPath());
                        db.update(TABLE_SONGS, values, KEY_URI + " = ?", new String[] { song.getUri() });
                        android.util.Log.d("DatabaseUpdate", "Updated album art for existing song: " + song.getTitle());
                    }
                    duplicateCount++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            // Don't close db - let SQLiteOpenHelper manage the connection pool
        }

        return new int[] { newCount, duplicateCount };
    }

    // Get total count of songs
    public int getSongCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SONGS, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        // Don't close db - let SQLiteOpenHelper manage the connection pool
        return count;
    }

    // Get songs with pagination (limit and offset)
    public List<Song> getSongsPaginated(int limit, int offset) {
        List<Song> songs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_SONGS + " ORDER BY " + KEY_TITLE + " ASC LIMIT ? OFFSET ?",
                new String[] { String.valueOf(limit), String.valueOf(offset) });

        if (cursor.moveToFirst()) {
            do {
                Song song = new Song(
                        cursor.getString(0), // id
                        cursor.getString(1), // title
                        cursor.getString(2), // artist
                        cursor.getString(3), // uri
                        cursor.getLong(4), // duration
                        cursor.getString(5) // album_cover_path
                );
                songs.add(song);
            } while (cursor.moveToNext());
        }
        cursor.close();
        // Don't close db - let SQLiteOpenHelper manage the connection pool
        return songs;
    }
    //
    // public long insertSong(Song song) {
    // SQLiteDatabase db = this.getWritableDatabase();
    // ContentValues values = new ContentValues();
    // values.put(COLUMN_TITLE, song.getTitle());
    // values.put(COLUMN_ARTIST, song.getArtist());
    // values.put(COLUMN_URI, song.getUri());
    // values.put(COLUMN_DURATION, song.getDuration());
    // return db.insert(TABLE_SONGS, null, values);
    // }

    public List<Song> getAllSongs() {
        List<Song> songList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SONGS, null);

        if (cursor.moveToFirst()) {
            do {
                Song song = new Song(
                        cursor.getString(0), // id
                        cursor.getString(1), // title
                        cursor.getString(2), // artist
                        cursor.getString(3), // uri
                        cursor.getLong(4), // duration
                        cursor.getString(5) // album_cover_path (may be null)
                );
                songList.add(song);
            } while (cursor.moveToNext());
        }

        cursor.close();
        // Don't close db - let SQLiteOpenHelper manage the connection pool
        return songList;
    }

    public void deleteAllSongs() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SONGS, null, null);
        // Don't close db - let SQLiteOpenHelper manage the connection pool
    }

    /** Delete a single song by ID */
    public void deleteSong(String songId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete from songs table
        db.delete(TABLE_SONGS, KEY_ID + " = ?", new String[] { songId });
        // Also remove from favorites if exists
        db.delete(TABLE_FAVORITES, KEY_SONG_ID + " = ?", new String[] { songId });
        // Don't close db - let SQLiteOpenHelper manage the connection pool
    }

    // ========== FAVORITES METHODS ==========

    /** Add song to favorites */
    public boolean addToFavorites(String songId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_SONG_ID, songId);
        values.put(KEY_ADDED_TIME, System.currentTimeMillis());

        long result = db.insert(TABLE_FAVORITES, null, values);
        // Don't close db - let SQLiteOpenHelper manage the connection pool
        return result != -1;
    }

    /** Remove song from favorites */
    public boolean removeFromFavorites(String songId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_FAVORITES, KEY_SONG_ID + " = ?", new String[] { songId });
        // Don't close db - let SQLiteOpenHelper manage the connection pool
        return result > 0;
    }

    /** Check if song is in favorites */
    public boolean isFavorite(String songId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT 1 FROM " + TABLE_FAVORITES + " WHERE " + KEY_SONG_ID + " = ? LIMIT 1",
                new String[] { songId });

        boolean isFav = cursor.moveToFirst();
        cursor.close();
        // Don't close db - let SQLiteOpenHelper manage the connection pool
        return isFav;
    }

    /** Get all favorite songs */
    public List<Song> getFavoriteSongs() {
        List<Song> favoriteSongs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Join songs and favorites tables, order by most recently added
        String query = "SELECT s.* FROM " + TABLE_SONGS + " s " +
                "INNER JOIN " + TABLE_FAVORITES + " f ON s." + KEY_ID + " = f." + KEY_SONG_ID +
                " ORDER BY f." + KEY_ADDED_TIME + " DESC";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Song song = new Song(
                        cursor.getString(0), // id
                        cursor.getString(1), // title
                        cursor.getString(2), // artist
                        cursor.getString(3), // uri
                        cursor.getLong(4), // duration
                        cursor.getString(5) // album_cover_path
                );
                song.setFavorite(true); // Mark as favorite
                favoriteSongs.add(song);
            } while (cursor.moveToNext());
        }

        cursor.close();
        // Don't close db - let SQLiteOpenHelper manage the connection pool
        return favoriteSongs;
    }

    /** Get count of favorite songs */
    public int getFavoritesCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_FAVORITES, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        // Don't close db - let SQLiteOpenHelper manage the connection pool
        return count;
    }

    /** Toggle favorite status - returns new status (true if now favorite) */
    public boolean toggleFavorite(String songId) {
        if (isFavorite(songId)) {
            removeFromFavorites(songId);
            return false;
        } else {
            addToFavorites(songId);
            return true;
        }
    }

    /** Update an existing song in the database */
    public boolean updateSong(Song song) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_TITLE, song.getTitle());
        values.put(KEY_ARTIST, song.getArtist());
        values.put(KEY_URI, song.getUri());
        values.put(KEY_DURATION, song.getDuration());
        values.put(KEY_ALBUM_COVER_PATH, song.getAlbumCoverPath());

        int result = db.update(TABLE_SONGS, values, KEY_ID + " = ?", new String[] { song.getId() });
        // Don't close db - let SQLiteOpenHelper manage the connection pool
        return result > 0;
    }

    /**
     * Search songs by query string (searches in title and artist)
     * 
     * @param query     Search query
     * @param sortOrder Sort order (title, artist, duration, or recent)
     * @return List of matching songs
     */
    public List<Song> searchSongs(String query, String sortOrder) {
        List<Song> songList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String orderByClause;
        switch (sortOrder.toLowerCase()) {
            case "artist":
                orderByClause = KEY_ARTIST + " ASC, " + KEY_TITLE + " ASC";
                break;
            case "duration":
                orderByClause = KEY_DURATION + " DESC";
                break;
            case "recent":
                orderByClause = "rowid DESC"; // Most recently added
                break;
            case "title":
            default:
                orderByClause = KEY_TITLE + " ASC";
                break;
        }

        String selection = KEY_TITLE + " LIKE ? OR " + KEY_ARTIST + " LIKE ?";
        String[] selectionArgs = new String[] { "%" + query + "%", "%" + query + "%" };

        Cursor cursor = db.query(
                TABLE_SONGS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                orderByClause);

        if (cursor.moveToFirst()) {
            do {
                Song song = new Song(
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_ARTIST)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_URI)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DURATION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_ALBUM_COVER_PATH)));
                songList.add(song);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return songList;
    }

    /**
     * Get all songs with sorting
     * 
     * @param sortOrder Sort order (title, artist, duration, or recent)
     * @return Sorted list of all songs
     */
    public List<Song> getAllSongsSorted(String sortOrder) {
        List<Song> songList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String orderByClause;
        switch (sortOrder.toLowerCase()) {
            case "artist":
                orderByClause = KEY_ARTIST + " ASC, " + KEY_TITLE + " ASC";
                break;
            case "duration":
                orderByClause = KEY_DURATION + " DESC";
                break;
            case "recent":
                orderByClause = "rowid DESC"; // Most recently added
                break;
            case "title":
            default:
                orderByClause = KEY_TITLE + " ASC";
                break;
        }

        Cursor cursor = db.query(
                TABLE_SONGS,
                null,
                null,
                null,
                null,
                null,
                orderByClause);

        if (cursor.moveToFirst()) {
            do {
                Song song = new Song(
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_ARTIST)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_URI)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DURATION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_ALBUM_COVER_PATH)));
                songList.add(song);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return songList;
    }

    /**
     * Get songs filtered by artist
     * 
     * @param artist    Artist name
     * @param sortOrder Sort order
     * @return List of songs by the artist
     */
    public List<Song> getSongsByArtist(String artist, String sortOrder) {
        List<Song> songList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String orderByClause;
        switch (sortOrder.toLowerCase()) {
            case "duration":
                orderByClause = KEY_DURATION + " DESC";
                break;
            case "recent":
                orderByClause = "rowid DESC";
                break;
            case "title":
            default:
                orderByClause = KEY_TITLE + " ASC";
                break;
        }

        String selection = KEY_ARTIST + " = ?";
        String[] selectionArgs = new String[] { artist };

        Cursor cursor = db.query(
                TABLE_SONGS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                orderByClause);

        if (cursor.moveToFirst()) {
            do {
                Song song = new Song(
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_ARTIST)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_URI)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DURATION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_ALBUM_COVER_PATH)));
                songList.add(song);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return songList;
    }

    /**
     * Get list of unique artists
     * 
     * @return List of artist names
     */
    public List<String> getAllArtists() {
        List<String> artistList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                true, // distinct
                TABLE_SONGS,
                new String[] { KEY_ARTIST },
                null,
                null,
                null,
                null,
                KEY_ARTIST + " ASC",
                null);

        if (cursor.moveToFirst()) {
            do {
                String artist = cursor.getString(0);
                if (artist != null && !artist.isEmpty()) {
                    artistList.add(artist);
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
        return artistList;
    }
}
