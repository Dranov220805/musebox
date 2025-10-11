package com.example.musebox.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musebox.R;
import com.example.musebox.services.MusicService;

public class FullPlayerActivity extends AppCompatActivity {

    private MusicService musicService;
    private boolean isBound = false;

    private ImageButton btnClose, btnFullPlayPause, btnFullPrevious, btnFullNext, btnFullShuffle, btnFullRepeat;
    private SeekBar seekBarProgress;
    private TextView txtFullSongTitle, txtFullArtist, txtCurrentTime, txtTotalTime;

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
        seekBarProgress = findViewById(R.id.seekBarProgress);
        txtFullSongTitle = findViewById(R.id.txtFullSongTitle);
        txtFullArtist = findViewById(R.id.txtFullArtist);
        txtCurrentTime = findViewById(R.id.txtCurrentTime);
        txtTotalTime = findViewById(R.id.txtTotalTime);

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
        if (musicService == null) return;

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
        if (musicService == null) return;

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
        if (musicService == null) return;

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
        if (musicService == null) return;

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
}
