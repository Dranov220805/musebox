package com.example.musebox.fragments;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.adapters.SongAdapter;
import com.example.musebox.database.SongDatabaseHelper;
import com.example.musebox.models.Song;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    public interface OnSongSelectedListener {
        void onSongSelected(Song song);

        void onFavoritesClicked();
    }

    private RecyclerView recyclerSongs;
    private MaterialCardView favoritesCard;
    private LinearLayout emptyView;
    private View scrollContent;
    private Button btnImport;
    private TextView tvSongCount;
    private TextView tvFavoritesCount;
    private android.widget.ProgressBar progressBarLoading;
    private SongAdapter adapter;
    private SongDatabaseHelper dbHelper;
    private OnSongSelectedListener listener;

    private static final int REQUEST_PERMISSION = 200;
    private static final int PAGE_SIZE = 30; // Load 30 songs at a time for faster response
    private int currentOffset = 0;
    private boolean isLoading = false;
    private boolean hasMoreData = true;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnSongSelectedListener) {
            listener = (OnSongSelectedListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerSongs = view.findViewById(R.id.recyclerSongs);
        favoritesCard = view.findViewById(R.id.favoritesCard);
        emptyView = view.findViewById(R.id.emptyView);
        scrollContent = view.findViewById(R.id.scrollContent);
        btnImport = view.findViewById(R.id.btnImport);
        tvSongCount = view.findViewById(R.id.tvSongCount);
        tvFavoritesCount = view.findViewById(R.id.tvFavoritesCount);
        progressBarLoading = view.findViewById(R.id.progressBarLoading);

        dbHelper = new SongDatabaseHelper(requireContext());
        // Ensure index exists for faster queries
        dbHelper.ensureIndexExists();

        // Setup favorites card click listener
        favoritesCard.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoritesClicked();
            }
        });

        // Setup songs RecyclerView (vertical)
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerSongs.setLayoutManager(layoutManager);
        adapter = new SongAdapter(); // Using optimized adapter with DiffUtil
        recyclerSongs.setAdapter(adapter);

        // Add scroll listener to NestedScrollView for pagination
        scrollContent.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (v instanceof androidx.core.widget.NestedScrollView) {
                    androidx.core.widget.NestedScrollView scrollView = (androidx.core.widget.NestedScrollView) v;
                    View child = scrollView.getChildAt(0);
                    if (child != null) {
                        int diff = (child.getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));

                        android.util.Log.d("HomeFragment", "Scroll: diff=" + diff +
                                ", isLoading=" + isLoading + ", hasMore=" + hasMoreData);

                        // Load more when within 500 pixels from bottom
                        if (diff <= 500 && !isLoading && hasMoreData && scrollY > oldScrollY) {
                            android.util.Log.d("HomeFragment", "Triggering loadMoreSongs()");
                            loadMoreSongs();
                        }
                    }
                }
            }
        });

        // Set the click listener for song items
        adapter.setOnSongClickListener(song -> {
            if (listener != null) {
                listener.onSongSelected(song);
            }
        });

        // Set the menu listener for song options
        adapter.setOnSongMenuListener(new SongAdapter.OnSongMenuListener() {
            @Override
            public void onAddToQueue(Song song) {
                if (getActivity() instanceof com.example.musebox.activities.HomeActivity) {
                    ((com.example.musebox.activities.HomeActivity) getActivity()).addSongToQueue(song);
                }
            }

            @Override
            public void onAddToFavourite(Song song) {
                toggleFavorite(song);
            }

            @Override
            public void onAddToPlaylist(Song song) {
                Toast.makeText(requireContext(), "Add " + song.getTitle() + " to playlist", Toast.LENGTH_SHORT).show();
                // TODO: Show playlist selection dialog
            }

            @Override
            public void onDeleteSong(Song song, int position) {
                showDeleteConfirmationDialog(song, position);
            }
        });

        btnImport.setOnClickListener(v -> checkPermissionAndScan());

        loadSongsFromDatabase();
        return view;
    }

    public void checkPermissionAndScan() {
        // Check Android version and request appropriate permission
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            permission = Manifest.permission.READ_MEDIA_AUDIO;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { permission }, REQUEST_PERMISSION);
        } else {
            scanDeviceForAudioFiles();
        }
    }

    private android.app.AlertDialog showImportProgressDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Importing Music");
        builder.setCancelable(false);

        final View progressView = getLayoutInflater().inflate(R.layout.dialog_import_progress, null);
        builder.setView(progressView);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        return dialog;
    }

    private void scanDeviceForAudioFiles() {
        // Show progress dialog
        android.app.AlertDialog dialog = showImportProgressDialog();
        android.widget.ProgressBar progressBar = dialog.findViewById(R.id.progressBar);
        android.widget.TextView textStatus = dialog.findViewById(R.id.textStatus);
        android.widget.TextView textCount = dialog.findViewById(R.id.textCount);

        new Thread(() -> {
            int newCount = 0;
            int[] duplicates = { 0 };

            try {
                ContentResolver resolver = requireContext().getContentResolver();
                
                // Get all available volumes (internal storage, SDCard, etc.)
                java.util.Set<String> volumes = new java.util.HashSet<>();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ - Use getExternalVolumeNames to get all volumes
                    volumes.addAll(MediaStore.getExternalVolumeNames(requireContext()));
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
                requireActivity().runOnUiThread(() -> {
                    textStatus.setText("Scanning all storage volumes...");
                    textCount.setText("Counting songs...");
                });

                for (String volume : volumes) {
                    Uri uri = MediaStore.Audio.Media.getContentUri(volume);
                    android.util.Log.d("HomeFragmentScan", "Scanning volume: " + volume + " at URI: " + uri);
                    
                    try (Cursor cursor = resolver.query(uri, projection, null, null, null)) {
                        if (cursor != null) {
                            totalSongs += cursor.getCount();
                            android.util.Log.d("HomeFragmentScan", "Volume " + volume + " has " + cursor.getCount() + " songs");
                        }
                    } catch (Exception e) {
                        android.util.Log.w("HomeFragmentScan", "Failed to scan volume " + volume + ": " + e.getMessage());
                    }
                }

                if (totalSongs == 0) {
                    requireActivity().runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(requireContext(), "No music found on any storage volume", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                final int finalTotalSongs = totalSongs;
                requireActivity().runOnUiThread(() -> {
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
                    android.util.Log.d("HomeFragmentScan", "Processing volume: " + volume);
                    
                    requireActivity().runOnUiThread(() -> {
                        textStatus.setText("Importing from " + volume + " storage...");
                    });

                    try (Cursor cursor = resolver.query(uri, projection, null, null, null)) {
                        if (cursor == null) continue;

                        while (cursor.moveToNext()) {
                            long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                            String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                            String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                            String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                            long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));

                            // Skip non-existent files
                            if (path == null) continue;

                            Uri contentUri = ContentUris.withAppendedId(uri, id);

                            android.util.Log.d("HomeFragmentScan", "Processing: " + title + " by " + artist + " from " + volume);

                            // Extract and save embedded album art if present
                            String albumArtPath = null;
                            try {
                                android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                                
                                // Try content URI first, fallback to file path for SDCard compatibility
                                try {
                                    retriever.setDataSource(requireContext(), contentUri);
                                } catch (Exception e) {
                                    android.util.Log.d("HomeFragmentScan", "Content URI failed for " + title + ", trying file path");
                                    retriever.setDataSource(path);
                                }
                                
                                byte[] art = retriever.getEmbeddedPicture();
                                if (art != null && getActivity() instanceof com.example.musebox.activities.HomeActivity) {
                                    android.util.Log.d("HomeFragmentScan", "✓ FOUND embedded album art in: " + title + " (size: " + art.length + " bytes)");

                                    // Save embedded art to internal storage
                                    com.example.musebox.activities.HomeActivity homeActivity = 
                                        (com.example.musebox.activities.HomeActivity) getActivity();
                                    albumArtPath = homeActivity.saveEmbeddedAlbumArt(art, title, artist);
                                    if (albumArtPath != null) {
                                        android.util.Log.d("HomeFragmentScan", "✓ SAVED album art to: " + albumArtPath);
                                    }
                                } else {
                                    android.util.Log.d("HomeFragmentScan", "✗ NO embedded album art in: " + title);
                                }
                                retriever.release();
                            } catch (Exception e) {
                                android.util.Log.d("HomeFragmentScan", "Error extracting album art for " + title + ": " + e.getMessage());
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
                                requireActivity().runOnUiThread(() -> {
                                    textStatus.setText("Importing: " + finalTitle + " (" + finalVolume + ")");
                                    textCount.setText(finalProcessed + " / " + finalTotalSongs);
                                    progressBar.setProgress(finalProcessed);
                                });

                                batch.clear();
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.w("HomeFragmentScan", "Error scanning volume " + volume + ": " + e.getMessage());
                    }
                }

                // Process any remaining songs
                if (!batch.isEmpty()) {
                    int[] result = dbHelper.addSongsIfNotExistBatch(batch);
                    newCount += result[0];
                    duplicates[0] += result[1];
                    processed += batch.size();

                    int finalProcessed = processed;
                    requireActivity().runOnUiThread(() -> {
                        textCount.setText(finalProcessed + " / " + finalTotalSongs);
                        progressBar.setProgress(finalProcessed);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Error scanning device music: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
                return;
            }

            int finalNewCount = newCount;
            int finalDuplicates = duplicates[0];
            requireActivity().runOnUiThread(() -> {
                dialog.dismiss();
                if (finalNewCount == 0 && finalDuplicates == 0) {
                    Toast.makeText(requireContext(), "No songs found", Toast.LENGTH_LONG).show();
                } else if (finalNewCount == 0) {
                    Toast.makeText(requireContext(), "Added 0 new songs. " + finalDuplicates + " duplicate(s) skipped.",
                            Toast.LENGTH_LONG).show();
                } else {
                    String message = "Imported " + finalNewCount + " new song(s) with album art from all storage volumes";
                    if (finalDuplicates > 0) {
                        message += ". " + finalDuplicates + " duplicate(s) skipped.";
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
                loadSongsFromDatabase();
                loadFavorites(); // Also reload favorites
            });
        }).start();
    }

    private void loadSongsFromDatabase() {
        // Reset pagination
        currentOffset = 0;
        hasMoreData = true;
        adapter.clearSongs();

        // Update song count
        int totalCount = dbHelper.getSongCount();
        if (totalCount == 0) {
            scrollContent.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            tvSongCount.setText("0 songs");
        } else {
            scrollContent.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            tvSongCount.setText(totalCount + (totalCount == 1 ? " song" : " songs"));

            // Load first page
            loadMoreSongs();
        }

        // Load favorites
        loadFavorites();
    }

    private void loadMoreSongs() {
        if (isLoading || !hasMoreData)
            return;

        isLoading = true;

        // Show loading indicator
        if (progressBarLoading != null) {
            progressBarLoading.setVisibility(View.VISIBLE);
        }

        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            List<Song> songs = dbHelper.getSongsPaginated(PAGE_SIZE, currentOffset);
            long loadTime = System.currentTimeMillis() - startTime;

            android.util.Log.d("HomeFragment",
                    "Loaded " + songs.size() + " songs in " + loadTime + "ms from offset " + currentOffset);

            requireActivity().runOnUiThread(() -> {
                if (songs.isEmpty()) {
                    hasMoreData = false;
                } else {
                    adapter.addSongs(songs);
                    currentOffset += songs.size();

                    // If we got less than PAGE_SIZE, we're at the end
                    if (songs.size() < PAGE_SIZE) {
                        hasMoreData = false;
                    }
                }
                isLoading = false;

                // Hide loading indicator
                if (progressBarLoading != null) {
                    progressBarLoading.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanDeviceForAudioFiles();
            } else {
                Toast.makeText(requireContext(),
                        "Permission denied! Cannot scan for songs.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadFavorites() {
        new Thread(() -> {
            int favCount = dbHelper.getFavoritesCount();

            requireActivity().runOnUiThread(() -> {
                // Always show favorites card, even with 0 count
                favoritesCard.setVisibility(View.VISIBLE);
                String countText = favCount + " favorite" + (favCount != 1 ? "s" : "");
                tvFavoritesCount.setText(countText);
            });
        }).start();
    }

    private void toggleFavorite(Song song) {
        new Thread(() -> {
            boolean isFavorite = dbHelper.toggleFavorite(song.getId());
            song.setFavorite(isFavorite);

            requireActivity().runOnUiThread(() -> {
                String message = isFavorite ? "Added \"" + song.getTitle() + "\" to favorites"
                        : "Removed \"" + song.getTitle() + "\" from favorites";
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

                // Reload favorites section
                loadFavorites();
            });
        }).start();
    }

    private void showDeleteConfirmationDialog(Song song, int position) {
        if (getContext() == null)
            return;

        // Inflate custom dialog layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_remove_song, null);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        // Make dialog background transparent for rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Set song title
        TextView tvSongTitle = dialogView.findViewById(R.id.tvSongTitle);
        tvSongTitle.setText(song.getTitle());

        // Setup click listeners
        dialogView.findViewById(R.id.btn_remove_device).setOnClickListener(v -> {
            dialog.dismiss();
            deleteSongFromDevice(song, position);
        });

        dialogView.findViewById(R.id.btn_remove_library).setOnClickListener(v -> {
            dialog.dismiss();
            deleteSongFromLibrary(song, position);
        });

        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void deleteSongFromDevice(Song song, int position) {
        new Thread(() -> {
            boolean fileDeleted = false;
            boolean dbDeleted = false;
            String errorMessage = null;

            try {
                // First, try to delete the physical file
                File file = new File(song.getUri());
                if (file.exists()) {
                    fileDeleted = file.delete();
                    if (!fileDeleted) {
                        errorMessage = "Failed to delete file. Check permissions or file may be in use.";
                    }
                } else {
                    // File doesn't exist - maybe already deleted manually
                    fileDeleted = true;
                    errorMessage = "File not found on device";
                }

                // Always remove from database regardless of file deletion result
                dbHelper.deleteSong(song.getId());
                dbDeleted = true; // Assume success unless exception is thrown

            } catch (Exception e) {
                errorMessage = "Error: " + e.getMessage();
                // Still try to remove from database even if file deletion failed
                try {
                    dbHelper.deleteSong(song.getId());
                    dbDeleted = true;
                } catch (Exception dbEx) {
                    errorMessage += " | Database error: " + dbEx.getMessage();
                    dbDeleted = false;
                }
            }

            // Get updated count after database operation
            int totalCount = dbHelper.getSongCount();

            boolean finalFileDeleted = fileDeleted;
            boolean finalDbDeleted = dbDeleted;
            String finalErrorMessage = errorMessage;

            requireActivity().runOnUiThread(() -> {
                if (finalDbDeleted) {
                    // Reload the entire song list to ensure correct positions
                    loadSongsFromDatabase();

                    // Show appropriate message
                    String message;
                    if (finalFileDeleted && finalErrorMessage == null) {
                        message = "Successfully deleted \"" + song.getTitle() + "\" from device";
                    } else if (finalFileDeleted) {
                        message = "Deleted \"" + song.getTitle() + "\" (" + finalErrorMessage + ")";
                    } else {
                        message = "Removed from library only. " + finalErrorMessage;
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "Failed to remove from database: " + finalErrorMessage,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void deleteSongFromLibrary(Song song, int position) {
        new Thread(() -> {
            boolean success = false;
            String errorMessage = null;

            try {
                // Only remove from database, keep the file on device
                dbHelper.deleteSong(song.getId());
                success = true; // Assume success unless exception is thrown
            } catch (Exception e) {
                errorMessage = "Database error: " + e.getMessage();
                success = false;
            }

            // Get updated count after database operation
            int totalCount = dbHelper.getSongCount();

            boolean finalSuccess = success;
            String finalErrorMessage = errorMessage;

            requireActivity().runOnUiThread(() -> {
                if (finalSuccess) {
                    // Reload the entire song list to ensure correct positions
                    loadSongsFromDatabase();

                    Toast.makeText(requireContext(),
                            "Removed \"" + song.getTitle() + "\" from library (file kept on device)",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), finalErrorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    public void refreshSongs() {
        // Reset pagination state and reload from the beginning
        currentOffset = 0;
        hasMoreData = true;
        isLoading = false;

        // Reload from database
        loadSongsFromDatabase();
    }
}