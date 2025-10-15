package com.example.musebox.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.activities.FullPlayerActivity;
import com.example.musebox.adapters.SongAdapter;
import com.example.musebox.database.PlaylistDatabaseHelper;
import com.example.musebox.database.SongDatabaseHelper;
import com.example.musebox.models.Playlist;
import com.example.musebox.models.Song;
import com.example.musebox.services.MusicService;
import com.example.musebox.utils.PlaylistDialogHelper;

import java.util.List;

public class PlaylistDetailFragment extends Fragment {
    private TextView tvTitle, tvDescription;
    private RecyclerView recyclerSongs;
    private SongAdapter songAdapter;
    private PlaylistDatabaseHelper dbHelper;
    private SongDatabaseHelper songDbHelper;
    private ImageButton btnAddSong, btnBack, btnMenu;
    private String playlistId;
    private Playlist playlist;

    // Music service
    private MusicService musicService;
    private boolean serviceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            serviceBound = false;
        }
    };

    public static PlaylistDetailFragment newInstance(String playlistId) {
        PlaylistDetailFragment fragment = new PlaylistDetailFragment();
        Bundle args = new Bundle();
        args.putString("playlist_id", playlistId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist_detail, container, false);

        tvTitle = view.findViewById(R.id.tvPlaylistTitle);
        tvDescription = view.findViewById(R.id.tvPlaylistDescription);
        recyclerSongs = view.findViewById(R.id.recyclerPlaylistSongs);
        btnAddSong = view.findViewById(R.id.btnAddSong);
        btnBack = view.findViewById(R.id.btnBack);
        btnMenu = view.findViewById(R.id.btnMenu);
        
        dbHelper = new PlaylistDatabaseHelper(requireContext());
        songDbHelper = new SongDatabaseHelper(requireContext());

        // Set back button click listener
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                // Pop back to playlist list
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }

        // Bind to MusicService
        Intent serviceIntent = new Intent(requireContext(), MusicService.class);
        requireContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // Get playlist ID from arguments
        if (getArguments() != null) {
            playlistId = getArguments().getString("playlist_id");
        }

        if (playlistId == null) {
            Toast.makeText(requireContext(), "Playlist not found", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return view;
        }

        loadPlaylist();

        if (btnAddSong != null) {
            btnAddSong.setOnClickListener(v -> showAddSongDialog());
        }

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> showPlaylistOptionsMenu(v));
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (serviceBound) {
            requireContext().unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    private void showPlaylistOptionsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.inflate(R.menu.menu_playlist_options);
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

        PlaylistDialogHelper.showEditPlaylistDialog(requireContext(), playlist,
                (playlistId, newName, newDescription) -> {
                    // Reload playlist to show updated info
                    loadPlaylist();
                });
    }

    private void showDeletePlaylistDialog() {
        if (playlist == null)
            return;

        PlaylistDialogHelper.showDeletePlaylistDialog(requireContext(), playlist, deletedPlaylist -> {
            boolean success = dbHelper.deletePlaylist(deletedPlaylist.getId());
            if (success) {
                Toast.makeText(requireContext(), "Playlist deleted", Toast.LENGTH_SHORT).show();
                // Pop back to playlist list
                requireActivity().getSupportFragmentManager().popBackStack();
            } else {
                Toast.makeText(requireContext(), "Failed to delete playlist", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddSongDialog() {
        // Safety check - ensure playlist is loaded
        if (playlist == null) {
            Toast.makeText(requireContext(), "Playlist not loaded yet", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(requireContext(), "All songs are already in this playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create dialog with song selection
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_playlist, null);
        RecyclerView recyclerSongs = dialogView.findViewById(R.id.recyclerSongs);

        // Create adapter for song selection
        SongSelectAdapter selectAdapter = new SongSelectAdapter(availableSongs);
        recyclerSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerSongs.setAdapter(selectAdapter);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Add Songs to Playlist")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    List<Song> selectedSongs = selectAdapter.getSelectedSongs();
                    if (selectedSongs.isEmpty()) {
                        Toast.makeText(requireContext(), "No songs selected", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Add each selected song to the playlist
                    for (Song song : selectedSongs) {
                        dbHelper.addSongToPlaylist(playlistId, song);
                    }

                    Toast.makeText(requireContext(), "Added " + selectedSongs.size() + " song(s)",
                            Toast.LENGTH_SHORT).show();
                    loadPlaylist(); // Reload the playlist
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadPlaylist() {
        playlist = dbHelper.getPlaylistWithSongs(playlistId);
        if (playlist == null) {
            Toast.makeText(requireContext(), "Playlist not found", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }
        tvTitle.setText(playlist.getName());
        tvDescription.setText(playlist.getDescription());

        // Load songs from playlist
        List<Song> songs = playlist.getSongs();

        // Initialize adapter with songs
        songAdapter = new SongAdapter();
        songAdapter.setSongs(songs);
        recyclerSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
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

                    // Open full player
                    Intent intent = new Intent(requireContext(), FullPlayerActivity.class);
                    startActivity(intent);
                    requireActivity().overridePendingTransition(android.R.anim.slide_in_left,
                            android.R.anim.slide_out_right);

                    Toast.makeText(requireContext(), "Playing: " + song.getTitle(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "Music service not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Set menu listener for song options
        songAdapter.setOnSongMenuListener(new SongAdapter.OnSongMenuListener() {
            @Override
            public void onAddToQueue(Song song) {
                // Add to queue
                if (serviceBound && musicService != null) {
                    musicService.addToQueue(song);
                    Toast.makeText(requireContext(), "Added to queue", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Music service not available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAddToFavourite(Song song) {
                // Toggle favorite status
                boolean isFavorite = songDbHelper.isFavorite(song.getId());
                if (isFavorite) {
                    songDbHelper.removeFromFavorites(song.getId());
                    Toast.makeText(requireContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
                } else {
                    songDbHelper.addToFavorites(song.getId());
                    Toast.makeText(requireContext(), "Added to favorites", Toast.LENGTH_SHORT).show();
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
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Remove Song")
                .setMessage("Remove \"" + song.getTitle() + "\" from this playlist?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    boolean removed = dbHelper.removeSongFromPlaylist(playlistId, song.getId());
                    if (removed) {
                        Toast.makeText(requireContext(), "Song removed", Toast.LENGTH_SHORT).show();
                        loadPlaylist(); // Reload the playlist
                    } else {
                        Toast.makeText(requireContext(), "Failed to remove song", Toast.LENGTH_SHORT).show();
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

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_song_select, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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
