package com.example.musebox.models;

import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private String id;
    private String name;
    private String description;
    private List<Song> songs;

    // Constructor
    public Playlist(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.songs = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
    }

    // Add a song to the playlist
    public void addSong(Song song) {
        this.songs.add(song);
    }

    // Remove a song from the playlist
    public void removeSong(Song song) {
        this.songs.remove(song);
    }

    // Get the total number of songs
    public int getSongCount() {
        return this.songs.size();
    }

    // Check if the playlist contains a specific song
    public boolean containsSong(Song song) {
        return this.songs.contains(song);
    }
}