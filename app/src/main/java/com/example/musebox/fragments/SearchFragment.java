package com.example.musebox.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.activities.FullPlayerActivity;
import com.example.musebox.activities.HomeActivity;
import com.example.musebox.adapters.ArtistFilterAdapter;
import com.example.musebox.adapters.SongAdapter;
import com.example.musebox.database.SongDatabaseHelper;
import com.example.musebox.models.Song;
import com.example.musebox.utils.PlaylistDialogHelper;
import com.example.musebox.utils.SongActionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * SearchFragment - Provides search, sort, and filter functionality for the
 * music library
 * Features:
 * - Real-time search by title and artist
 * - Sort by: Title, Artist, Duration, Recently Added
 * - Filter by: All Songs, Favorites, Specific Artist
 */
public class SearchFragment extends Fragment {

    private EditText etSearch;
    private ImageButton btnClearSearch;
    private Button btnSort;
    private Button btnFilter;
    private TextView tvResultsCount;
    private RecyclerView rvSearchResults;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyMessage;

    private SongAdapter adapter;
    private SongDatabaseHelper dbHelper;

    private String currentSortOrder = "title"; // title, artist, duration, recent
    private String currentFilter = "all"; // all, favorites, artist
    private String currentArtistFilter = null;
    private String currentSearchQuery = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new SongDatabaseHelper(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupRecyclerView();
        setupSearchBar();
        setupSortButton();
        setupFilterButton();

        // Load all songs initially
        loadSongs();
    }

    private void initializeViews(View view) {
        etSearch = view.findViewById(R.id.etSearch);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        btnSort = view.findViewById(R.id.btnSort);
        btnFilter = view.findViewById(R.id.btnFilter);
        tvResultsCount = view.findViewById(R.id.tvResultsCount);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
    }

    private void setupRecyclerView() {
        adapter = new SongAdapter();
        adapter.setDatabaseHelper(dbHelper); // Set database helper for favorite checking
        rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSearchResults.setAdapter(adapter);

        // Handle song click - play song immediately while keeping current queue
        adapter.setOnSongClickListener(song -> {
            if (getActivity() instanceof HomeActivity) {
                HomeActivity homeActivity = (HomeActivity) getActivity();

                // Play the song now while keeping the current queue
                homeActivity.playNowKeepQueue(song);

                // Open FullPlayerActivity to show the player
                Intent intent = new Intent(requireContext(), FullPlayerActivity.class);
                startActivity(intent);
            }
        });

        // Handle menu actions
        adapter.setOnSongMenuListener(new SongAdapter.OnSongMenuListener() {
            @Override
            public void onAddToQueue(Song song) {
                if (getActivity() instanceof HomeActivity) {
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
                            // Update only the specific item to refresh heart icon
                            adapter.notifyItemChanged(position);

                            // Reload if we're filtering by favorites
                            if ("favorites".equals(currentFilter)) {
                                loadSongs();
                            }
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
                                        (deletedSong, success) -> loadSongs());
                            }

                            @Override
                            public void onDeleteFromLibrary(Song song, int position) {
                                SongActionUtils.deleteSongFromLibrary(requireContext(), song, dbHelper,
                                        (deletedSong, success) -> loadSongs());
                            }
                        });
            }
        });

        // Handle file deleted callback
        adapter.setOnSongFileDeletedListener((song, position) -> {
            Toast.makeText(requireContext(),
                    "File not found: " + song.getTitle(),
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void setupSearchBar() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                loadSongs();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            currentSearchQuery = "";
        });
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
            loadSongs();
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
            loadSongs();
            dialog.dismiss();
        });

        // Handle "Favorites" button
        btnFilterFavorites.setOnClickListener(view -> {
            currentFilter = "favorites";
            currentArtistFilter = null;
            btnFilter.setText("Filter: Favorites");
            loadSongs();
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
                        loadSongs();
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

    private void loadSongs() {
        new Thread(() -> {
            List<Song> songs;

            // Apply filter first
            if ("favorites".equals(currentFilter)) {
                // Get favorites
                songs = dbHelper.getFavoriteSongs();

                // Apply search if query exists
                if (!currentSearchQuery.isEmpty()) {
                    List<Song> filteredSongs = new ArrayList<>();
                    String query = currentSearchQuery.toLowerCase();
                    for (Song song : songs) {
                        if (song.getTitle().toLowerCase().contains(query) ||
                                song.getArtist().toLowerCase().contains(query)) {
                            filteredSongs.add(song);
                        }
                    }
                    songs = filteredSongs;
                }

                // Sort manually (favorites query doesn't support sorting)
                sortSongsList(songs);

            } else if ("artist".equals(currentFilter) && currentArtistFilter != null) {
                // Get songs by artist
                if (currentSearchQuery.isEmpty()) {
                    songs = dbHelper.getSongsByArtist(currentArtistFilter, currentSortOrder);
                } else {
                    // Search within artist's songs
                    songs = dbHelper.getSongsByArtist(currentArtistFilter, currentSortOrder);
                    List<Song> filteredSongs = new ArrayList<>();
                    String query = currentSearchQuery.toLowerCase();
                    for (Song song : songs) {
                        if (song.getTitle().toLowerCase().contains(query)) {
                            filteredSongs.add(song);
                        }
                    }
                    songs = filteredSongs;
                }

            } else {
                // All songs
                if (currentSearchQuery.isEmpty()) {
                    songs = dbHelper.getAllSongsSorted(currentSortOrder);
                } else {
                    songs = dbHelper.searchSongs(currentSearchQuery, currentSortOrder);
                }
            }

            final List<Song> finalSongs = songs;
            requireActivity().runOnUiThread(() -> {
                adapter.submitList(finalSongs);
                updateResultsCount(finalSongs.size());
                updateEmptyState(finalSongs.isEmpty());
            });
        }).start();
    }

    private void sortSongsList(List<Song> songs) {
        switch (currentSortOrder) {
            case "artist":
                songs.sort((s1, s2) -> {
                    int artistCompare = s1.getArtist().compareToIgnoreCase(s2.getArtist());
                    if (artistCompare != 0)
                        return artistCompare;
                    return s1.getTitle().compareToIgnoreCase(s2.getTitle());
                });
                break;
            case "duration":
                songs.sort((s1, s2) -> Long.compare(s2.getDuration(), s1.getDuration()));
                break;
            case "recent":
                // Already in order from database (most recent first)
                break;
            case "title":
            default:
                songs.sort((s1, s2) -> s1.getTitle().compareToIgnoreCase(s2.getTitle()));
                break;
        }
    }

    private void updateResultsCount(int count) {
        String text = count + (count == 1 ? " song" : " songs");
        tvResultsCount.setText(text);
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            rvSearchResults.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);

            if (!currentSearchQuery.isEmpty()) {
                tvEmptyMessage.setText("No results for \"" + currentSearchQuery + "\"");
            } else if ("favorites".equals(currentFilter)) {
                tvEmptyMessage.setText("No favorite songs yet");
            } else if ("artist".equals(currentFilter)) {
                tvEmptyMessage.setText("No songs by " + currentArtistFilter);
            } else {
                tvEmptyMessage.setText("No songs in library");
            }
        } else {
            rvSearchResults.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
