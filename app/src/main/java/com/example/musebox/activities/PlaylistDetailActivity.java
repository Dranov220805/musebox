package com.example.musebox.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.musebox.R;
import com.example.musebox.adapters.SongAdapter;
import com.example.musebox.database.PlaylistDatabaseHelper;
import com.example.musebox.database.SongDatabaseHelper;
import com.example.musebox.models.Playlist;
import com.example.musebox.models.Song;
import com.example.musebox.services.MusicService;
import com.example.musebox.utils.PlaylistDialogHelper;

import java.util.List;

public class PlaylistDetailActivity extends AppCompatActivity {
    private TextView tvTitle, tvDescription;
    private RecyclerView recyclerSongs;
    private SongAdapter songAdapter;
    private PlaylistDatabaseHelper dbHelper;
    private SongDatabaseHelper songDbHelper;
    private ImageButton btnAddSong, btnBack, btnMenu;
    private String playlistId;
    private Playlist playlist;

    // Mini player views
    private View miniPlayer;
    private TextView tvMiniSongTitle;
    private TextView tvMiniArtistName;
    private ImageButton btnMiniPlayPause, btnMiniQueue;
    private com.example.musebox.views.CircularProgressView circularProgress;
    private ImageView imgMiniSongArt;

    // Handler for updating progress
    private android.os.Handler progressHandler = new android.os.Handler();
    private Runnable progressRunnable;

    // Music service
    private MusicService musicService;
    private boolean serviceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            serviceBound = true;

