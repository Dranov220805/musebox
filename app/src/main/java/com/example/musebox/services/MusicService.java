package com.example.musebox.services;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;

public class MusicService extends Service {

    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private boolean isPrepared = false;

    // === Binder class ===
    public class LocalBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // === Core playback methods ===

    /** Play song from Uri **/
    public void playSong(Uri songUri) {
        stopCurrent();

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(getApplicationContext(), songUri);
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPrepared = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Play song from local path **/
    public void playSong(String path) {
        if (path == null) return;
        playSong(Uri.fromFile(new File(path)));
    }

    /** Toggle pause/resume **/
    public void pauseOrResume() {
        if (mediaPlayer == null || !isPrepared) return;

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
    }

    /** Stop and release current media player **/
    public void stopCurrent() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            isPrepared = false;
        }
    }

    /** Return true if song is playing **/
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    /** Return current playback position in milliseconds **/
    public int getCurrentPosition() {
        return (mediaPlayer != null && isPrepared) ? mediaPlayer.getCurrentPosition() : 0;
    }

    /** Return song duration in milliseconds **/
    public int getDuration() {
        return (mediaPlayer != null && isPrepared) ? mediaPlayer.getDuration() : 0;
    }

    /** Volume 0.0f–1.0f **/
    public void setVolume(float volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume, volume);
        }
    }

    /** Playback speed (requires API 23+) **/
    public void setPlaybackSpeed(float speed) {
        if (mediaPlayer != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
        }
    }

    @Override
    public void onDestroy() {
        stopCurrent();
        super.onDestroy();
    }
}