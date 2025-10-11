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
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_SONGS = "songs";
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_URI = "uri";
    private static final String KEY_DURATION = "duration";

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
                + KEY_DURATION + " INTEGER"
                + ")";
        db.execSQL(CREATE_SONGS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONGS);
        onCreate(db);
    }

    public void addSong(Song song) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ID, song.getId());
        values.put(KEY_TITLE, song.getTitle());
        values.put(KEY_ARTIST, song.getArtist());
        values.put(KEY_URI, song.getUri());
        values.put(KEY_DURATION, song.getDuration());
        db.insert(TABLE_SONGS, null, values);
        db.close();
    }

    // Check if song already exists by URI (path)
    public boolean songExists(String uri) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT * FROM " + TABLE_SONGS + " WHERE " + KEY_URI + " = ?",
            new String[]{uri}
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    // Add song only if it doesn't exist, returns true if added
    public boolean addSongIfNotExists(Song song) {
        if (songExists(song.getUri())) {
            return false; // Song already exists, not added
        }
        addSong(song);
        return true; // Song was added
    }
//
//    public long insertSong(Song song) {
//        SQLiteDatabase db = this.getWritableDatabase();
//        ContentValues values = new ContentValues();
//        values.put(COLUMN_TITLE, song.getTitle());
//        values.put(COLUMN_ARTIST, song.getArtist());
//        values.put(COLUMN_URI, song.getUri());
//        values.put(COLUMN_DURATION, song.getDuration());
//        return db.insert(TABLE_SONGS, null, values);
//    }

    public List<Song> getAllSongs() {
        List<Song> songList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SONGS, null);

        if (cursor.moveToFirst()) {
            do {
                Song song = new Song(
                        cursor.getString(0),  // id
                        cursor.getString(1),  // title
                        cursor.getString(2),  // artist
                        cursor.getString(3),  // uri
                        cursor.getLong(4)     // duration
                );
                songList.add(song);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return songList;
    }

    public void deleteAllSongs() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SONGS, null, null);
        db.close();
    }
}