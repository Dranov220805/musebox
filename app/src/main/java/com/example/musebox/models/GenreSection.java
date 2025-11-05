package com.example.musebox.models;

import java.util.List;

/**
 * Model for a genre section containing a genre name and its music list
 */
public class GenreSection {
    private String genreName;
    private List<MusicRecommendation> musicList;

    public GenreSection(String genreName, List<MusicRecommendation> musicList) {
        this.genreName = genreName;
        this.musicList = musicList;
    }

    public String getGenreName() {
        return genreName;
    }

    public void setGenreName(String genreName) {
        this.genreName = genreName;
    }

    public List<MusicRecommendation> getMusicList() {
        return musicList;
    }

    public void setMusicList(List<MusicRecommendation> musicList) {
        this.musicList = musicList;
    }
}
