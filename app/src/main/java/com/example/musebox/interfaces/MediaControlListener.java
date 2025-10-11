package com.example.musebox.interfaces;

import com.example.musebox.models.Song;

public interface MediaControlListener {
    void onPlay(Song song);
    void onPause();
    void onSeek(int position);
    void onVolumeChanged(float volume);
    void onSpeedChanged(float speed);
}