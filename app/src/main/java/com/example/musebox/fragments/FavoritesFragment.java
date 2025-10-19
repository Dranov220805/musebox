package com.example.musebox.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.adapters.SongAdapter;
import com.example.musebox.database.SongDatabaseHelper;
import com.example.musebox.models.Song;
import com.example.musebox.services.MusicService;
import com.example.musebox.utils.SongActionUtils;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private TextView tvEmptyMessage;
    private TextView tvFavoritesCount;
    private ImageButton btnBack;
    private SongAdapter adapter;
    private SongDatabaseHelper dbHelper;
    private List<Song> favoriteSongs = new ArrayList<>();

    // Interface for communicating with parent activity
    public interface OnFavoritesFragmentListener {
        void onBackPressed();

        MusicService getMusicService();

        boolean isMusicServiceBound();

        void onSongSelected(Song song, List<Song> displayedSongs);

        void onFavoriteSongSelected(Song song, List<Song> favoritesPlaylist);

        void addSongToQueue(Song song);
    }

    private OnFavoritesFragmentListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFavoritesFragmentListener) {
            listener = (OnFavoritesFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFavoritesFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_favorites, container, false);

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerFavorites);
        emptyView = view.findViewById(R.id.emptyView);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        tvFavoritesCount = view.findViewById(R.id.tvFavoritesCount);
        btnBack = view.findViewById(R.id.btnBack); // May be null in portrait mode

        // Initialize database helper
        dbHelper = new SongDatabaseHelper(getContext());

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SongAdapter();
        adapter.setDatabaseHelper(dbHelper); // Set database helper for favorite checking
        adapter.setOnSongClickListener(this::onSongClicked);

        // Set the menu listener for song options (same as HomeFragment)
        adapter.setOnSongMenuListener(new SongAdapter.OnSongMenuListener() {
            @Override
            public void onAddToQueue(Song song) {
                if (listener != null && listener.isMusicServiceBound()) {
                    SongActionUtils.addToQueue(requireContext(), song, listener.getMusicService());
                } else {
                    Toast.makeText(requireContext(), "Music service not available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAddToFavourite(Song song, int position) {
                // Toggle favorite status (will remove from favorites since we're in favorites)
                SongActionUtils.toggleFavorite(requireContext(), song, dbHelper,
                        (s, isFavorite) -> {
                            if (!isFavorite) {
                                // Song was removed from favorites, update the list
                                loadFavorites();
                            } else {
                                // Just refresh the item to update heart icon
                                adapter.notifyItemChanged(position);
                            }
                        });
            }

            @Override
            public void onAddToPlaylist(Song song) {
                // Show dialog to select playlist
                com.example.musebox.utils.PlaylistDialogHelper.showAddToPlaylistDialog(requireContext(), song, null);
            }

            @Override
            public void onDeleteSong(Song song, int position) {
                SongActionUtils.showDeleteConfirmationDialog(requireContext(), song, position,
                        new SongActionUtils.OnSongDeleteListener() {
                            @Override
                            public void onDeleteFromDevice(Song song, int position) {
                                SongActionUtils.deleteSongFromDevice(requireContext(), song, dbHelper,
                                        (deletedSong, success) -> loadFavorites());
                            }

                            @Override
                            public void onDeleteFromLibrary(Song song, int position) {
                                SongActionUtils.deleteSongFromLibrary(requireContext(), song, dbHelper,
                                        (deletedSong, success) -> loadFavorites());
                            }
                        });
            }
        });

        recyclerView.setAdapter(adapter);

        // Back button
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBackPressed();
                }
            });
        }

        // Load favorites
        loadFavorites();

        return view;
    }

    private void onSongClicked(Song song) {
        if (listener != null) {
            listener.onFavoriteSongSelected(song, favoriteSongs);
        }
    }

    private void loadFavorites() {
        favoriteSongs = dbHelper.getFavoriteSongs();
        if (adapter != null) {
            adapter.setSongs(favoriteSongs);
        }

        if (favoriteSongs.isEmpty()) {
            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }
            if (emptyView != null) {
                emptyView.setVisibility(View.VISIBLE);
            }
        } else {
            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
            }
            if (emptyView != null) {
                emptyView.setVisibility(View.GONE);
            }
        }

        // Update count with proper formatting
        updateFavoritesCount();
    }

    private void updateFavoritesCount() {
        if (tvFavoritesCount != null) {
            int count = favoriteSongs.size();
            String countText = count + (count == 1 ? " song" : " songs");
            tvFavoritesCount.setText(countText);
        }
    }

    // Public method to refresh favorites from parent activity
    public void refreshFavorites() {
        loadFavorites();
    }

    // Public method to add/remove favorite using utility
    public void toggleFavorite(Song song) {
        SongActionUtils.toggleFavorite(requireContext(), song, dbHelper,
                (s, isFavorite) -> {
                    if (isFavorite) {
                        // Add to current list
                        favoriteSongs.add(song);
                        if (adapter != null) {
                            adapter.addSongs(List.of(song));
                        }
                    } else {
                        // Remove from current list if it exists
                        for (int i = 0; i < favoriteSongs.size(); i++) {
                            if (favoriteSongs.get(i).getId() == song.getId()) {
                                favoriteSongs.remove(i);
                                if (adapter != null) {
                                    adapter.removeSong(i);
                                }
                                break;
                            }
                        }
                    }

                    // Update UI
                    if (favoriteSongs.isEmpty()) {
                        if (recyclerView != null) {
                            recyclerView.setVisibility(View.GONE);
                        }
                        if (emptyView != null) {
                            emptyView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (recyclerView != null) {
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                        if (emptyView != null) {
                            emptyView.setVisibility(View.GONE);
                        }
                    }

                    // Update count
                    updateFavoritesCount();
                });
    }
}