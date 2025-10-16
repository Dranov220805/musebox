package com.example.musebox.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
import com.example.musebox.activities.HomeActivity;
import com.example.musebox.adapters.SongAdapter;
import com.example.musebox.database.SongDatabaseHelper;
import com.example.musebox.models.Song;
import com.example.musebox.utils.PlaylistDialogHelper;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.util.List;

public class HomeFragment extends Fragment {

    public interface OnSongSelectedListener {
        void onSongSelected(Song song, List<Song> displayedSongs);

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

        // Set the click listener for song items
        adapter.setOnSongClickListener(song -> {
            if (listener != null) {
                // Pass the currently displayed songs list along with the selected song
                listener.onSongSelected(song, adapter.getAllSongs());
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
                // Show dialog to select playlist
                PlaylistDialogHelper.showAddToPlaylistDialog(requireContext(), song, null);
            }

            @Override
            public void onDeleteSong(Song song, int position) {
                showDeleteConfirmationDialog(song, position);
            }
        });

        // Set the file deleted listener to handle songs whose files have been deleted
        adapter.setOnSongFileDeletedListener(new SongAdapter.OnSongFileDeletedListener() {
            @Override
            public void onSongFileDeleted(Song song, int position) {
                // Show a dialog asking if user wants to remove this song from the library
                new AlertDialog.Builder(requireContext())
                        .setTitle("Song File Not Found")
                        .setMessage("The file for \"" + song.getTitle() + "\" was not found on your device. " +
                                "It may have been moved or deleted. Do you want to remove it from your library?")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            // Remove from database
                            dbHelper.deleteSong(song.getId());

                            // Remove from adapter
                            adapter.removeSong(position);

                            // Update song count display
                            updateSongCountDisplay();

                            Toast.makeText(requireContext(), "Removed from library", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Keep", (dialog, which) -> {
                            // Do nothing - keep the song in the library even though file is missing
                            dialog.dismiss();
                        })
                        .setNeutralButton("Refresh Library", (dialog, which) -> {
                            // Trigger a full rescan of the music library
                            checkPermissionAndScan();
                        })
                        .show();
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

    /**
     * Scan device for audio files - delegates to HomeActivity's unified import
     * method
     */
    private void scanDeviceForAudioFiles() {
        // Delegate to HomeActivity's importMusicFromMediaStore which now scans all
        // volumes
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).onImportMusicSelected(null);
        } else {
            Toast.makeText(requireContext(), "Error: Unable to access import functionality", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSongsFromDatabase() {
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

            // Show loading indicator
            if (progressBarLoading != null) {
                progressBarLoading.setVisibility(View.VISIBLE);
            }

            // Load all songs at once
            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                List<Song> songs = dbHelper.getAllSongs();
                long loadTime = System.currentTimeMillis() - startTime;

                android.util.Log.d("HomeFragment",
                        "Loaded " + songs.size() + " songs in " + loadTime + "ms");

                requireActivity().runOnUiThread(() -> {
                    adapter.setSongs(songs);

                    // Hide loading indicator
                    if (progressBarLoading != null) {
                        progressBarLoading.setVisibility(View.GONE);
                    }
                });
            }).start();
        }

        // Load favorites
        loadFavorites();
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

    /**
     * Update the song count display based on current database state
     */
    private void updateSongCountDisplay() {
        int totalCount = dbHelper.getSongCount();
        if (totalCount == 0) {
            scrollContent.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            tvSongCount.setText("0 songs");
        } else {
            scrollContent.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            tvSongCount.setText(totalCount + (totalCount == 1 ? " song" : " songs"));
        }
    }

    public void refreshSongs() {
        // Reload from database
        loadSongsFromDatabase();
        loadFavorites(); // Also reload favorites
    }
}