            // Update mini player if there's a song playing
            Song currentSong = musicService.getCurrentSong();
            if (currentSong != null) {
                miniPlayer.setVisibility(View.VISIBLE);
                updateMiniPlayer();
                progressHandler.post(progressRunnable);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        // Hide action bar since we have custom header
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        tvTitle = findViewById(R.id.tvPlaylistTitle);
        tvDescription = findViewById(R.id.tvPlaylistDescription);
        recyclerSongs = findViewById(R.id.recyclerPlaylistSongs);
        btnAddSong = findViewById(R.id.btnAddSong);
        btnBack = findViewById(R.id.btnBack);
        btnMenu = findViewById(R.id.btnMenu);
        dbHelper = new PlaylistDatabaseHelper(this);
        songDbHelper = new SongDatabaseHelper(this);

        // Initialize mini player views
        miniPlayer = findViewById(R.id.includeMiniPlayer);
        tvMiniSongTitle = miniPlayer.findViewById(R.id.txtSongTitle);
        tvMiniArtistName = miniPlayer.findViewById(R.id.txtArtistName);
        btnMiniPlayPause = miniPlayer.findViewById(R.id.btnPlayPause);
        btnMiniQueue = miniPlayer.findViewById(R.id.btnQueue);
        circularProgress = miniPlayer.findViewById(R.id.circularProgress);
        imgMiniSongArt = miniPlayer.findViewById(R.id.imgSongArt);

        // Setup progress updater
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (musicService != null) {
                    // Update play/pause button state
                    if (musicService.isPlaying()) {
                        btnMiniPlayPause.setImageResource(R.drawable.ic_pause);
                    } else {
                        btnMiniPlayPause.setImageResource(R.drawable.ic_play);
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
            Intent intent = new Intent(PlaylistDetailActivity.this, FullPlayerActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        btnMiniPlayPause.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.pauseOrResume();
                if (musicService.isPlaying()) {
                    btnMiniPlayPause.setImageResource(R.drawable.ic_pause);
                } else {
                    btnMiniPlayPause.setImageResource(R.drawable.ic_play);
                }
            }
        });

        // Queue button - open queue activity
        btnMiniQueue.setOnClickListener(v -> {
            Intent intent = new Intent(PlaylistDetailActivity.this, QueueActivity.class);
            startActivity(intent);
        });

        // Set back button click listener
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Bind to MusicService
        Intent serviceIntent = new Intent(this, MusicService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        playlistId = getIntent().getStringExtra("playlist_id");
        if (playlistId == null) {
            Toast.makeText(this, "Playlist not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadPlaylist();

        if (btnAddSong != null) {
            btnAddSong.setOnClickListener(v -> showAddSongDialog());
        }

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> showPlaylistOptionsMenu(v));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressHandler.removeCallbacks(progressRunnable);
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update mini player when returning from FullPlayerActivity
        if (serviceBound && musicService != null) {
            Song currentSong = musicService.getCurrentSong();
            if (currentSong != null) {
                miniPlayer.setVisibility(View.VISIBLE);
                updateMiniPlayer();
                progressHandler.post(progressRunnable);
            } else {
                miniPlayer.setVisibility(View.GONE);
                progressHandler.removeCallbacks(progressRunnable);
            }
        }
    }

    private void updateMiniPlayer() {
        if (musicService == null)
            return;

        Song currentSong = musicService.getCurrentSong();
        if (currentSong != null) {
            tvMiniSongTitle.setText(currentSong.getTitle());
            tvMiniArtistName.setText(currentSong.getArtist());

            // Update play/pause button
            if (musicService.isPlaying()) {
                btnMiniPlayPause.setImageResource(R.drawable.ic_pause);
            } else {
                btnMiniPlayPause.setImageResource(R.drawable.ic_play);
            }

            // Load album art using the same method as HomeActivity
            loadMiniPlayerAlbumArt(currentSong);

            // Show mini player
            miniPlayer.setVisibility(View.VISIBLE);
        } else {
            miniPlayer.setVisibility(View.GONE);
        }
    }

    /**
     * Load album art for mini player - same as HomeActivity
     */
    private void loadMiniPlayerAlbumArt(Song song) {
        if (song == null || imgMiniSongArt == null) {
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
                    .into(imgMiniSongArt);
        } else {
            // No custom cover, load embedded art from audio file
            loadMiniPlayerEmbeddedAlbumArt(song.getUri());
        }
    }

    /**
     * Load embedded album art from audio file for mini player - same as
     * HomeActivity
     */
    private void loadMiniPlayerEmbeddedAlbumArt(String audioFilePath) {
        if (imgMiniSongArt == null) {
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
                .into(imgMiniSongArt);
    }

    private void showPlaylistOptionsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.inflate(R.menu.menu_playlist_options);

        // Set text color to white for menu items
        android.view.Menu menu = popup.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            android.view.MenuItem menuItem = menu.getItem(i);
            android.text.SpannableString spanString = new android.text.SpannableString(menuItem.getTitle().toString());
            spanString.setSpan(new android.text.style.ForegroundColorSpan(getResources().getColor(R.color.white, null)),
                    0, spanString.length(), 0);
            menuItem.setTitle(spanString);
        }

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_edit_playlist) {
                showEditPlaylistDialog();
                return true;
            } else if (itemId == R.id.menu_delete_playlist) {
                showDeletePlaylistDialog();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showEditPlaylistDialog() {
        if (playlist == null)
            return;

        PlaylistDialogHelper.showEditPlaylistDialog(this, playlist, (playlistId, newName, newDescription) -> {
            // Reload playlist to show updated info
            loadPlaylist();
        });
    }

    private void showDeletePlaylistDialog() {
        if (playlist == null)
            return;

        PlaylistDialogHelper.showDeletePlaylistDialog(this, playlist, deletedPlaylist -> {
            boolean success = dbHelper.deletePlaylist(deletedPlaylist.getId());
            if (success) {
                Toast.makeText(this, "Playlist deleted", Toast.LENGTH_SHORT).show();
                finish(); // Close activity after deletion
            } else {
                Toast.makeText(this, "Failed to delete playlist", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddSongDialog() {
        // Safety check - ensure playlist is loaded
        if (playlist == null) {
            Toast.makeText(this, "Playlist not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get all songs from song database
        List<Song> allSongs = songDbHelper.getAllSongs();

        // Filter out songs already in this playlist
        List<Song> playlistSongs = playlist.getSongs();
        List<Song> availableSongs = new java.util.ArrayList<>();
        for (Song song : allSongs) {
            boolean alreadyInPlaylist = false;
            for (Song playlistSong : playlistSongs) {
                if (playlistSong.getId().equals(song.getId())) {
                    alreadyInPlaylist = true;
                    break;
                }
            }
            if (!alreadyInPlaylist) {
                availableSongs.add(song);
            }
        }

        if (availableSongs.isEmpty()) {
            Toast.makeText(this, "All songs are already in this playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create dialog with song selection
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_songs, null);
        RecyclerView recyclerSongs = dialogView.findViewById(R.id.recyclerSongs);

        // Create adapter for song selection
        SongSelectAdapter selectAdapter = new SongSelectAdapter(availableSongs);
        recyclerSongs.setLayoutManager(new LinearLayoutManager(this));
        recyclerSongs.setAdapter(selectAdapter);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Add Songs to Playlist")
                .setView(dialogView)
                .setPositiveButton("Add", (d, which) -> {
                    List<Song> selectedSongs = selectAdapter.getSelectedSongs();
                    if (selectedSongs.isEmpty()) {
                        Toast.makeText(this, "No songs selected", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Add each selected song to the playlist
                    for (Song song : selectedSongs) {
                        dbHelper.addSongToPlaylist(playlistId, song);
                    }

                    Toast.makeText(this, "Added " + selectedSongs.size() + " song(s)", Toast.LENGTH_SHORT).show();
                    loadPlaylist(); // Reload the playlist
                })
                .setNegativeButton("Cancel", null)
                .create();

        // Set dialog background to include buttons
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg_with_inset);
        }

        dialog.show();

        // Set title color to Spotify green
        android.widget.TextView titleView = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if (titleView != null) {
            titleView.setTextColor(getResources().getColor(R.color.spotify_green, null));
        }
    }

    private void loadPlaylist() {
        playlist = dbHelper.getPlaylistWithSongs(playlistId);
        if (playlist == null) {
            Toast.makeText(this, "Playlist not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        tvTitle.setText(playlist.getName());
        tvDescription.setText(playlist.getDescription());

        // Load songs from playlist
        List<Song> songs = playlist.getSongs();

        // Initialize adapter with songs
        songAdapter = new SongAdapter();
        songAdapter.setSongs(songs);
        recyclerSongs.setLayoutManager(new LinearLayoutManager(this));
        recyclerSongs.setAdapter(songAdapter);

        // Set click listeners for playing songs
        songAdapter.setOnSongClickListener(song -> {
            // Play song from playlist
            if (serviceBound && musicService != null && songs != null && !songs.isEmpty()) {
                int position = songs.indexOf(song);
                if (position >= 0) {
                    // Set the entire playlist in the music service
                    musicService.setPlaylist(songs, position);

                    // Play the selected song
                    musicService.playSong(
                            Uri.parse(song.getUri()),
                            song.getTitle(),
                            song.getArtist());

                    // Update mini player
                    miniPlayer.setVisibility(View.VISIBLE);
                    updateMiniPlayer();
                    progressHandler.post(progressRunnable);

                    // Open full player
                    Intent intent = new Intent(PlaylistDetailActivity.this, FullPlayerActivity.class);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);

                    Toast.makeText(this, "Playing: " + song.getTitle(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Music service not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Set menu listener for song options
        songAdapter.setOnSongMenuListener(new SongAdapter.OnSongMenuListener() {
            @Override
            public void onAddToQueue(Song song) {
                // Add to queue
                if (serviceBound && musicService != null) {
                    musicService.addToQueue(song);
                    Toast.makeText(PlaylistDetailActivity.this, "Added to queue", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PlaylistDetailActivity.this, "Music service not available", Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void onAddToFavourite(Song song) {
                // Toggle favorite status
                boolean isFavorite = songDbHelper.isFavorite(song.getId());
                if (isFavorite) {
                    songDbHelper.removeFromFavorites(song.getId());
                    Toast.makeText(PlaylistDetailActivity.this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                } else {
                    songDbHelper.addToFavorites(song.getId());
                    Toast.makeText(PlaylistDetailActivity.this, "Added to favorites", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAddToPlaylist(Song song) {
                // Remove from THIS playlist instead of adding to another
                showRemoveSongDialog(song, songs.indexOf(song));
            }

            @Override
            public void onDeleteSong(Song song, int position) {
                showRemoveSongDialog(song, position);
            }
        });
    }

    private void showRemoveSongDialog(Song song, int position) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Remove Song")
                .setMessage("Remove \"" + song.getTitle() + "\" from this playlist?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    boolean removed = dbHelper.removeSongFromPlaylist(playlistId, song.getId());
                    if (removed) {
                        Toast.makeText(this, "Song removed", Toast.LENGTH_SHORT).show();
                        loadPlaylist(); // Reload the playlist
                    } else {
                        Toast.makeText(this, "Failed to remove song", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Inner adapter for song selection with checkboxes
    private class SongSelectAdapter extends RecyclerView.Adapter<SongSelectAdapter.ViewHolder> {
        private final List<Song> songs;
        private final java.util.HashSet<Song> selectedSongs = new java.util.HashSet<>();

        public SongSelectAdapter(List<Song> songs) {
            this.songs = songs;
        }

        public List<Song> getSelectedSongs() {
            return new java.util.ArrayList<>(selectedSongs);
        }

        @androidx.annotation.NonNull
        @Override
        public ViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_song_select, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull ViewHolder holder, int position) {
            Song song = songs.get(position);
            holder.tvTitle.setText(song.getTitle());
            holder.tvArtist.setText(song.getArtist());
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(selectedSongs.contains(song));
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedSongs.add(song);
                } else {
                    selectedSongs.remove(song);
                }
            });
        }

        @Override
        public int getItemCount() {
            return songs.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.CheckBox checkBox;
            TextView tvTitle, tvArtist;

            ViewHolder(View itemView) {
                super(itemView);
                checkBox = itemView.findViewById(R.id.cbSelectSong);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvArtist = itemView.findViewById(R.id.tvArtist);
            }
        }
    }
}
