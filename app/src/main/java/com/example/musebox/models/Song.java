package com.example.musebox.models;

import java.util.UUID;

public class Song {
    private String id;
    private String title;
    private String artist;
    private String uri;
    private long duration;
    private boolean isFavorite;
    private String albumCoverPath;

    public Song(String title, String artist, String uri, long duration) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.artist = artist;
        this.uri = uri;
        this.duration = duration;
        this.isFavorite = false;
        this.albumCoverPath = null;
    }

    public Song(String id, String title, String artist, String uri, long duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.uri = uri;
        this.duration = duration;
        this.isFavorite = false;
        this.albumCoverPath = null;
    }

    public Song(String id, String title, String artist, String uri, long duration, String albumCoverPath) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.uri = uri;
        this.duration = duration;
        this.isFavorite = false;
        this.albumCoverPath = albumCoverPath;
    }

    public Song(String title, String artist, String uri, long duration, String albumCoverPath) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.artist = artist;
        this.uri = uri;
        this.duration = duration;
        this.isFavorite = false;
        this.albumCoverPath = albumCoverPath;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getUri() {
        return uri;
    }

    public long getDuration() {
        return duration;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public String getAlbumCoverPath() {
        return albumCoverPath;
    }

    // Optional setters if needed
    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public void setAlbumCoverPath(String albumCoverPath) {
        this.albumCoverPath = albumCoverPath;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Song song = (Song) obj;
        return id != null ? id.equals(song.id) : song.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}