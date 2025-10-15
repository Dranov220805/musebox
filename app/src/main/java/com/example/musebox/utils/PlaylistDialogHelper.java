package com.example.musebox.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.database.PlaylistDatabaseHelper;
import com.example.musebox.models.Playlist;
import com.example.musebox.models.Song;

import java.util.List;

/**
 * Helper class for showing playlist selection dialogs
 */
public class PlaylistDialogHelper {

    public interface OnPlaylistSelectedListener {
        void onPlaylistSelected(Playlist playlist);
    }

    /**
     * Show a dialog to select a playlist to add a song to
     * 
     * @param context  Context
     * @param song     Song to add to playlist
     * @param listener Callback when playlist is selected
     */
    public static void showAddToPlaylistDialog(Context context, Song song, OnPlaylistSelectedListener listener) {
        PlaylistDatabaseHelper dbHelper = new PlaylistDatabaseHelper(context);
        List<Playlist> playlists = dbHelper.getAllPlaylists();

        // Check if there are any playlists
        if (playlists == null || playlists.isEmpty()) {
            showNoPlaylistsDialog(context);
            return;
        }

        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Add to Playlist");

        // Create list of playlist names
        String[] playlistNames = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) {
            playlistNames[i] = playlists.get(i).getName();
        }

        builder.setItems(playlistNames, (dialog, which) -> {
            Playlist selectedPlaylist = playlists.get(which);

            // Add song to selected playlist
            boolean success = dbHelper.addSongToPlaylist(selectedPlaylist.getId(), song);

            if (success) {
                Toast.makeText(context, "Added to " + selectedPlaylist.getName(), Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onPlaylistSelected(selectedPlaylist);
                }
            } else {
                Toast.makeText(context, "Song already in playlist or failed to add", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Show dialog when there are no playlists available
     */
    private static void showNoPlaylistsDialog(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("No Playlists")
                .setMessage("You don't have any playlists yet. Create a playlist first from the Playlist tab.")
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Show dialog to confirm playlist deletion
     */
    public static void showDeletePlaylistDialog(Context context, Playlist playlist,
            OnDeleteConfirmedListener listener) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Playlist")
                .setMessage(
                        "Are you sure you want to delete \"" + playlist.getName() + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (listener != null) {
                        listener.onDeleteConfirmed(playlist);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Show dialog to edit playlist name and description
     */
    public static void showEditPlaylistDialog(Context context, Playlist playlist, OnPlaylistEditedListener listener) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_playlist, null);
        android.widget.EditText etName = dialogView.findViewById(R.id.etPlaylistName);
        android.widget.EditText etDesc = dialogView.findViewById(R.id.etPlaylistDescription);

        // Hide the songs recycler view as we're just editing playlist info
        RecyclerView rvSongs = dialogView.findViewById(R.id.recyclerSongs);
        if (rvSongs != null) {
            rvSongs.setVisibility(View.GONE);
        }

        // Pre-fill with existing data
        etName.setText(playlist.getName());
        etDesc.setText(playlist.getDescription());

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Edit Playlist")
                .setView(dialogView)
                .setPositiveButton("Save", null) // Set to null to handle click manually
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                String newName = etName.getText().toString().trim();
                String newDesc = etDesc.getText().toString().trim();

                if (newName.isEmpty()) {
                    Toast.makeText(context, "Playlist name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                PlaylistDatabaseHelper dbHelper = new PlaylistDatabaseHelper(context);

                // Update name if changed
                if (!newName.equals(playlist.getName())) {
                    dbHelper.renamePlaylist(playlist.getId(), newName);
                }

                // Update description if changed
                if (!newDesc.equals(playlist.getDescription())) {
                    dbHelper.updatePlaylistDescription(playlist.getId(), newDesc);
                }

                Toast.makeText(context, "Playlist updated", Toast.LENGTH_SHORT).show();

                if (listener != null) {
                    listener.onPlaylistEdited(playlist.getId(), newName, newDesc);
                }

                dialog.dismiss();
            });
        });

        dialog.show();
    }

    public interface OnDeleteConfirmedListener {
        void onDeleteConfirmed(Playlist playlist);
    }

    public interface OnPlaylistEditedListener {
        void onPlaylistEdited(String playlistId, String newName, String newDescription);
    }
}
