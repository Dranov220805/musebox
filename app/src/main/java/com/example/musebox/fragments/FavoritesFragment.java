package com.example.musebox.fragments;

import android.content.Context;
import android.net.Uri;
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
        btnBack = view.findViewById(R.id.btnBack); // May be null in portrait mode

        // Initialize database helper
        dbHelper = new SongDatabaseHelper(getContext());

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SongAdapter();
        adapter.setOnSongClickListener(this::onSongClicked);
        adapter.setMenuActionOverride((song, position) -> {
            // Override menu to show "Remove from Favorites" instead of "Delete Song"
            removeFromFavorites(song, position);
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

        // Update count if view exists
        if (tvFavoritesCount != null) {
            tvFavoritesCount.setText(String.valueOf(favoriteSongs.size()));
        }
    }

    private void removeFromFavorites(Song song, int position) {
        new Thread(() -> {
            boolean success = dbHelper.removeFromFavorites(song.getId());
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (success) {
                        // Validate position before removal to prevent crashes
                        if (position >= 0 && position < favoriteSongs.size()) {
                            favoriteSongs.remove(position);
                            adapter.removeSong(position);

                            if (favoriteSongs.isEmpty()) {
                                if (recyclerView != null) {
                                    recyclerView.setVisibility(View.GONE);
                                }
                                if (emptyView != null) {
                                    emptyView.setVisibility(View.VISIBLE);
                                }
                            }

                            // Update count if view exists
                            if (tvFavoritesCount != null) {
                                tvFavoritesCount.setText(String.valueOf(favoriteSongs.size()));
                            }

                            Toast.makeText(getContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
                        } else {
                            // If position is invalid, reload the entire list to sync
                            loadFavorites();
                            Toast.makeText(getContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    // Public method to refresh favorites from parent activity
    public void refreshFavorites() {
        loadFavorites();
    }

    // Public method to add/remove favorite
    public void toggleFavorite(Song song) {
        new Thread(() -> {
            boolean isFavorite = dbHelper.isFavorite(song.getId());
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isFavorite) {
                        dbHelper.removeFromFavorites(song.getId());
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
                    } else {
                        dbHelper.addToFavorites(song.getId());
                        // Add to current list
                        favoriteSongs.add(song);
                        if (adapter != null) {
                            adapter.addSongs(List.of(song));
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

                    // Update count if view exists
                    if (tvFavoritesCount != null) {
                        tvFavoritesCount.setText(String.valueOf(favoriteSongs.size()));
                    }
                });
            }
        }).start();
    }
}