package com.example.musebox.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.adapters.SongAdapter;
import com.example.musebox.database.SongDatabaseHelper;
import com.example.musebox.fragments.NavigationBarFragment;
import com.example.musebox.models.Song;
import com.example.musebox.services.MusicService;

import java.util.ArrayList;
import java.util.List;

import android.net.Uri;
import androidx.appcompat.app.AlertDialog;

public class FavoritesActivity extends AppCompatActivity
        implements NavigationBarFragment.OnNavigationItemSelectedListener {

    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private TextView tvEmptyMessage;
    private TextView tvFavoritesCount; // Favorites count display
    private ImageButton btnBack; // Back button
    private SongAdapter adapter;
    private SongDatabaseHelper dbHelper;
    private List<Song> favoriteSongs = new ArrayList<>();

    // Mini player views
    private View miniPlayer;
    private TextView tvSongTitle;
    private ImageButton btnPlayPause, btnQueue;
    private com.example.musebox.views.CircularProgressView circularProgress;

    // Handler for updating progress
    private android.os.Handler progressHandler = new android.os.Handler();
    private Runnable progressRunnable;
    private String lastSongTitle = "";

    private MusicService musicService;
    private boolean serviceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            serviceBound = true;
            updateMiniPlayer();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        // Setup back button and favorites count display
        tvFavoritesCount = findViewById(R.id.tvFavoritesCount);
        btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        recyclerView = findViewById(R.id.recyclerFavorites);
        emptyView = findViewById(R.id.emptyView);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);

        // Initialize mini player
        miniPlayer = findViewById(R.id.includeMiniPlayer);
        tvSongTitle = miniPlayer.findViewById(R.id.txtSongTitle);
        btnPlayPause = miniPlayer.findViewById(R.id.btnPlayPause);
        btnQueue = miniPlayer.findViewById(R.id.btnQueue);
        circularProgress = miniPlayer.findViewById(R.id.circularProgress);

        // Setup progress updater
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (musicService != null) {
                    // Check if song has changed
                    String currentTitle = musicService.getCurrentSongTitle();
                    if (currentTitle != null && !currentTitle.equals(lastSongTitle)) {
                        lastSongTitle = currentTitle;
                        updateMiniPlayer();
                    }

                    // Update play/pause button state
                    if (musicService.isPlaying()) {
                        btnPlayPause.setImageResource(R.drawable.ic_pause);
                    } else {
                        btnPlayPause.setImageResource(R.drawable.ic_play);
                    }

                    // Update progress if playing
                    if (musicService.isPlaying()) {
                        int currentPosition = musicService.getCurrentPosition();
                        int duration = musicService.getDuration();

                        if (duration > 0) {
                            float progress = (currentPosition * 100f) / duration;
                            circularProgress.setProgress(progress);
                        }
                    }
                }
                progressHandler.postDelayed(this, 100); // Update every 100ms
            }
        };

        // Mini player click - expand to full screen
        miniPlayer.setOnClickListener(v -> {
            Intent intent = new Intent(FavoritesActivity.this, FullPlayerActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        btnPlayPause.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.pauseOrResume();
                if (musicService.isPlaying()) {
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                } else {
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                }
            }
        });

        // Queue button - open queue activity
        btnQueue.setOnClickListener(v -> {
            Intent intent = new Intent(FavoritesActivity.this, QueueActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        dbHelper = new SongDatabaseHelper(this);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter();
        adapter.setOnSongClickListener(this::onSongClicked);
        adapter.setMenuActionOverride((song, position) -> {
            // Override menu to show "Remove from Favorites" instead of "Delete Song"
            removeFromFavorites(song, position);
        });
        recyclerView.setAdapter(adapter);

        // Add NavigationBarFragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.navigation_container, new NavigationBarFragment())
                    .commit();
        }

        // Bind to MusicService
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        loadFavorites();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop progress updates
        progressHandler.removeCallbacks(progressRunnable);

        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop updating when activity is not visible
        progressHandler.removeCallbacks(progressRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume updating when activity is visible
        if (musicService != null) {
            // Update mini player with current song info
            updateMiniPlayer();
            // Start progress updates
            progressHandler.post(progressRunnable);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Update mini player UI with current song info from service
    private void updateMiniPlayer() {
        if (musicService != null) {
            String title = musicService.getCurrentSongTitle();
            if (title != null && !title.equals("No song playing")) {
                tvSongTitle.setText(title);
                miniPlayer.setVisibility(View.VISIBLE);

                // Update play/pause button
                if (musicService.isPlaying()) {
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                } else {
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                }
            }
        }
    }

    private void loadFavorites() {
        new Thread(() -> {
            List<Song> songs = dbHelper.getFavoriteSongs();
            runOnUiThread(() -> {
                favoriteSongs.clear();
                favoriteSongs.addAll(songs);
                adapter.setSongs(favoriteSongs); // Use DiffUtil-powered method

                // Update favorites count in landscape header (if present)
                if (tvFavoritesCount != null) {
                    int count = favoriteSongs.size();
                    String countText = count + " favorite" + (count != 1 ? "s" : "");
                    tvFavoritesCount.setText(countText);
                }

                if (favoriteSongs.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                    tvEmptyMessage.setText("No favorite songs yet");
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void onSongClicked(Song song) {
        if (serviceBound && !favoriteSongs.isEmpty()) {
            int position = favoriteSongs.indexOf(song);
            musicService.setPlaylist(favoriteSongs, position);

            Song currentSong = favoriteSongs.get(position);
            musicService.playSong(
                    android.net.Uri.parse(currentSong.getUri()),
                    currentSong.getTitle(),
                    currentSong.getArtist());

            // Show mini player
            miniPlayer.setVisibility(View.VISIBLE);

            // Reset circular progress
            circularProgress.setProgress(0);

            // Start progress updates
            progressHandler.post(progressRunnable);

            // Update UI
            tvSongTitle.setText(currentSong.getTitle());
            btnPlayPause.setImageResource(R.drawable.ic_pause);

            Toast.makeText(this, "Playing: " + currentSong.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    private void removeFromFavorites(Song song, int position) {
        new Thread(() -> {
            boolean success = dbHelper.removeFromFavorites(song.getId());
            runOnUiThread(() -> {
                if (success) {
                    favoriteSongs.remove(position);
                    adapter.removeSong(position); // Use DiffUtil-powered method
                    Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();

                    // Update favorites count in landscape header (if present)
                    if (tvFavoritesCount != null) {
                        int count = favoriteSongs.size();
                        String countText = count + " favorite" + (count != 1 ? "s" : "");
                        tvFavoritesCount.setText(countText);
                    }

                    if (favoriteSongs.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                        tvEmptyMessage.setText("No favorite songs yet");
                    }
                }
            });
        }).start();
    }

    // NavigationBarFragment.OnNavigationItemSelectedListener implementation
    @Override
    public void onNavigationItemSelected(String item) {
        Intent intent = null;
        switch (item) {
            case "home":
                // Go back to HomeActivity
                finish();
                return;
            case "search":
                Toast.makeText(this, "Search feature coming soon", Toast.LENGTH_SHORT).show();
                return;
            case "playlist":
                Toast.makeText(this, "Playlist feature coming soon", Toast.LENGTH_SHORT).show();
                return;
            case "profile":
                Toast.makeText(this, "Profile feature coming soon", Toast.LENGTH_SHORT).show();
                return;
        }
    }

    @Override
    public void onCreatePlaylistSelected() {
        // Import the utility class
        com.example.musebox.utils.ThemedDialogUtils.showInfoDialog(
                this,
                "Create Playlist",
                "Feature coming soon! This will allow you to create custom playlists from your favorite songs.",
                null);
    }

    @Override
    public void onImportMusicSelected(Uri folderUri) {
        Toast.makeText(this, "Please import music from the Home screen", Toast.LENGTH_SHORT).show();
    }
}
