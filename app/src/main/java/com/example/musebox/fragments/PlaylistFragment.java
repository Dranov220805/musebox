package com.example.musebox.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musebox.adapters.PlaylistAdapter;

import com.example.musebox.R;
import com.example.musebox.database.PlaylistDatabaseHelper;
import com.example.musebox.models.Playlist;
import com.example.musebox.utils.PlaylistDialogHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class PlaylistFragment extends Fragment {
    private RecyclerView recyclerView;
    private PlaylistAdapter adapter;
    private PlaylistDatabaseHelper dbHelper;
    private FloatingActionButton fabCreate;
    private LinearLayout emptyStateView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerPlaylists);
        fabCreate = view.findViewById(R.id.fabCreatePlaylist);
        emptyStateView = view.findViewById(R.id.emptyStateView);
        dbHelper = new PlaylistDatabaseHelper(requireContext());

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PlaylistAdapter(playlist -> {
            // Open PlaylistDetailFragment
            PlaylistDetailFragment fragment = PlaylistDetailFragment.newInstance(playlist.getId());
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Add menu click listener for edit/delete menu
        adapter.setOnPlaylistMenuClickListener((playlist, anchorView) -> showPlaylistOptionsMenu(playlist, anchorView));

        recyclerView.setAdapter(adapter);

        // Load playlists
        loadPlaylists();

        // Setup FAB click listener
        fabCreate.setOnClickListener(v -> showCreatePlaylistDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload playlists when returning to this fragment (in case changes were made
        // in PlaylistDetailFragment)
        loadPlaylists();
    }

    private void showPlaylistOptionsMenu(Playlist playlist, View anchorView) {
        // Use the provided anchor view for the popup menu
        PopupMenu popup = new PopupMenu(requireContext(), anchorView);
        popup.getMenuInflater().inflate(R.menu.menu_playlist_options, popup.getMenu());

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
                showEditPlaylistDialog(playlist);
                return true;
            } else if (itemId == R.id.menu_delete_playlist) {
                showDeletePlaylistDialog(playlist);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void showEditPlaylistDialog(Playlist playlist) {
        PlaylistDialogHelper.showEditPlaylistDialog(requireContext(), playlist,
                (playlistId, newName, newDescription) -> {
                    // Reload playlists after edit
                    loadPlaylists();
                });
    }

    private void showDeletePlaylistDialog(Playlist playlist) {
        PlaylistDialogHelper.showDeletePlaylistDialog(requireContext(), playlist, deletedPlaylist -> {
            boolean success = dbHelper.deletePlaylist(deletedPlaylist.getId());
            if (success) {
                Toast.makeText(requireContext(), "Playlist deleted", Toast.LENGTH_SHORT).show();
                loadPlaylists();
            } else {
                Toast.makeText(requireContext(), "Failed to delete playlist", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPlaylists() {
        List<Playlist> playlists = dbHelper.getAllPlaylists();
        adapter.setPlaylists(playlists);
        updateEmptyState(playlists.isEmpty());
    }

    /**
     * Updates the visibility of the empty state view based on whether playlists
     * exist
     * 
     * @param isEmpty true if there are no playlists, false otherwise
     */
    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            emptyStateView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showCreatePlaylistDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_playlist, null);
        final android.widget.EditText etName = dialogView.findViewById(R.id.etPlaylistName);
        final android.widget.EditText etDesc = dialogView.findViewById(R.id.etPlaylistDescription);
        final RecyclerView rvSongs = dialogView.findViewById(R.id.recyclerSongs);
        final android.widget.Button btnConfirm = dialogView.findViewById(R.id.btnConfirmCreatePlaylist);

        // Load all songs from the database
        com.example.musebox.database.SongDatabaseHelper songDbHelper = new com.example.musebox.database.SongDatabaseHelper(
                requireContext());
        List<com.example.musebox.models.Song> allSongs = songDbHelper.getAllSongs();

        // Adapter for selecting songs
        SongSelectAdapter songSelectAdapter = new SongSelectAdapter(allSongs);
        rvSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSongs.setAdapter(songSelectAdapter);

        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(
                requireContext())
                .setTitle("Create Playlist")
                .setView(dialogView)
                .setCancelable(true)
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

        btnConfirm.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Playlist name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            // Create playlist and add selected songs
            String playlistId = dbHelper.createPlaylist(name, desc);
            List<com.example.musebox.models.Song> selectedSongs = songSelectAdapter.getSelectedSongs();
            for (com.example.musebox.models.Song song : selectedSongs) {
                dbHelper.addSongToPlaylist(playlistId, song);
            }
            loadPlaylists();
            Toast.makeText(requireContext(), "Playlist created!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    // Adapter for selecting songs with checkboxes
    private static class SongSelectAdapter extends RecyclerView.Adapter<SongSelectAdapter.SongViewHolder> {
        private final List<com.example.musebox.models.Song> songs;
        private final List<com.example.musebox.models.Song> selected = new ArrayList<>();

        public SongSelectAdapter(List<com.example.musebox.models.Song> songs) {
            this.songs = songs;
        }

        public List<com.example.musebox.models.Song> getSelectedSongs() {
            return selected;
        }

        @NonNull
        @Override
        public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_select, parent, false);
            return new SongViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
            com.example.musebox.models.Song song = songs.get(position);
            holder.bind(song, selected);
        }

        @Override
        public int getItemCount() {
            return songs == null ? 0 : songs.size();
        }

        static class SongViewHolder extends RecyclerView.ViewHolder {
            private final android.widget.TextView tvTitle;
            private final android.widget.TextView tvArtist;
            private final android.widget.CheckBox cbSelect;

            public SongViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvArtist = itemView.findViewById(R.id.tvArtist);
                cbSelect = itemView.findViewById(R.id.cbSelectSong);
            }

            public void bind(final com.example.musebox.models.Song song,
                    final List<com.example.musebox.models.Song> selected) {
                tvTitle.setText(song.getTitle());
                tvArtist.setText(song.getArtist());
                cbSelect.setChecked(selected.contains(song));
                cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        if (!selected.contains(song))
                            selected.add(song);
                    } else {
                        selected.remove(song);
                    }
                });
            }
        }
    }
}
