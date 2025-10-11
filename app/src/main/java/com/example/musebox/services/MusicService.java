package com.example.musebox.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.musebox.R;
import com.example.musebox.activities.HomeActivity;

import java.io.File;
import java.io.IOException;

public class MusicService extends Service {

    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private boolean isPrepared = false;

    // Notification constants
    private static final String CHANNEL_ID = "MusicPlaybackChannel";
    private static final int NOTIFICATION_ID = 1;

    // Actions for notification buttons
    public static final String ACTION_PLAY_PAUSE = "com.example.musebox.ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "com.example.musebox.ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "com.example.musebox.ACTION_PREVIOUS";
    public static final String ACTION_STOP = "com.example.musebox.ACTION_STOP";

    // Current song info
    private String currentSongTitle = "No song playing";
    private String currentSongArtist = "Unknown Artist";

    // Playlist management
    private java.util.List<com.example.musebox.models.Song> playlist = new java.util.ArrayList<>();
    private int currentSongIndex = -1;

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

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PLAY_PAUSE:
                    pauseOrResume();
                    updateNotification();
                    break;
                case ACTION_NEXT:
                    playNext();
                    break;
                case ACTION_PREVIOUS:
                    playPrevious();
                    break;
                case ACTION_STOP:
                    stopPlaybackAndRemoveNotification();
                    break;
            }
        }
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Playback",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Controls for music playback");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // === Core playback methods ===

    /** Play song from Uri with title and artist **/
    public void playSong(Uri songUri, String title, String artist) {
        stopCurrent();

        currentSongTitle = title != null ? title : "Unknown Song";
        currentSongArtist = artist != null ? artist : "Unknown Artist";

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(getApplicationContext(), songUri);
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPrepared = true;

            // Show notification when song starts
            showNotification();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Play song from Uri **/
    public void playSong(Uri songUri) {
        playSong(songUri, "Unknown Song", "Unknown Artist");
    }

    /** Play song from local path **/
    public void playSong(String path) {
        if (path == null)
            return;
        playSong(Uri.fromFile(new File(path)));
    }

    /** Toggle pause/resume **/
    public void pauseOrResume() {
        if (mediaPlayer == null || !isPrepared)
            return;

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
        updateNotification();
    }

    /** Stop and release current media player **/
    public void stopCurrent() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying())
                mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            isPrepared = false;
        }
    }

    /** Stop playback and remove notification **/
    public void stopPlaybackAndRemoveNotification() {
        stopCurrent();
        stopForeground(true);
        stopSelf();
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

    /** Seek to position **/
    public void seekTo(int position) {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(position);
        }
    }

    /** Get current song title **/
    public String getCurrentSongTitle() {
        return currentSongTitle;
    }

    /** Get current song artist **/
    public String getCurrentSongArtist() {
        return currentSongArtist;
    }

    /** Set playlist **/
    public void setPlaylist(java.util.List<com.example.musebox.models.Song> songs, int startIndex) {
        this.playlist = songs;
        this.currentSongIndex = startIndex;
    }

    /** Get current playlist **/
    public java.util.List<com.example.musebox.models.Song> getPlaylist() {
        return new java.util.ArrayList<>(playlist);
    }

    /** Get current song from playlist **/
    public com.example.musebox.models.Song getCurrentSong() {
        if (playlist.isEmpty() || currentSongIndex < 0 || currentSongIndex >= playlist.size()) {
            return null;
        }
        return playlist.get(currentSongIndex);
    }

    /** Get current song index **/
    public int getCurrentSongIndex() {
        return currentSongIndex;
    }

    /** Play next song in playlist **/
    public void playNext() {
        if (playlist.isEmpty())
            return;

        currentSongIndex = (currentSongIndex + 1) % playlist.size();
        com.example.musebox.models.Song nextSong = playlist.get(currentSongIndex);
        playSong(android.net.Uri.parse(nextSong.getUri()), nextSong.getTitle(), nextSong.getArtist());
    }

    /** Play previous song in playlist **/
    public void playPrevious() {
        if (playlist.isEmpty())
            return;

        currentSongIndex = (currentSongIndex - 1 + playlist.size()) % playlist.size();
        com.example.musebox.models.Song previousSong = playlist.get(currentSongIndex);
        playSong(android.net.Uri.parse(previousSong.getUri()), previousSong.getTitle(), previousSong.getArtist());
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

    // === Notification Methods ===

    private void showNotification() {
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void updateNotification() {
        Notification notification = createNotification();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private Notification createNotification() {
        // Intent to open the app when notification is clicked
        Intent notificationIntent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Play/Pause action
        Intent playPauseIntent = new Intent(this, MusicService.class);
        playPauseIntent.setAction(ACTION_PLAY_PAUSE);
        PendingIntent playPausePendingIntent = PendingIntent.getService(
                this,
                0,
                playPauseIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Previous action
        Intent previousIntent = new Intent(this, MusicService.class);
        previousIntent.setAction(ACTION_PREVIOUS);
        PendingIntent previousPendingIntent = PendingIntent.getService(
                this,
                1,
                previousIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Next action
        Intent nextIntent = new Intent(this, MusicService.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(
                this,
                2,
                nextIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Stop action
        Intent stopIntent = new Intent(this, MusicService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                3,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Determine play/pause icon
        int playPauseIcon = isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play;

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentSongTitle)
                .setContentText(currentSongArtist)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // Add media style
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                // Add action buttons
                .addAction(R.drawable.ic_skip_previous, "Previous", previousPendingIntent)
                .addAction(playPauseIcon, "Play/Pause", playPausePendingIntent)
                .addAction(R.drawable.ic_skip_next, "Next", nextPendingIntent);

        return builder.build();
    }

    @Override
    public void onDestroy() {
        stopCurrent();
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // App is swiped away from recent apps - stop service and remove notification
        stopPlaybackAndRemoveNotification();
        super.onTaskRemoved(rootIntent);
    }
}