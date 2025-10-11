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

    private ImageButton btnClose, btnFullPlayPause, btnFullPrevious, btnFullNext;
    private SeekBar seekBarProgress;
    private TextView txtFullSongTitle, txtFullArtist, txtCurrentTime, txtTotalTime;

    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;

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

                    updatePlayPauseButton();
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
            }
        });

        btnFullNext.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.playNext();
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
        }
    }

    private void updatePlayPauseButton() {
        if (musicService != null && musicService.isPlaying()) {
            btnFullPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            btnFullPlayPause.setImageResource(R.drawable.ic_play);
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
