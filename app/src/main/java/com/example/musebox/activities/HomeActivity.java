package com.example.musebox.activities;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.musebox.R;
import com.example.musebox.database.SongDatabaseHelper;
import com.example.musebox.fragments.*;
import com.example.musebox.models.Song;
import com.example.musebox.services.MusicService;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity
        implements NavigationBarFragment.OnNavigationItemSelectedListener,
        HomeFragment.OnSongSelectedListener,
        QueueFragment.OnQueueFragmentListener,
        FavoritesFragment.OnFavoritesFragmentListener {

    private SongDatabaseHelper dbHelper;

    private MusicService musicService;
    private boolean isBound = false;

    // Mini player views
    private View miniPlayer;
    private TextView tvSongTitle;
    private TextView tvArtistName;
    private ImageButton btnPlayPause, btnSpeed, btnQueue;
    private com.example.musebox.views.CircularProgressView circularProgress;
    private ImageView imgSongArt;

    // Handler for updating progress
    private android.os.Handler progressHandler = new android.os.Handler();
    private Runnable progressRunnable;

    // Current song and playlist
    private Song currentSong;
    private List<Song> currentPlaylist = new ArrayList<>();
    private String lastSongTitle = "";
    private int lastSongIndex = -1;
    private int lastPlaybackPosition = 0;
    private boolean wasPlaying = false;

    // Flag to restore miniplayer state after orientation change
    private boolean shouldRestoreMiniPlayer = false;

    // For permission handling
    private Uri pendingImportUri;

    // ServiceConnection to bind with MusicService
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            isBound = true;

            Song currentSong = musicService.getCurrentSong();
            int songIndex = musicService.getCurrentSongIndex();
            java.util.List<?> playlist = musicService.getPlaylist();

            android.util.Log.d("HomeActivity", "onServiceConnected: currentSong=" +
                    (currentSong != null ? currentSong.getTitle() : "null") +
                    ", songIndex=" + songIndex +
                    ", playlistSize=" + playlist.size());

            // If service lost its playlist but should have one (e.g., after orientation
            // change)
            // Restore it from the database
            if (playlist.isEmpty() && shouldRestoreMiniPlayer) {
                android.util.Log.d("HomeActivity",
                        "onServiceConnected: Service lost playlist, restoring from database");
                // Load all songs from database
                List<Song> allSongs = dbHelper.getAllSongs();
                if (!allSongs.isEmpty()) {
                    // Use saved index if available and valid, otherwise search by title
                    int restoredIndex = 0;

                    if (lastSongIndex >= 0 && lastSongIndex < allSongs.size()) {
                        // Verify the song at this index matches the saved title
                        if (lastSongTitle != null && allSongs.get(lastSongIndex).getTitle().equals(lastSongTitle)) {
                            restoredIndex = lastSongIndex;
                            android.util.Log.d("HomeActivity", "onServiceConnected: Using saved index " + restoredIndex
                                    + " for song '" + lastSongTitle + "'");
                        } else {
                            android.util.Log.d("HomeActivity",
                                    "onServiceConnected: Index mismatch, searching by title");
                            // Index doesn't match, search by title
                            if (lastSongTitle != null && !lastSongTitle.isEmpty()) {
                                for (int i = 0; i < allSongs.size(); i++) {
                                    if (allSongs.get(i).getTitle().equals(lastSongTitle)) {
                                        restoredIndex = i;
                                        android.util.Log.d("HomeActivity", "onServiceConnected: Found saved song '"
                                                + lastSongTitle + "' at index " + restoredIndex);
                                        break;
                                    }
                                }
                            }
                        }
                    } else if (lastSongTitle != null && !lastSongTitle.isEmpty()) {
                        // No valid index saved, search by title
                        android.util.Log.d("HomeActivity",
                                "onServiceConnected: No valid saved index, searching by title");
                        for (int i = 0; i < allSongs.size(); i++) {
                            if (allSongs.get(i).getTitle().equals(lastSongTitle)) {
                                restoredIndex = i;
                                android.util.Log.d("HomeActivity", "onServiceConnected: Found saved song '"
                                        + lastSongTitle + "' at index " + restoredIndex);
                                break;
                            }
                        }
                    }

                    // Restore the playlist to the service
                    musicService.setPlaylist(allSongs, restoredIndex);
                    currentSong = musicService.getCurrentSong();
                    android.util.Log.d("HomeActivity",
                            "onServiceConnected: Playlist restored with " + allSongs.size() + " songs, currentSong=" +
                                    (currentSong != null ? currentSong.getTitle() : "null"));

                    // Restore playback position if available
                    if (lastPlaybackPosition > 0) {
                        musicService.seekTo(lastPlaybackPosition);
                        android.util.Log.d("HomeActivity",
                                "onServiceConnected: Restored playback position to " + lastPlaybackPosition);
                    }

                    // Resume playback if it was playing before orientation change
                    if (wasPlaying && !musicService.isPlaying()) {
                        musicService.pauseOrResume();
                        android.util.Log.d("HomeActivity", "onServiceConnected: Resumed playback");
                    }
                }
            }

            // Update mini player when service reconnects (e.g., after orientation change)
            // Check if there's a current song playing
            if (currentSong != null || shouldRestoreMiniPlayer) {
                shouldRestoreMiniPlayer = true;
                // Ensure miniplayer is visible
                if (miniPlayer.getVisibility() != View.VISIBLE) {
                    miniPlayer.setVisibility(View.VISIBLE);
                    android.util.Log.d("HomeActivity", "onServiceConnected: Set miniPlayer to VISIBLE");
                }
                // Post to ensure all views are fully initialized and service metadata is set
                miniPlayer.postDelayed(() -> {
                    android.util.Log.d("HomeActivity", "onServiceConnected: Calling updateMiniPlayer");
                    updateMiniPlayer();
                    progressHandler.post(progressRunnable);
                }, 100); // Small delay to ensure service has finished setting metadata
            } else {
                android.util.Log.d("HomeActivity", "onServiceConnected: No current song");
            }
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
        setContentView(R.layout.activity_home);

        miniPlayer = findViewById(R.id.includeMiniPlayer);
        tvSongTitle = miniPlayer.findViewById(R.id.txtSongTitle);
        tvArtistName = miniPlayer.findViewById(R.id.txtArtistName);
        btnPlayPause = miniPlayer.findViewById(R.id.btnPlayPause);
        btnQueue = miniPlayer.findViewById(R.id.btnQueue);
        circularProgress = miniPlayer.findViewById(R.id.circularProgress);
        imgSongArt = miniPlayer.findViewById(R.id.imgSongArt);

        android.util.Log.d("HomeActivity", "onCreate: Views initialized - miniPlayer=" + miniPlayer +
                ", tvSongTitle=" + tvSongTitle + ", btnPlayPause=" + btnPlayPause);

        // Bind to MusicService
        Intent serviceIntent = new Intent(this, MusicService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

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
            Intent intent = new Intent(HomeActivity.this, FullPlayerActivity.class);
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

        // Queue button - open queue fragment
        btnQueue.setOnClickListener(v -> {
            QueueFragment queueFragment = new QueueFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_container, queueFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Swipe down to dismiss
        setupSwipeGesture();

        dbHelper = new SongDatabaseHelper(this);

        // Check for songs without album art and offer to update them
        checkAndOfferAlbumArtUpdate();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_container, new HomeFragment())
                    .commit();

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.navigation_container, new NavigationBarFragment())
                    .commit();
        } else {
            // Restoring from saved state (e.g., orientation change)
            // Restore miniplayer visibility if it was visible before
            shouldRestoreMiniPlayer = savedInstanceState.getBoolean("isMiniPlayerVisible", false);
            String savedTitle = savedInstanceState.getString("lastSongTitle", "");
            lastSongIndex = savedInstanceState.getInt("lastSongIndex", -1);
            android.util.Log.d("HomeActivity", "onCreate: Restoring from saved state, shouldRestoreMiniPlayer=" +
                    shouldRestoreMiniPlayer + ", savedTitle=" + savedTitle + ", lastSongIndex=" + lastSongIndex);

            if (shouldRestoreMiniPlayer) {
                // Set visibility immediately if we know it should be visible
                // The actual content will be updated when service connects
                miniPlayer.setVisibility(View.VISIBLE);

                // Also restore the last song title temporarily
                if (!savedTitle.isEmpty() && tvSongTitle != null) {
                    tvSongTitle.setText(savedTitle);
                    android.util.Log.d("HomeActivity", "onCreate: Set temporary title: " + savedTitle);
                }
            }
        }
    }

    private void setupSwipeGesture() {
        miniPlayer.setOnTouchListener(new android.view.View.OnTouchListener() {
            private float startY;
            private static final int MIN_DISTANCE = 100;

            @Override
            public boolean onTouch(android.view.View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startY = event.getY();
                        return false;

                    case android.view.MotionEvent.ACTION_UP:
                        float endY = event.getY();
                        float deltaY = endY - startY;

                        if (deltaY > MIN_DISTANCE) {
                            // Swiped down - animate and dismiss
                            miniPlayer.animate()
                                    .translationY(miniPlayer.getHeight())
                                    .alpha(0f)
                                    .setDuration(300)
                                    .withEndAction(() -> {
                                        if (musicService != null) {
                                            musicService.stopPlaybackAndRemoveNotification();
                                        }
                                        miniPlayer.setVisibility(View.GONE);
                                        miniPlayer.setTranslationY(0);
                                        miniPlayer.setAlpha(1f);
                                        progressHandler.removeCallbacks(progressRunnable);
                                    })
                                    .start();
                            return true;
                        }
                        break;
                }
                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop progress updates
        progressHandler.removeCallbacks(progressRunnable);

        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
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
        if (musicService != null && musicService.getCurrentSong() != null) {
            // Update mini player with current song info
            updateMiniPlayer();
            // Start progress updates
            progressHandler.post(progressRunnable);
        }
    }

    @Override
    protected void onSaveInstanceState(@androidx.annotation.NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save mini player state
        boolean isMiniPlayerVisible = miniPlayer != null && miniPlayer.getVisibility() == View.VISIBLE;
        outState.putBoolean("isMiniPlayerVisible", isMiniPlayerVisible);
        outState.putString("lastSongTitle", lastSongTitle);

        // Save song index, playback position, and playing state if service is available
        if (musicService != null) {
            lastSongIndex = musicService.getCurrentSongIndex();
            lastPlaybackPosition = musicService.getCurrentPosition();
            wasPlaying = musicService.isPlaying();
            android.util.Log.d("HomeActivity", "onSaveInstanceState: Saving state - index=" + lastSongIndex +
                    ", position=" + lastPlaybackPosition + ", wasPlaying=" + wasPlaying);
        }
        outState.putInt("lastSongIndex", lastSongIndex);
        outState.putInt("lastPlaybackPosition", lastPlaybackPosition);
        outState.putBoolean("wasPlaying", wasPlaying);
    }

    @Override
    protected void onRestoreInstanceState(@androidx.annotation.NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore mini player state
        shouldRestoreMiniPlayer = savedInstanceState.getBoolean("isMiniPlayerVisible", false);
        lastSongTitle = savedInstanceState.getString("lastSongTitle", "");
        lastSongIndex = savedInstanceState.getInt("lastSongIndex", -1);
        lastPlaybackPosition = savedInstanceState.getInt("lastPlaybackPosition", 0);
        wasPlaying = savedInstanceState.getBoolean("wasPlaying", false);

        android.util.Log.d("HomeActivity",
                "onRestoreInstanceState: Restored state - shouldRestore=" + shouldRestoreMiniPlayer +
                        ", title=" + lastSongTitle + ", index=" + lastSongIndex +
                        ", position=" + lastPlaybackPosition + ", wasPlaying=" + wasPlaying);

        // Update mini player if it was visible and service is already bound
        if (shouldRestoreMiniPlayer && musicService != null && musicService.getCurrentSong() != null) {
            updateMiniPlayer();
            progressHandler.post(progressRunnable);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions,
            @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 201) { // Our permission request code
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Importing music using MediaStore...", Toast.LENGTH_SHORT).show();
                importMusicFromMediaStore(pendingImportUri);
                pendingImportUri = null;
            } else {
                Toast.makeText(this, "Permission denied! Cannot scan for songs.", Toast.LENGTH_SHORT).show();
                pendingImportUri = null;
            }
        }
    }

    // Implement HomeFragment.OnSongSelectedListener
    @Override
    public void onSongSelected(Song song) {
        if (musicService != null && song != null) {
            // Store current song
            currentSong = song;

            // Load all songs from database as playlist
            currentPlaylist = dbHelper.getAllSongs();
            int songIndex = currentPlaylist.indexOf(song);
            if (songIndex == -1)
                songIndex = 0;

            // Set playlist in service
            musicService.setPlaylist(currentPlaylist, songIndex);

            // Play the selected song with title and artist info
            Uri songUri = Uri.parse(song.getUri());
            musicService.playSong(songUri, song.getTitle(), song.getArtist());

            // Show mini player (will be visible when user returns from full player)
            miniPlayer.setVisibility(View.VISIBLE);

            // Reset circular progress
            circularProgress.setProgress(0);

            // Start progress updates
            progressHandler.post(progressRunnable);

            // Update UI
            tvSongTitle.setText(song.getTitle());
            btnPlayPause.setImageResource(R.drawable.ic_pause);

            // Open expanded player immediately when a song is selected
            Intent intent = new Intent(HomeActivity.this, FullPlayerActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);

            Toast.makeText(this, "Playing: " + song.getTitle(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Music service not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFavoritesClicked() {
        // Navigate to favorites fragment
        FavoritesFragment favoritesFragment = new FavoritesFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_container, favoritesFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onFavoriteSongSelected(Song song, List<Song> favoritesPlaylist) {
        if (musicService != null && song != null) {
            // Store current song
            currentSong = song;

            // Use the favorites playlist instead of all songs
            currentPlaylist = favoritesPlaylist;
            int songIndex = currentPlaylist.indexOf(song);
            if (songIndex == -1)
                songIndex = 0;

            // Set playlist in service
            musicService.setPlaylist(currentPlaylist, songIndex);

            // Play the selected song with title and artist info
            Uri songUri = Uri.parse(song.getUri());
            musicService.playSong(songUri, song.getTitle(), song.getArtist());

            // Show mini player (will be visible when user returns from full player)
            miniPlayer.setVisibility(View.VISIBLE);

            // Reset circular progress
            circularProgress.setProgress(0);

            // Start progress updates
            progressHandler.post(progressRunnable);

            // Update UI
            tvSongTitle.setText(song.getTitle());
            btnPlayPause.setImageResource(R.drawable.ic_pause);

            // Open expanded player immediately when a song is selected
            Intent intent = new Intent(HomeActivity.this, FullPlayerActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);

            Toast.makeText(this, "Playing: " + song.getTitle(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Music service not available", Toast.LENGTH_SHORT).show();
        }
    }

    // Update mini player UI with current song info from service
    private void updateMiniPlayer() {
        if (musicService == null) {
            android.util.Log.d("HomeActivity", "updateMiniPlayer: musicService is null");
            return;
        }

        if (miniPlayer == null || tvSongTitle == null || btnPlayPause == null) {
            android.util.Log.e("HomeActivity", "updateMiniPlayer: Views not initialized");
            return;
        }

        String title = musicService.getCurrentSongTitle();
        String artist = musicService.getCurrentSongArtist();
        android.util.Log.d("HomeActivity", "updateMiniPlayer: title = " + title + ", artist = " + artist);

        if (title != null && !title.equals("No song playing")) {
            tvSongTitle.setText(title);

            // Update artist name if the view exists
            if (tvArtistName != null) {
                tvArtistName.setText(artist != null ? artist : "Unknown Artist");
            }

            miniPlayer.setVisibility(View.VISIBLE);

            // Update play/pause button
            if (musicService.isPlaying()) {
                btnPlayPause.setImageResource(R.drawable.ic_pause);
            } else {
                btnPlayPause.setImageResource(R.drawable.ic_play);
            }

            // Update current song reference
            currentSong = musicService.getCurrentSong();
            lastSongTitle = title;

            // Load album art for current song in mini player
            if (currentSong != null && imgSongArt != null) {
                loadMiniPlayerAlbumArt(currentSong);
            }

            // Refresh fragments if they are currently visible
            refreshCurrentFragment();
        } else {
            // No song playing, hide mini player
            miniPlayer.setVisibility(View.GONE);
            currentSong = null;
            lastSongTitle = "";
        }
    }

    // Refresh the currently visible fragment
    private void refreshCurrentFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.content_container);
        if (currentFragment instanceof QueueFragment) {
            ((QueueFragment) currentFragment).refreshQueue();
        } else if (currentFragment instanceof FavoritesFragment) {
            ((FavoritesFragment) currentFragment).refreshFavorites();
        }
    }

    // Enhanced method to add song to queue with better synchronization
    public void addSongToQueue(Song song) {
        if (musicService != null && song != null) {
            musicService.addToQueue(song);
            Toast.makeText(this, "Added \"" + song.getTitle() + "\" to queue", Toast.LENGTH_SHORT).show();

            // Update mini player and refresh fragments
            updateMiniPlayer();
        } else {
            Toast.makeText(this, "Music service not available", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to toggle favorite status and sync with UI
    public void toggleFavorite(Song song) {
        if (song == null)
            return;

        boolean isFavorite = dbHelper.isFavorite(song.getId());
        if (isFavorite) {
            dbHelper.removeFromFavorites(song.getId());
            Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
        } else {
            dbHelper.addToFavorites(song.getId());
            Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
        }

        // Refresh favorites fragment if it's currently visible
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.content_container);
        if (currentFragment instanceof FavoritesFragment) {
            ((FavoritesFragment) currentFragment).toggleFavorite(song);
        }
    }

    @Override
    public void onNavigationItemSelected(String item) {
        Fragment selected = null;
        switch (item) {
            case "home":
                selected = new HomeFragment();
                break;
            case "search":
                selected = new SearchFragment();
                break;
            case "playlist":
                selected = new PlaylistFragment();
                break;
            case "profile":
                selected = new ProfileFragment();
                break;
            case "favorites":
                selected = new FavoritesFragment();
                break;
            case "queue":
                selected = new QueueFragment();
                break;
        }

        if (selected != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_container, selected)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onCreatePlaylistSelected() {
        // Import the utility class
        com.example.musebox.utils.ThemedDialogUtils.showInfoDialog(
                this,
                "Create Playlist",
                "Feature coming soon! This will allow you to create custom playlists from your music library.",
                null);
    }

    @Override
    public void onImportMusicSelected(Uri folderUri) {
        // Check permissions first, just like HomeFragment does
        checkPermissionAndImport(folderUri);
    }

    @Override
    public void onUpdateAlbumArtSelected() {
        updateExistingSongsWithAlbumArt();
    }

    private void checkPermissionAndImport(Uri folderUri) {
        // Check Android version and request appropriate permission
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            permission = Manifest.permission.READ_MEDIA_AUDIO;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (androidx.core.app.ActivityCompat.checkSelfPermission(this,
                permission) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            androidx.core.app.ActivityCompat.requestPermissions(this, new String[] { permission }, 201);
            // Store folderUri for later use after permission is granted
            this.pendingImportUri = folderUri;
        } else {
            Toast.makeText(this, "Importing music using MediaStore...", Toast.LENGTH_SHORT).show();
            // Use MediaStore for importing music instead of folder scanning
            // MediaStore is system-wide and more efficient
            importMusicFromMediaStore(folderUri);
        }
    }

    /**
     * Scan all device music using MediaStore (system-wide scan)
     * This method can be called for a full device scan, separate from folder
     * imports
     */
    public void scanAllDeviceMusic() {
        Toast.makeText(this, "Scanning all device music...", Toast.LENGTH_SHORT).show();
        importMusicFromMediaStore(null); // Pass null since we're not scanning a specific folder
    }

    /**
     * Use MediaStore for system-wide indexed songs if possible,
     * otherwise fall back to manual recursive scan.
     */
    private void importMusicFromMediaStore(Uri folderUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        final View progressView = getLayoutInflater().inflate(R.layout.dialog_import_progress, null);
        builder.setView(progressView);

        AlertDialog dialog = builder.create();

        // Set transparent background to remove white background behind CardView
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialog.show();

        ProgressBar progressBar = progressView.findViewById(R.id.progressBar);
        TextView textStatus = progressView.findViewById(R.id.textStatus);
        TextView textCount = progressView.findViewById(R.id.textCount);

        new Thread(() -> {
            final int[] duplicates = { 0 };
            int newCount = 0;

            try {
                ContentResolver resolver = getContentResolver();

                // Get all available volumes (internal storage, SDCard, etc.) to avoid
                // duplicates
                java.util.Set<String> volumes = new java.util.HashSet<>();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ - Use getExternalVolumeNames to get all volumes
                    volumes.addAll(MediaStore.getExternalVolumeNames(this));
                } else {
                    // Fallback for older Android versions
                    volumes.add(MediaStore.VOLUME_EXTERNAL);
                }

                String[] projection = {
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.DURATION
                };

                int totalSongs = 0;

                // First pass: Count total songs across all volumes
                runOnUiThread(() -> {
                    textStatus.setText("Scanning all storage volumes...");
                    textCount.setText("Counting songs...");
                });

                for (String volume : volumes) {
                    Uri uri = MediaStore.Audio.Media.getContentUri(volume);
                    android.util.Log.d("ImportMusic", "Scanning volume: " + volume + " at URI: " + uri);

                    try (Cursor cursor = resolver.query(uri, projection, null, null, null)) {
                        if (cursor != null) {
                            totalSongs += cursor.getCount();
                            android.util.Log.d("ImportMusic",
                                    "Volume " + volume + " has " + cursor.getCount() + " songs");
                        }
                    } catch (Exception e) {
                        android.util.Log.w("ImportMusic",
                                "Failed to scan volume " + volume + ": " + e.getMessage());
                    }
                }

                if (totalSongs == 0) {
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        if (folderUri != null) {
                            Toast.makeText(this, "No MediaStore data, scanning folder manually...",
                                    Toast.LENGTH_SHORT).show();
                            importMusicFromFolder(folderUri);
                        } else {
                            Toast.makeText(this, "No music found on any storage volume", Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }

                final int finalTotalSongs = totalSongs;
                runOnUiThread(() -> {
                    progressBar.setMax(finalTotalSongs);
                    textStatus.setText("Found " + finalTotalSongs + " songs across " + volumes.size() + " volume(s)");
                    textCount.setText("0 / " + finalTotalSongs);
                });

                // Second pass: Actually import songs from all volumes
                final int BATCH_SIZE = 50;
                List<Song> batch = new ArrayList<>();
                int processed = 0;

                for (String volume : volumes) {
                    Uri uri = MediaStore.Audio.Media.getContentUri(volume);
                    android.util.Log.d("ImportMusic", "Processing volume: " + volume);

                    runOnUiThread(() -> {
                        textStatus.setText("Importing from " + volume + " storage...");
                    });

                    try (Cursor cursor = resolver.query(uri, projection, null, null, null)) {
                        if (cursor == null)
                            continue;

                        while (cursor.moveToNext()) {
                            long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                            String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                            String artist = cursor
                                    .getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                            String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                            long duration = cursor
                                    .getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));

                            // Skip non-existent files
                            if (path == null)
                                continue;

                            Uri contentUri = ContentUris.withAppendedId(uri, id);

                            android.util.Log.d("ImportMusic",
                                    "Processing: " + title + " by " + artist + " from " + volume);

                            // Extract and save embedded album art if present
                            String albumArtPath = null;
                            try {
                                android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();

                                // Try content URI first, fallback to file path for SDCard compatibility
                                try {
                                    retriever.setDataSource(getApplicationContext(), contentUri);
                                } catch (Exception e) {
                                    android.util.Log.d("ImportMusic",
                                            "Content URI failed for " + title + ", trying file path");
                                    retriever.setDataSource(path);
                                }

                                byte[] art = retriever.getEmbeddedPicture();
                                if (art != null) {
                                    android.util.Log.d("ImportMusic", "✓ FOUND embedded album art in: " + title
                                            + " (size: " + art.length + " bytes)");

                                    // Save embedded art to internal storage
                                    albumArtPath = saveEmbeddedAlbumArt(art, title, artist);
                                    if (albumArtPath != null) {
                                        android.util.Log.d("ImportMusic", "✓ SAVED album art to: " + albumArtPath);
                                    }
                                } else {
                                    android.util.Log.d("ImportMusic", "✗ NO embedded album art in: " + title);
                                }
                                retriever.release();
                            } catch (Exception e) {
                                android.util.Log.d("ImportMusic",
                                        "Error extracting album art for " + title + ": " + e.getMessage());
                            }

                            Song song = new Song(title, artist, contentUri.toString(), (int) duration, albumArtPath);
                            batch.add(song);

                            // Process batch when it reaches BATCH_SIZE or on last item
                            if (batch.size() >= BATCH_SIZE || (!cursor.isAfterLast() && cursor.isLast())) {
                                int[] result = dbHelper.addSongsIfNotExistBatch(batch);
                                newCount += result[0];
                                duplicates[0] += result[1];
                                processed += batch.size();

                                // Update UI
                                int finalProcessed = processed;
                                String finalTitle = title;
                                String finalVolume = volume;
                                runOnUiThread(() -> {
                                    textStatus.setText("Importing: " + finalTitle + " (" + finalVolume + ")");
                                    textCount.setText(finalProcessed + " / " + finalTotalSongs);
                                    progressBar.setProgress(finalProcessed);
                                });

                                batch.clear();
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.w("ImportMusic",
                                "Error scanning volume " + volume + ": " + e.getMessage());
                    }
                }

                // Process any remaining songs
                if (!batch.isEmpty()) {
                    int[] result = dbHelper.addSongsIfNotExistBatch(batch);
                    newCount += result[0];
                    duplicates[0] += result[1];
                    processed += batch.size();

                    int finalProcessed = processed;
                    runOnUiThread(() -> {
                        textCount.setText(finalProcessed + " / " + finalTotalSongs);
                        progressBar.setProgress(finalProcessed);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    dialog.dismiss();
                    if (folderUri != null) {
                        Toast.makeText(this, "Error using MediaStore. Falling back to folder scan.", Toast.LENGTH_SHORT)
                                .show();
                        importMusicFromFolder(folderUri);
                    } else {
                        Toast.makeText(this, "Error scanning device music: " + e.getMessage(), Toast.LENGTH_LONG)
                                .show();
                    }
                });
                return;
            }

            int finalNewCount = newCount;
            int finalDuplicates = duplicates[0];
            runOnUiThread(() -> {
                dialog.dismiss();
                if (finalNewCount == 0 && finalDuplicates == 0) {
                    Toast.makeText(this, "No songs found", Toast.LENGTH_LONG).show();
                } else if (finalNewCount == 0) {
                    Toast.makeText(this, "Added 0 new songs. " + finalDuplicates + " duplicate(s) skipped.",
                            Toast.LENGTH_LONG).show();
                } else {
                    String message = "Imported " + finalNewCount
                            + " new song(s) with album art from all storage volumes";
                    if (finalDuplicates > 0) {
                        message += ". " + finalDuplicates + " duplicate(s) skipped.";
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                }

                // Refresh HomeFragment after import to update the song list
                if (finalNewCount > 0) {
                    refreshHomeFragment();
                }
            });
        }).start();
    }

    /**
     * Manual SAF-based recursive scanner — used only if MediaStore fails
     */
    private void importMusicFromFolder(Uri treeUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        final View progressView = getLayoutInflater().inflate(R.layout.dialog_import_progress, null);
        builder.setView(progressView);

        AlertDialog dialog = builder.create();

        // Set transparent background to remove white background behind CardView
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialog.show();

        ProgressBar progressBar = progressView.findViewById(R.id.progressBar);
        TextView textStatus = progressView.findViewById(R.id.textStatus);
        TextView textCount = progressView.findViewById(R.id.textCount);

        new Thread(() -> {
            List<Song> importedSongs = new ArrayList<>();
            final int[] duplicates = { 0 }; // Use array to allow modification in callback
            int total = countAudioFiles(treeUri);
            final int totalFiles = Math.max(total, 1);
            final int BATCH_SIZE = 20; // Update UI every 20 songs

            runOnUiThread(() -> {
                progressBar.setMax(totalFiles);
                textStatus.setText("Scanning...");
                textCount.setText("0 / " + totalFiles);
            });

            scanFolderRecursively(treeUri, importedSongs, duplicates, new ImportProgressCallback() {
                int imported = 0;
                String lastSongName = "";

                @Override
                public void onSongDetected(String name) {
                    imported++;
                    lastSongName = name;

                    // Update UI only every BATCH_SIZE songs or on last song
                    if (imported % BATCH_SIZE == 0 || imported == totalFiles) {
                        int finalImported = imported;
                        String finalName = lastSongName;
                        runOnUiThread(() -> {
                            textStatus.setText("Importing: " + finalName);
                            textCount.setText(finalImported + " / " + totalFiles);
                            progressBar.setProgress(finalImported);
                        });
                    }
                }
            });

            int newCount = importedSongs.size();
            int finalDuplicates = duplicates[0];
            runOnUiThread(() -> {
                dialog.dismiss();
                if (newCount == 0 && finalDuplicates == 0) {
                    Toast.makeText(this, "No songs found", Toast.LENGTH_LONG).show();
                } else if (newCount == 0) {
                    Toast.makeText(this, "Added 0 new songs. " + finalDuplicates + " duplicate(s) skipped.",
                            Toast.LENGTH_LONG).show();
                } else {
                    String message = "Imported " + newCount + " new song(s)";
                    if (finalDuplicates > 0) {
                        message += ". " + finalDuplicates + " duplicate(s) skipped.";
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                }

                // Refresh HomeFragment after import
                if (newCount > 0) {
                    refreshHomeFragment();
                }
            });
        }).start();
    }

    private void scanFolderRecursively(Uri folderUri, List<Song> importedSongs, int[] duplicates,
            ImportProgressCallback callback) {
        ContentResolver resolver = getContentResolver();
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                folderUri, DocumentsContract.getTreeDocumentId(folderUri));

        try (Cursor cursor = resolver.query(childrenUri,
                new String[] {
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                },
                null, null, null)) {

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String docId = cursor.getString(0);
                    String displayName = cursor.getString(1);
                    String mimeType = cursor.getString(2);

                    Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId);

                    if (mimeType == null)
                        continue;

                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                        scanFolderRecursively(fileUri, importedSongs, duplicates, callback);
                    } else if (mimeType.startsWith("audio/")) {
                        // Extract metadata from audio file
                        String title = displayName;
                        String artist = "Unknown Artist";
                        long duration = 0;
                        String albumArtPath = null;

                        try {
                            android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                            retriever.setDataSource(getApplicationContext(), fileUri);

                            // Extract basic metadata
                            String retrievedTitle = retriever
                                    .extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE);
                            String retrievedArtist = retriever
                                    .extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST);
                            String retrievedDuration = retriever
                                    .extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);

                            if (retrievedTitle != null && !retrievedTitle.trim().isEmpty()) {
                                title = retrievedTitle;
                            } else {
                                // Remove file extension from display name if title is not available
                                title = displayName.replaceFirst("\\.[^.]*$", "");
                            }

                            if (retrievedArtist != null && !retrievedArtist.trim().isEmpty()) {
                                artist = retrievedArtist;
                            }

                            if (retrievedDuration != null) {
                                try {
                                    duration = Long.parseLong(retrievedDuration);
                                } catch (NumberFormatException e) {
                                    duration = 0;
                                }
                            }

                            // Extract and save embedded album art if present
                            byte[] art = retriever.getEmbeddedPicture();
                            if (art != null) {
                                android.util.Log.d("FolderScan", "✓ FOUND embedded album art in: " + title + " (size: "
                                        + art.length + " bytes)");

                                // Save embedded art to internal storage
                                albumArtPath = saveEmbeddedAlbumArt(art, title, artist);
                                if (albumArtPath != null) {
                                    android.util.Log.d("FolderScan", "✓ SAVED album art to: " + albumArtPath);
                                }
                            } else {
                                android.util.Log.d("FolderScan", "✗ NO embedded album art in: " + title);
                            }

                            retriever.release();
                        } catch (Exception e) {
                            android.util.Log.d("FolderScan",
                                    "Error extracting metadata for " + displayName + ": " + e.getMessage());
                            // Keep default values if extraction fails
                        }

                        Song song = new Song(title, artist, fileUri.toString(), (int) duration, albumArtPath);

                        // Only add if not duplicate
                        if (dbHelper.addSongIfNotExists(song)) {
                            importedSongs.add(song);
                        } else {
                            duplicates[0]++;
                        }

                        if (callback != null)
                            callback.onSongDetected(title);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int countAudioFiles(Uri folderUri) {
        int count = 0;
        ContentResolver resolver = getContentResolver();
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                folderUri, DocumentsContract.getTreeDocumentId(folderUri));

        try (Cursor cursor = resolver.query(childrenUri,
                new String[] {
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                },
                null, null, null)) {

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String docId = cursor.getString(0);
                    String mimeType = cursor.getString(1);
                    Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId);

                    if (mimeType == null)
                        continue;

                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                        count += countAudioFiles(fileUri);
                    } else if (mimeType.startsWith("audio/")) {
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    private interface ImportProgressCallback {
        void onSongDetected(String name);
    }

    // QueueFragment.OnQueueFragmentListener implementation
    @Override
    public void onBackPressed() {
        getSupportFragmentManager().popBackStack();
    }

    @Override
    public MusicService getMusicService() {
        return musicService;
    }

    @Override
    public boolean isMusicServiceBound() {
        return isBound;
    }

    // FavoritesFragment.OnFavoritesFragmentListener implementation
    // onBackPressed(), getMusicService(), and isMusicServiceBound() are already
    // implemented above
    // onSongSelected() is already implemented for
    // HomeFragment.OnSongSelectedListener
    // addSongToQueue() is already implemented as a public method above

    /**
     * Update existing songs in database with extracted embedded album art
     * This method scans all existing songs and extracts/saves their embedded album
     * art
     */
    public void updateExistingSongsWithAlbumArt() {
        Toast.makeText(this, "Updating album art for existing songs...", Toast.LENGTH_SHORT).show();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        final View progressView = getLayoutInflater().inflate(R.layout.dialog_import_progress, null);
        builder.setView(progressView);

        AlertDialog dialog = builder.create();

        // Set transparent background to remove white background behind CardView
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialog.show();

        ProgressBar progressBar = progressView.findViewById(R.id.progressBar);
        TextView textStatus = progressView.findViewById(R.id.textStatus);
        TextView textCount = progressView.findViewById(R.id.textCount);

        new Thread(() -> {
            SongDatabaseHelper dbHelper = new SongDatabaseHelper(this);
            List<Song> allSongs = dbHelper.getAllSongs();
            int total = allSongs.size();
            int processed = 0;
            int updated = 0;

            runOnUiThread(() -> {
                progressBar.setMax(total);
                textStatus.setText("Checking songs for album art...");
                textCount.setText("0 / " + total);
            });

            for (Song song : allSongs) {
                // Skip songs that already have saved album art
                if (song.getAlbumCoverPath() != null && !song.getAlbumCoverPath().isEmpty()) {
                    processed++;
                    continue;
                }

                try {
                    Uri audioUri = Uri.parse(song.getUri());
                    android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                    retriever.setDataSource(this, audioUri);
                    byte[] art = retriever.getEmbeddedPicture();

                    if (art != null) {
                        android.util.Log.d("AlbumArtUpdate", "✓ FOUND embedded art in: " + song.getTitle());

                        // Save embedded art to storage
                        String albumArtPath = saveEmbeddedAlbumArt(art, song.getTitle(), song.getArtist());
                        if (albumArtPath != null) {
                            // Update song in database with album art path
                            song.setAlbumCoverPath(albumArtPath);
                            dbHelper.updateSong(song);
                            updated++;
                            android.util.Log.d("AlbumArtUpdate", "✓ SAVED album art for: " + song.getTitle());
                        }
                    }

                    retriever.release();
                } catch (Exception e) {
                    android.util.Log.w("AlbumArtUpdate",
                            "Failed to extract art for: " + song.getTitle() + " - " + e.getMessage());
                }

                processed++;
                final int finalProcessed = processed;
                final int finalUpdated = updated;
                runOnUiThread(() -> {
                    textStatus.setText("Processing: " + song.getTitle());
                    textCount.setText(finalProcessed + " / " + total + " (" + finalUpdated + " updated)");
                    progressBar.setProgress(finalProcessed);
                });
            }

            final int finalUpdated = updated;
            runOnUiThread(() -> {
                dialog.dismiss();
                Toast.makeText(this, "Updated " + finalUpdated + " songs with album art!", Toast.LENGTH_LONG).show();

                // Refresh the home fragment to show updated album art
                refreshHomeFragment();
            });
        }).start();
    }

    /**
     * Check if there are songs without album art and offer to update them
     * automatically
     */
    private void checkAndOfferAlbumArtUpdate() {
        new Thread(() -> {
            SongDatabaseHelper dbHelper = new SongDatabaseHelper(this);
            List<Song> allSongs = dbHelper.getAllSongs();

            // Count songs without album art
            int songsWithoutArt = 0;
            for (Song song : allSongs) {
                if (song.getAlbumCoverPath() == null || song.getAlbumCoverPath().isEmpty()) {
                    songsWithoutArt++;
                }
            }

            final int finalCount = songsWithoutArt;

            // Only show dialog if there are songs without album art
            if (finalCount > 0) {
                runOnUiThread(() -> {
                    showAlbumArtUpdateDialog(finalCount);
                });
            }
        }).start();
    }

    /**
     * Show dialog offering to update album art for songs that don't have it
     */
    private void showAlbumArtUpdateDialog(int songsWithoutArt) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Album Art Missing");
        builder.setMessage("Found " + songsWithoutArt
                + " songs without album art. Would you like to extract album art from these songs now?\n\nThis will scan your music files for embedded album artwork.");

        builder.setPositiveButton("Update Now", (dialog, which) -> {
            updateExistingSongsWithAlbumArt();
        });

        builder.setNegativeButton("Later", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.setNeutralButton("Don't Ask Again", (dialog, which) -> {
            // Save preference to not ask again
            android.content.SharedPreferences prefs = getSharedPreferences("musebox_prefs", MODE_PRIVATE);
            prefs.edit().putBoolean("album_art_check_disabled", true).apply();
            dialog.dismiss();
        });

        // Only show if user hasn't disabled this check
        android.content.SharedPreferences prefs = getSharedPreferences("musebox_prefs", MODE_PRIVATE);
        if (!prefs.getBoolean("album_art_check_disabled", false)) {
            builder.show();
        }
    }

    /**
     * Load album art for mini player asynchronously using Glide with caching.
     * Prioritizes custom album cover path over embedded audio file art.
     */
    private void loadMiniPlayerAlbumArt(Song song) {
        if (song == null || imgSongArt == null) {
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
                    .error(R.drawable.ic_music_note)
                    .centerCrop()
                    .into(imgSongArt);
        } else {
            // No custom cover, load embedded art from audio file
            loadMiniPlayerEmbeddedAlbumArt(song.getUri());
        }
    }

    /**
     * Load embedded album art from audio file for mini player
     */
    private void loadMiniPlayerEmbeddedAlbumArt(String audioFilePath) {
        if (imgSongArt == null) {
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
                        android.util.Log.w("MiniPlayerAlbumArt", "Failed to load album art");
                        return false; // Let Glide handle showing error drawable
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.Bitmap resource, Object model,
                            com.bumptech.glide.request.target.Target<android.graphics.Bitmap> target,
                            com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        return false; // Let Glide handle the success case
                    }
                })
                .into(imgSongArt);
    }

    /**
     * Save embedded album art to internal storage
     * 
     * @param artBytes  The embedded album art bytes
     * @param songTitle The song title (for unique filename)
     * @param artist    The artist name (for unique filename)
     * @return The file path of saved album art, or null if save failed
     */
    public String saveEmbeddedAlbumArt(byte[] artBytes, String songTitle, String artist) {
        try {
            // Create album art directory in internal storage
            java.io.File albumArtDir = new java.io.File(getFilesDir(), "album_art");
            if (!albumArtDir.exists()) {
                albumArtDir.mkdirs();
            }

            // Create unique filename based on song and artist
            String sanitizedTitle = sanitizeFileName(songTitle);
            String sanitizedArtist = sanitizeFileName(artist);
            String filename = sanitizedArtist + "_" + sanitizedTitle + ".jpg";

            java.io.File artFile = new java.io.File(albumArtDir, filename);

            // Don't overwrite if file already exists
            if (artFile.exists()) {
                return artFile.getAbsolutePath();
            }

            // Write bytes to file
            java.io.FileOutputStream fos = new java.io.FileOutputStream(artFile);
            fos.write(artBytes);
            fos.close();

            return artFile.getAbsolutePath();
        } catch (Exception e) {
            android.util.Log.e("AlbumArt", "Failed to save album art: " + e.getMessage());
            return null;
        }
    }

    /**
     * Sanitize filename by removing invalid characters
     */
    private String sanitizeFileName(String input) {
        if (input == null)
            return "unknown";
        return input.replaceAll("[^a-zA-Z0-9._-]", "_").substring(0, Math.min(input.length(), 50));
    }

    private void refreshHomeFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_container);
        if (fragment instanceof HomeFragment) {
            ((HomeFragment) fragment).refreshSongs();
        }
    }
}