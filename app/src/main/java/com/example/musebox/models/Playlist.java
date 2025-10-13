package com.example.musebox.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Playlist {
    private String id;
    private String name;
    private List<Song> songs;
    private long createdAt;

    // Constructor for new playlist
    public Playlist(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.songs = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    // Constructor for loaded playlist
    public Playlist(String id, String name, List<Song> songs, long createdAt) {
        this.id = id;
        this.name = name;
        this.songs = songs != null ? songs : new ArrayList<>();
        this.createdAt = createdAt;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
    }

    // Utility Methods
    public void addSong(Song song) {
        if (!songs.contains(song)) {
            songs.add(song);
        }
    }

    public void removeSong(Song song) {
        songs.remove(song);
    }

    public boolean containsSong(Song song) {
        return songs.contains(song);
    }

    public int getSongCount() {
        return songs.size();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Playlist playlist = (Playlist) obj;
        return id != null ? id.equals(playlist.id) : playlist.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
