package com.example.musebox.models;

import java.io.Serializable;

public class MusicRecommendation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String trackName;
    private String artistName;
    private String albumName;
    private String audioUrl;
    private String imageUrl;
    private int duration; // in seconds
    private String source; // "jamendo"

    public MusicRecommendation(String trackName, String artistName, String albumName,
            String audioUrl, String imageUrl, int duration, String source) {
        this.trackName = trackName;
        this.artistName = artistName;
        this.albumName = albumName;
        this.audioUrl = audioUrl;
        this.imageUrl = imageUrl;
        this.duration = duration;
        this.source = source;
    }

    public String getTrackName() {
        return trackName;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
