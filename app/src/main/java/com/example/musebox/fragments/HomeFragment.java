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
import com.example.musebox.utils.SongActionUtils;
import com.example.musebox.utils.ThemedDialogUtils;
import com.google.android.material.card.MaterialCardView;

import android.app.AlertDialog;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import com.example.musebox.adapters.ArtistFilterAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    private Button btnSort;
    private Button btnFilter;
    private TextView tvSongCount;
    private TextView tvFavoritesCount;
    private android.widget.ProgressBar progressBarLoading;
    private SongAdapter adapter;
    private SongDatabaseHelper dbHelper;
    private OnSongSelectedListener listener;

    private static final int REQUEST_PERMISSION = 200;

    // Sort and filter state
    private String currentSortOrder = "title";
    private String currentFilter = "all";
    private String currentArtistFilter = null;

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
        btnSort = view.findViewById(R.id.btnSort);
        btnFilter = view.findViewById(R.id.btnFilter);
        tvSongCount = view.findViewById(R.id.tvSongCount);
        tvFavoritesCount = view.findViewById(R.id.tvFavoritesCount);
        progressBarLoading = view.findViewById(R.id.progressBarLoading);

        dbHelper = new SongDatabaseHelper(requireContext());
        // Ensure index exists for faster queries
        dbHelper.ensureIndexExists();

        // Setup sort and filter buttons
        setupSortButton();
        setupFilterButton();

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
        adapter.setDatabaseHelper(dbHelper); // Set database helper for favorite checking
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
                    HomeActivity activity = (HomeActivity) getActivity();
                    if (activity.isMusicServiceBound()) {
                        SongActionUtils.addToQueue(requireContext(), song, activity.getMusicService());
                    } else {
                        Toast.makeText(requireContext(), "Music service not available", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onAddToFavourite(Song song, int position) {
                SongActionUtils.toggleFavorite(requireContext(), song, dbHelper,
                        (s, isFavorite) -> {
                            loadFavorites();
                            // Refresh only the specific item to update the heart icon
                            adapter.notifyItemChanged(position);
                        });
            }

            @Override
            public void onAddToPlaylist(Song song) {
                // Show dialog to select playlist
                PlaylistDialogHelper.showAddToPlaylistDialog(requireContext(), song, null);
            }

            @Override
            public void onDeleteSong(Song song, int position) {
                SongActionUtils.showDeleteConfirmationDialog(requireContext(), song, position,
                        new SongActionUtils.OnSongDeleteListener() {
                            @Override
                            public void onDeleteFromDevice(Song song, int position) {
                                SongActionUtils.deleteSongFromDevice(requireContext(), song, dbHelper,
                                        (deletedSong, success) -> loadSongsFromDatabase());
                            }

                            @Override
                            public void onDeleteFromLibrary(Song song, int position) {
                                SongActionUtils.deleteSongFromLibrary(requireContext(), song, dbHelper,
                                        (deletedSong, success) -> loadSongsFromDatabase());
                            }
                        });
            }
        });

        // Set the file deleted listener to handle songs whose files have been deleted
        adapter.setOnSongFileDeletedListener(new SongAdapter.OnSongFileDeletedListener() {
            @Override
            public void onSongFileDeleted(Song song, int position) {
                // Show a dialog asking if user wants to remove this song from the library
                String message = "The file for \"" + song.getTitle() + "\" was not found on your device. " +
                        "It may have been moved or deleted. What would you like to do?";

                String[] options = {
                        "Remove from Library",
                        "Keep in Library",
                        "Refresh Library"
                };

                ThemedDialogUtils.showListDialog(
                        requireContext(),
                        "Song File Not Found",
                        options,
                        -1, // No pre-selected item
                        R.drawable.ic_warning,
                        R.color.warning,
                        (selectedIndex) -> {
                            switch (selectedIndex) {
                                case 0: // Remove from Library
                                    // Remove from database
                                    dbHelper.deleteSong(song.getId());

                                    // Remove from adapter
                                    adapter.removeSong(position);

                                    // Update song count display
                                    updateSongCountDisplay();

                                    Toast.makeText(requireContext(), "Removed from library", Toast.LENGTH_SHORT).show();
                                    break;

                                case 1: // Keep in Library
                                    // Do nothing - keep the song in the library even though file is missing
                                    break;

                                case 2: // Refresh Library
                                    // Trigger a full rescan of the music library
                                    checkPermissionAndScan();
                                    break;
                            }
                        });
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

    private void setupSortButton() {
        btnSort.setOnClickListener(v -> showSortDialog());
    }

    private void showSortDialog() {
        // Inflate custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sort_options, null);

        RadioGroup radioGroupSort = dialogView.findViewById(R.id.radioGroupSort);
        RadioButton radioTitle = dialogView.findViewById(R.id.radioTitle);
        RadioButton radioArtist = dialogView.findViewById(R.id.radioArtist);
        RadioButton radioDuration = dialogView.findViewById(R.id.radioDuration);
        RadioButton radioRecent = dialogView.findViewById(R.id.radioRecent);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnApply = dialogView.findViewById(R.id.btnApply);

        // Set current selection
        switch (currentSortOrder) {
            case "title":
                radioTitle.setChecked(true);
                break;
            case "artist":
                radioArtist.setChecked(true);
                break;
            case "duration":
                radioDuration.setChecked(true);
                break;
            case "recent":
                radioRecent.setChecked(true);
                break;
        }

        // Create dialog with custom view
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // Set transparent background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // Handle cancel button
        btnCancel.setOnClickListener(view -> dialog.dismiss());

        // Handle apply button
        btnApply.setOnClickListener(view -> {
            int selectedId = radioGroupSort.getCheckedRadioButtonId();
            String newSortOrder = currentSortOrder;
            String sortLabel = "";

            if (selectedId == R.id.radioTitle) {
                newSortOrder = "title";
                sortLabel = "Title";
            } else if (selectedId == R.id.radioArtist) {
                newSortOrder = "artist";
                sortLabel = "Artist";
            } else if (selectedId == R.id.radioDuration) {
                newSortOrder = "duration";
                sortLabel = "Duration";
            } else if (selectedId == R.id.radioRecent) {
                newSortOrder = "recent";
                sortLabel = "Recent";
            }

            currentSortOrder = newSortOrder;
            btnSort.setText("Sort: " + sortLabel);
            loadSongsFromDatabase();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupFilterButton() {
        btnFilter.setOnClickListener(v -> showFilterDialog());
    }

    private void showFilterDialog() {
        // Inflate custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_options, null);

        Button btnFilterAll = dialogView.findViewById(R.id.btnFilterAll);
        Button btnFilterFavorites = dialogView.findViewById(R.id.btnFilterFavorites);
        RecyclerView rvArtists = dialogView.findViewById(R.id.rvArtists);
        TextView tvArtistsLabel = dialogView.findViewById(R.id.tvArtistsLabel);
        Button btnClose = dialogView.findViewById(R.id.btnClose);

        // Create dialog with custom view
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // Set transparent background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // Highlight current filter
        updateFilterButtonStyle(btnFilterAll, "all".equals(currentFilter));
        updateFilterButtonStyle(btnFilterFavorites, "favorites".equals(currentFilter));

        // Handle "All Songs" button
        btnFilterAll.setOnClickListener(view -> {
            currentFilter = "all";
            currentArtistFilter = null;
            btnFilter.setText("Filter: All");
            loadSongsFromDatabase();
            dialog.dismiss();
        });

        // Handle "Favorites" button
        btnFilterFavorites.setOnClickListener(view -> {
            currentFilter = "favorites";
            currentArtistFilter = null;
            btnFilter.setText("Filter: Favorites");
            loadSongsFromDatabase();
            dialog.dismiss();
        });

        // Setup artist list in background thread
        new Thread(() -> {
            List<String> artists = dbHelper.getAllArtists();

            requireActivity().runOnUiThread(() -> {
                if (artists.isEmpty()) {
                    tvArtistsLabel.setVisibility(View.GONE);
                    rvArtists.setVisibility(View.GONE);
                } else {
                    tvArtistsLabel.setVisibility(View.VISIBLE);
                    rvArtists.setVisibility(View.VISIBLE);

                    ArtistFilterAdapter artistAdapter = new ArtistFilterAdapter(artists);
                    rvArtists.setLayoutManager(new LinearLayoutManager(requireContext()));
                    rvArtists.setAdapter(artistAdapter);

                    artistAdapter.setOnArtistClickListener(artist -> {
                        currentFilter = "artist";
                        currentArtistFilter = artist;
                        btnFilter.setText("Filter: " + artist);
                        loadSongsFromDatabase();
                        dialog.dismiss();
                    });
                }
            });
        }).start();

        // Handle close button
        btnClose.setOnClickListener(view -> dialog.dismiss());

        dialog.show();
    }

    private void updateFilterButtonStyle(Button button, boolean isSelected) {
        if (isSelected) {
            button.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.spotify_green, null)));
        } else {
            button.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.spotify_black, null)));
        }
    }

    private void sortSongsList(List<Song> songs) {
        switch (currentSortOrder) {
            case "title":
                Collections.sort(songs, (s1, s2) -> s1.getTitle().compareToIgnoreCase(s2.getTitle()));
                break;
            case "artist":
                Collections.sort(songs, (s1, s2) -> s1.getArtist().compareToIgnoreCase(s2.getArtist()));
                break;
            case "duration":
                Collections.sort(songs, (s1, s2) -> Long.compare(s2.getDuration(), s1.getDuration()));
                break;
            case "recent":
                Collections.sort(songs, (s1, s2) -> s2.getId().compareTo(s1.getId()));
                break;
        }
    }

    private void loadSongsFromDatabase() {
        // Update total song count
        int totalCount = dbHelper.getSongCount();
        tvSongCount.setText(totalCount + (totalCount == 1 ? " song" : " songs"));

        if (totalCount == 0) {
            recyclerSongs.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerSongs.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);

            // Load songs in background thread
            new Thread(() -> {
                List<Song> songs;

                // Apply filter first
                if ("favorites".equals(currentFilter)) {
                    // Get favorites
                    songs = dbHelper.getFavoriteSongs();
                    // Sort manually (favorites query doesn't support sorting)
                    sortSongsList(songs);

                } else if ("artist".equals(currentFilter) && currentArtistFilter != null) {
                    // Get songs by artist
                    songs = dbHelper.getSongsByArtist(currentArtistFilter, currentSortOrder);

                } else {
                    // Get all songs with sorting
                    songs = dbHelper.getAllSongsSorted(currentSortOrder);
                }

                requireActivity().runOnUiThread(() -> {
                    adapter.setSongs(songs);
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
                // Display just the number
                tvFavoritesCount.setText(String.valueOf(favCount));
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