package com.example.musebox.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.net.Uri;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.musebox.R;
import com.example.musebox.models.Song;
import com.example.musebox.services.MusicService;

public class FullPlayerActivity extends AppCompatActivity {

    private MusicService musicService;
    private boolean isBound = false;

    private ImageButton btnClose, btnFullPlayPause, btnFullPrevious, btnFullNext, btnFullShuffle, btnFullRepeat;
    private ImageButton btnVolumeToggle, btnSkipBackward, btnSkipForward;
    private SeekBar seekBarProgress;
    private TextView txtFullSongTitle, txtFullArtist, txtCurrentTime, txtTotalTime, txtVolumeLevel;
    private TextView btnPlaybackSpeed;
    private ImageView imgAlbumArt;

    private AudioManager audioManager;
    private boolean isMuted = false;
    private int lastVolumeLevel = 70;
    private float currentPlaybackSpeed = 1.0f;

    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;
    private String lastSongTitle = "";

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            isBound = true;
            updateUI();
            startProgressUpdates();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_player);

        // Initialize views
        btnClose = findViewById(R.id.btnClose);
        btnFullPlayPause = findViewById(R.id.btnFullPlayPause);
        btnFullPrevious = findViewById(R.id.btnFullPrevious);
        btnFullNext = findViewById(R.id.btnFullNext);
        btnFullShuffle = findViewById(R.id.btnFullShuffle);
        btnFullRepeat = findViewById(R.id.btnFullRepeat);
        btnVolumeToggle = findViewById(R.id.btnVolumeToggle);
        btnSkipBackward = findViewById(R.id.btnSkipBackward);
        btnSkipForward = findViewById(R.id.btnSkipForward);
        seekBarProgress = findViewById(R.id.seekBarProgress);
        txtFullSongTitle = findViewById(R.id.txtFullSongTitle);
        txtFullArtist = findViewById(R.id.txtFullArtist);
        txtCurrentTime = findViewById(R.id.txtCurrentTime);
        txtTotalTime = findViewById(R.id.txtTotalTime);
        txtVolumeLevel = findViewById(R.id.txtVolumeLevel);
        btnPlaybackSpeed = findViewById(R.id.btnPlaybackSpeed);
        imgAlbumArt = findViewById(R.id.imgAlbumArt);

        // Initialize AudioManager
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Setup volume control
        setupVolumeControl();

        // Setup playback speed button
        setupPlaybackSpeedButton();

        // Bind to service
        Intent serviceIntent = new Intent(this, MusicService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // Setup progress updater
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (musicService != null) {
                    int currentPosition = musicService.getCurrentPosition();
                    int duration = musicService.getDuration();

                    if (duration > 0) {
                        seekBarProgress.setMax(duration);
                        seekBarProgress.setProgress(currentPosition);
                        txtCurrentTime.setText(formatTime(currentPosition));
                        txtTotalTime.setText(formatTime(duration));
                    }

                    // Check if song changed
                    String currentTitle = musicService.getCurrentSongTitle();
                    if (currentTitle != null && !currentTitle.equals(lastSongTitle)) {
                        lastSongTitle = currentTitle;
                        updateUI();
                    }

                    updatePlayPauseButton();
                    updateShuffleButton();
                    updateRepeatButton();
                }
                progressHandler.postDelayed(this, 500);
            }
        };

        // Click listeners
        btnClose.setOnClickListener(v -> finish());

        btnFullPlayPause.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.pauseOrResume();
                updatePlayPauseButton();
            }
        });

        btnFullPrevious.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.playPrevious();
                // Update UI after song change
                updateUI();
            }
        });

        btnFullNext.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.playNext();
                // Update UI after song change
                updateUI();
            }
        });

        btnFullShuffle.setOnClickListener(v -> {
            if (musicService != null) {
                toggleShuffle();
            }
        });

        btnFullRepeat.setOnClickListener(v -> {
            if (musicService != null) {
                cycleRepeatMode();
            }
        });

        btnVolumeToggle.setOnClickListener(v -> toggleMute());

        btnSkipBackward.setOnClickListener(v -> skipBackward());

        btnSkipForward.setOnClickListener(v -> skipForward());

        seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && musicService != null) {
                    musicService.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void updateUI() {
        if (musicService != null) {
            txtFullSongTitle.setText(musicService.getCurrentSongTitle());
            txtFullArtist.setText(musicService.getCurrentSongArtist());

            // Load album art for current song
            Song currentSong = musicService.getCurrentSong();
            if (currentSong != null && imgAlbumArt != null) {
                loadAlbumArt(currentSong);
            }

            updatePlayPauseButton();
            updateShuffleButton();
            updateRepeatButton();
        }
    }

    private void updatePlayPauseButton() {
        if (musicService != null && musicService.isPlaying()) {
            btnFullPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            btnFullPlayPause.setImageResource(R.drawable.ic_play);
        }
    }

    private void toggleShuffle() {
        if (musicService == null)
            return;

        MusicService.PlaybackMode currentMode = musicService.getPlaybackMode();

        if (currentMode == MusicService.PlaybackMode.SHUFFLE) {
            // Turn off shuffle, go to normal mode
            musicService.setPlaybackMode(MusicService.PlaybackMode.NORMAL);
        } else {
            // Turn on shuffle
            musicService.setPlaybackMode(MusicService.PlaybackMode.SHUFFLE);
        }

        updateShuffleButton();
    }

    private void cycleRepeatMode() {
        if (musicService == null)
            return;

        MusicService.PlaybackMode currentMode = musicService.getPlaybackMode();

        // Cycle: NORMAL -> REPEAT_ALL -> REPEAT_ONE -> NORMAL
        switch (currentMode) {
            case NORMAL:
            case SHUFFLE:
                musicService.setPlaybackMode(MusicService.PlaybackMode.REPEAT_ALL);
                break;
            case REPEAT_ALL:
                musicService.setPlaybackMode(MusicService.PlaybackMode.REPEAT_ONE);
                break;
            case REPEAT_ONE:
                musicService.setPlaybackMode(MusicService.PlaybackMode.NORMAL);
                break;
        }

        updateRepeatButton();
    }

    private void updateShuffleButton() {
        if (musicService == null)
            return;

        MusicService.PlaybackMode mode = musicService.getPlaybackMode();

        if (mode == MusicService.PlaybackMode.SHUFFLE) {
            // Shuffle is ON - highlight the button
            btnFullShuffle.setColorFilter(getResources().getColor(R.color.white, null));
        } else {
            // Shuffle is OFF - gray out the button
            btnFullShuffle.setColorFilter(getResources().getColor(R.color.gray, null));
        }
    }

    private void updateRepeatButton() {
        if (musicService == null)
            return;

        MusicService.PlaybackMode mode = musicService.getPlaybackMode();

        switch (mode) {
            case REPEAT_ONE:
                // Show repeat one icon and highlight
                btnFullRepeat.setImageResource(R.drawable.ic_repeat_one);
                btnFullRepeat.setColorFilter(getResources().getColor(R.color.white, null));
                break;
            case REPEAT_ALL:
                // Show repeat all icon and highlight
                btnFullRepeat.setImageResource(R.drawable.ic_repeat_all);
                btnFullRepeat.setColorFilter(getResources().getColor(R.color.white, null));
                break;
            case NORMAL:
            case SHUFFLE:
            default:
                // Show repeat all icon but gray out
                btnFullRepeat.setImageResource(R.drawable.ic_repeat_all);
                btnFullRepeat.setColorFilter(getResources().getColor(R.color.gray, null));
                break;
        }
    }

    private void startProgressUpdates() {
        progressHandler.post(progressRunnable);
    }

    private String formatTime(int milliseconds) {
        int minutes = (milliseconds / 1000) / 60;
        int seconds = (milliseconds / 1000) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void setupVolumeControl() {
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        // Initialize last volume level with current volume
        lastVolumeLevel = currentVolume;

        // Update volume display
        int percentage = currentVolume * 100 / maxVolume;
        txtVolumeLevel.setText(percentage + "%");

        // Update volume icon
        if (currentVolume == 0) {
            btnVolumeToggle.setImageResource(R.drawable.ic_volume_off);
            isMuted = true;
        } else {
            btnVolumeToggle.setImageResource(R.drawable.ic_volume_up);
            isMuted = false;
        }
    }

    private void setupPlaybackSpeedButton() {
        btnPlaybackSpeed.setOnClickListener(v -> showSpeedMenu(v));
    }

    private void showSpeedMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.inflate(R.menu.playback_speed_menu);

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.speed_0_5) {
                currentPlaybackSpeed = 0.5f;
                btnPlaybackSpeed.setText("0.5x");
            } else if (itemId == R.id.speed_0_75) {
                currentPlaybackSpeed = 0.75f;
                btnPlaybackSpeed.setText("0.75x");
            } else if (itemId == R.id.speed_1_0) {
                currentPlaybackSpeed = 1.0f;
                btnPlaybackSpeed.setText("1.0x");
            } else if (itemId == R.id.speed_1_25) {
                currentPlaybackSpeed = 1.25f;
                btnPlaybackSpeed.setText("1.25x");
            } else if (itemId == R.id.speed_1_5) {
                currentPlaybackSpeed = 1.5f;
                btnPlaybackSpeed.setText("1.5x");
            } else if (itemId == R.id.speed_2_0) {
                currentPlaybackSpeed = 2.0f;
                btnPlaybackSpeed.setText("2.0x");
            }

            if (musicService != null) {
                musicService.setPlaybackSpeed(currentPlaybackSpeed);
            }

            return true;
        });

        popupMenu.show();
    }

    private void toggleMute() {
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        if (isMuted) {
            // Unmute - restore previous volume
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, lastVolumeLevel, 0);
            btnVolumeToggle.setImageResource(R.drawable.ic_volume_up);
            int percentage = lastVolumeLevel * 100 / maxVolume;
            txtVolumeLevel.setText(percentage + "%");
            isMuted = false;
        } else {
            // Mute - save current volume and set to 0
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (currentVolume > 0) {
                lastVolumeLevel = currentVolume;
            }
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            btnVolumeToggle.setImageResource(R.drawable.ic_volume_off);
            txtVolumeLevel.setText("0%");
            isMuted = true;
        }
    }

    private void skipBackward() {
        if (musicService != null) {
            int currentPosition = musicService.getCurrentPosition();
            int newPosition = Math.max(0, currentPosition - 10000); // Skip back 10 seconds
            musicService.seekTo(newPosition);
        }
    }

    private void skipForward() {
        if (musicService != null) {
            int currentPosition = musicService.getCurrentPosition();
            int duration = musicService.getDuration();
            int newPosition = Math.min(duration, currentPosition + 10000); // Skip forward 1 seconds
            musicService.seekTo(newPosition);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressHandler.removeCallbacks(progressRunnable);
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        progressHandler.removeCallbacks(progressRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isBound && musicService != null) {
            updateUI();
            startProgressUpdates();
        }
    }

    /**
     * Load album art asynchronously using Glide with caching.
     * Prioritizes custom album cover path over embedded audio file art.
     */
    private void loadAlbumArt(Song song) {
        if (song == null || imgAlbumArt == null) {
            return;
        }

        // Check if song has a custom album cover path
        String albumCoverPath = song.getAlbumCoverPath();

        if (albumCoverPath != null && !albumCoverPath.isEmpty()) {
            // Load from custom album cover path (file path or URI)
            Glide.with(this)
                    .load(albumCoverPath)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_music_note) // Show default on error, or fallback to embedded art
                    .centerCrop()
                    .into(imgAlbumArt);
        } else {
            // No custom cover, load embedded art from audio file
            loadEmbeddedAlbumArt(song.getUri());
        }
    }

    /**
     * Load embedded album art from audio file
     */
    private void loadEmbeddedAlbumArt(String audioFilePath) {
        if (imgAlbumArt == null) {
            return;
        }

        Uri audioUri = Uri.parse(audioFilePath);

        Glide.with(this)
                .asBitmap() // Explicitly request bitmap to properly extract embedded art
                .load(audioUri)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .centerCrop()
                .timeout(3000) // 3 second timeout to prevent hanging
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.Bitmap>() {
                    @Override
                    public boolean onLoadFailed(
                            @androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model,
                            com.bumptech.glide.request.target.Target<android.graphics.Bitmap> target,
                            boolean isFirstResource) {
                        // Simplified error logging
                        android.util.Log.w("FullPlayerAlbumArt", "Failed to load album art");
                        return false; // Let Glide handle showing error drawable
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.Bitmap resource, Object model,
                            com.bumptech.glide.request.target.Target<android.graphics.Bitmap> target,
                            com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        return false; // Let Glide handle the success case
                    }
                })
                .into(imgAlbumArt);
    }
}
