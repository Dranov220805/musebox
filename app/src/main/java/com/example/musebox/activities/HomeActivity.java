package com.example.musebox.activities;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
    private ImageButton btnPlayPause, btnSpeed, btnQueue;
    private SeekBar seekBar;
    private com.example.musebox.views.CircularProgressView circularProgress;

    // Handler for updating progress
    private android.os.Handler progressHandler = new android.os.Handler();
    private Runnable progressRunnable;

    // Current song and playlist
    private Song currentSong;
    private List<Song> currentPlaylist = new ArrayList<>();
    private String lastSongTitle = "";

    // ServiceConnection to bind with MusicService
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            isBound = true;
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
        btnPlayPause = miniPlayer.findViewById(R.id.btnPlayPause);
        btnQueue = miniPlayer.findViewById(R.id.btnQueue);
        circularProgress = miniPlayer.findViewById(R.id.circularProgress);

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

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_container, new HomeFragment())
                    .commit();

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.navigation_container, new NavigationBarFragment())
                    .commit();
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
        if (musicService != null) {
            // Update mini player with current song info
            updateMiniPlayer();
            // Start progress updates
            progressHandler.post(progressRunnable);
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

            // Show mini player
            miniPlayer.setVisibility(View.VISIBLE);

            // Reset circular progress
            circularProgress.setProgress(0);

            // Start progress updates
            progressHandler.post(progressRunnable);

            // Update UI
            tvSongTitle.setText(song.getTitle());
            btnPlayPause.setImageResource(R.drawable.ic_pause);

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

            // Show mini player
            miniPlayer.setVisibility(View.VISIBLE);

            // Reset circular progress
            circularProgress.setProgress(0);

            // Start progress updates
            progressHandler.post(progressRunnable);

            // Update UI
            tvSongTitle.setText(song.getTitle());
            btnPlayPause.setImageResource(R.drawable.ic_pause);

            Toast.makeText(this, "Playing: " + song.getTitle(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Music service not available", Toast.LENGTH_SHORT).show();
        }
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

                // Update current song reference
                currentSong = musicService.getCurrentSong();
                lastSongTitle = title;

                // Refresh fragments if they are currently visible
                refreshCurrentFragment();
            } else {
                // No song playing, hide mini player
                miniPlayer.setVisibility(View.GONE);
                currentSong = null;
                lastSongTitle = "";
            }
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
        Toast.makeText(this, "Importing music from: " + folderUri, Toast.LENGTH_SHORT).show();
        // Try using MediaStore first
        importMusicFromMediaStore(folderUri);
    }

    /**
     * Use MediaStore for system-wide indexed songs if possible,
     * otherwise fall back to manual recursive scan.
     */
    private void importMusicFromMediaStore(Uri folderUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Importing Music");
        builder.setCancelable(false);

        final View progressView = getLayoutInflater().inflate(R.layout.dialog_import_progress, null);
        builder.setView(progressView);

        AlertDialog dialog = builder.create();
        dialog.show();

        ProgressBar progressBar = progressView.findViewById(R.id.progressBar);
        TextView textStatus = progressView.findViewById(R.id.textStatus);
        TextView textCount = progressView.findViewById(R.id.textCount);

        new Thread(() -> {
            final int[] duplicates = { 0 }; // Declare outside try block
            int newCount = 0;

            try {
                ContentResolver resolver = getContentResolver();
                Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

                String[] projection = {
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.DURATION
                };

                try (Cursor cursor = resolver.query(uri, projection, null, null, null)) {
                    if (cursor == null || cursor.getCount() == 0) {
                        // If MediaStore gives nothing, fall back
                        runOnUiThread(() -> {
                            Toast.makeText(this, "No MediaStore data, scanning manually...", Toast.LENGTH_SHORT).show();
                        });
                        dialog.dismiss();
                        importMusicFromFolder(folderUri);
                        return;
                    }

                    int total = cursor.getCount();
                    final int BATCH_SIZE = 50; // Process 50 songs at a time
                    List<Song> batch = new ArrayList<>();
                    int processed = 0;

                    runOnUiThread(() -> {
                        progressBar.setMax(total);
                        textStatus.setText("Importing from MediaStore...");
                        textCount.setText("0 / " + total);
                    });

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                        String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                        String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                        String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                        long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));

                        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                        Song song = new Song(title, artist, contentUri.toString(), (int) duration);
                        batch.add(song);

                        // Process batch when it reaches BATCH_SIZE or on last item
                        if (batch.size() >= BATCH_SIZE || !cursor.isAfterLast() && cursor.isLast()) {
                            int[] result = dbHelper.addSongsIfNotExistBatch(batch);
                            newCount += result[0];
                            duplicates[0] += result[1];
                            processed += batch.size();

                            // Update UI
                            int finalProcessed = processed;
                            String finalTitle = title;
                            runOnUiThread(() -> {
                                textStatus.setText("Importing: " + finalTitle);
                                textCount.setText(finalProcessed + " / " + total);
                                progressBar.setProgress(finalProcessed);
                            });

                            batch.clear();
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
                            textCount.setText(finalProcessed + " / " + total);
                            progressBar.setProgress(finalProcessed);
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error using MediaStore. Falling back to folder scan.", Toast.LENGTH_SHORT)
                            .show();
                });
                dialog.dismiss();
                importMusicFromFolder(folderUri);
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
                    String message = "Imported " + finalNewCount + " new song(s)";
                    if (finalDuplicates > 0) {
                        message += ". " + finalDuplicates + " duplicate(s) skipped.";
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    /**
     * Manual SAF-based recursive scanner — used only if MediaStore fails
     */
    private void importMusicFromFolder(Uri treeUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Scanning Folder");
        builder.setCancelable(false);

        final View progressView = getLayoutInflater().inflate(R.layout.dialog_import_progress, null);
        builder.setView(progressView);

        AlertDialog dialog = builder.create();
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
                        Song song = new Song(displayName, "Unknown Artist", fileUri.toString(), 0);

                        // Only add if not duplicate
                        if (dbHelper.addSongIfNotExists(song)) {
                            importedSongs.add(song);
                        } else {
                            duplicates[0]++;
                        }

                        if (callback != null)
                            callback.onSongDetected(displayName);
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
}