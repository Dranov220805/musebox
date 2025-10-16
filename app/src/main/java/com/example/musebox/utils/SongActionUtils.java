package com.example.musebox.utils;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.musebox.R;
import com.example.musebox.database.PlaylistDatabaseHelper;
import com.example.musebox.database.SongDatabaseHelper;
import com.example.musebox.models.Song;
import com.example.musebox.services.MusicService;

import java.io.File;

/**
 * Utility class for common song-related actions to avoid code duplication.
 * Consolidates:
 * - Favorite management (toggle, add, remove)
 * - Song deletion (from device, from library)
 * - Queue management
 * - Playlist song removal
 */
public class SongActionUtils {

    // ==================== FAVORITES OPERATIONS ====================

    /**
     * Toggle favorite status for a song with UI feedback
     * 
     * @param context     Context for database and Toast
     * @param song        Song to toggle
     * @param dbHelper    Database helper instance
     * @param onCompleted Callback after operation completes (optional)
     */
    public static void toggleFavorite(Context context, Song song, SongDatabaseHelper dbHelper,
            OnFavoriteToggledListener onCompleted) {
        new Thread(() -> {
            boolean isFavorite = dbHelper.toggleFavorite(song.getId());
            song.setFavorite(isFavorite);

            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    String message = isFavorite
                            ? "Added \"" + song.getTitle() + "\" to favorites"
                            : "Removed \"" + song.getTitle() + "\" from favorites";
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

                    if (onCompleted != null) {
                        onCompleted.onFavoriteToggled(song, isFavorite);
                    }
                });
            }
        }).start();
    }

    /**
     * Add song to favorites with UI feedback
     * 
     * @param context  Context for database and Toast
     * @param song     Song to add
     * @param dbHelper Database helper instance
     */
    public static void addToFavorites(Context context, Song song, SongDatabaseHelper dbHelper) {
        new Thread(() -> {
            boolean success = dbHelper.addToFavorites(song.getId());
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    /**
     * Remove song from favorites with UI feedback
     * 
     * @param context     Context for database and Toast
     * @param song        Song to remove
     * @param dbHelper    Database helper instance
     * @param onCompleted Callback after operation completes (optional)
     */
    public static void removeFromFavorites(Context context, Song song, SongDatabaseHelper dbHelper,
            OnFavoriteRemovedListener onCompleted) {
        new Thread(() -> {
            boolean success = dbHelper.removeFromFavorites(song.getId());
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
                        if (onCompleted != null) {
                            onCompleted.onFavoriteRemoved(song);
                        }
                    }
                });
            }
        }).start();
    }

    // ==================== SONG DELETION OPERATIONS ====================

    /**
     * Show confirmation dialog for deleting a song
     * Offers options: Delete from device, Remove from library only, Cancel
     * 
     * @param context  Context for dialog
     * @param song     Song to delete
     * @param position Position in adapter
     * @param callback Callback for deletion actions
     */
    public static void showDeleteConfirmationDialog(Context context, Song song, int position,
            OnSongDeleteListener callback) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_remove_song, null);

        AlertDialog dialog = new AlertDialog.Builder(context)
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
            callback.onDeleteFromDevice(song, position);
        });

        dialogView.findViewById(R.id.btn_remove_library).setOnClickListener(v -> {
            dialog.dismiss();
            callback.onDeleteFromLibrary(song, position);
        });

        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Delete song from device (physical file + database entry)
     * 
     * @param context     Context for database and Toast
     * @param song        Song to delete
     * @param dbHelper    Database helper instance
     * @param onCompleted Callback after operation completes
     */
    public static void deleteSongFromDevice(Context context, Song song, SongDatabaseHelper dbHelper,
            OnSongDeletedListener onCompleted) {
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
                dbDeleted = true;

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

            boolean finalFileDeleted = fileDeleted;
            boolean finalDbDeleted = dbDeleted;
            String finalErrorMessage = errorMessage;

            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    if (finalDbDeleted) {
                        // Show appropriate message
                        String message;
                        if (finalFileDeleted && finalErrorMessage == null) {
                            message = "Successfully deleted \"" + song.getTitle() + "\" from device";
                        } else if (finalFileDeleted) {
                            message = "Deleted \"" + song.getTitle() + "\" (" + finalErrorMessage + ")";
                        } else {
                            message = "Removed from library only. " + finalErrorMessage;
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show();

                        if (onCompleted != null) {
                            onCompleted.onSongDeleted(song, finalDbDeleted);
                        }
                    } else {
                        Toast.makeText(context, "Failed to remove from database: " + finalErrorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    /**
     * Delete song from library only (database entry), keep physical file
     * 
     * @param context     Context for database and Toast
     * @param song        Song to delete
     * @param dbHelper    Database helper instance
     * @param onCompleted Callback after operation completes
     */
    public static void deleteSongFromLibrary(Context context, Song song, SongDatabaseHelper dbHelper,
            OnSongDeletedListener onCompleted) {
        new Thread(() -> {
            boolean success = false;
            String errorMessage = null;

            try {
                // Only remove from database, keep the file on device
                dbHelper.deleteSong(song.getId());
                success = true;
            } catch (Exception e) {
                errorMessage = "Database error: " + e.getMessage();
                success = false;
            }

            boolean finalSuccess = success;
            String finalErrorMessage = errorMessage;

            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    if (finalSuccess) {
                        Toast.makeText(context,
                                "Removed \"" + song.getTitle() + "\" from library (file kept on device)",
                                Toast.LENGTH_SHORT).show();

                        if (onCompleted != null) {
                            onCompleted.onSongDeleted(song, true);
                        }
                    } else {
                        Toast.makeText(context, finalErrorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    // ==================== PLAYLIST OPERATIONS ====================

    /**
     * Show dialog to remove song from a playlist
     * 
     * @param context    Context for dialog
     * @param song       Song to remove
     * @param playlistId Playlist ID
     * @param dbHelper   Playlist database helper
     * @param onRemoved  Callback after song is removed
     */
    public static void showRemoveSongFromPlaylistDialog(Context context, Song song, String playlistId,
            PlaylistDatabaseHelper dbHelper, OnPlaylistSongRemovedListener onRemoved) {
        ThemedDialogUtils.showSimpleDialog(
                context,
                "Remove Song",
                "Remove \"" + song.getTitle() + "\" from this playlist?",
                R.drawable.ic_delete,
                android.R.color.holo_red_dark,
                "Remove",
                "Cancel",
                new ThemedDialogUtils.OnDialogClickListener() {
                    @Override
                    public void onPositiveClick() {
                        boolean removed = dbHelper.removeSongFromPlaylist(playlistId, song.getId());
                        if (removed) {
                            Toast.makeText(context, "Song removed", Toast.LENGTH_SHORT).show();
                            if (onRemoved != null) {
                                onRemoved.onSongRemoved(song);
                            }
                        } else {
                            Toast.makeText(context, "Failed to remove song", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // ==================== QUEUE OPERATIONS ====================

    /**
     * Add song to queue with UI feedback
     * 
     * @param context      Context for Toast
     * @param song         Song to add
     * @param musicService Music service instance
     */
    public static void addToQueue(Context context, Song song, MusicService musicService) {
        if (musicService != null) {
            musicService.addToQueue(song);
            Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Music service not available", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Add song to queue at specific position with UI feedback
     * 
     * @param context      Context for Toast
     * @param song         Song to add
     * @param position     Position in queue
     * @param musicService Music service instance
     */
    public static void addToQueue(Context context, Song song, int position, MusicService musicService) {
        if (musicService != null) {
            musicService.addToQueue(song, position);
            Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Music service not available", Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== CALLBACK INTERFACES ====================

    public interface OnFavoriteToggledListener {
        void onFavoriteToggled(Song song, boolean isFavorite);
    }

    public interface OnFavoriteRemovedListener {
        void onFavoriteRemoved(Song song);
    }

    public interface OnSongDeleteListener {
        void onDeleteFromDevice(Song song, int position);

        void onDeleteFromLibrary(Song song, int position);
    }

    public interface OnSongDeletedListener {
        void onSongDeleted(Song song, boolean success);
    }

    public interface OnPlaylistSongRemovedListener {
        void onSongRemoved(Song song);
    }
}
