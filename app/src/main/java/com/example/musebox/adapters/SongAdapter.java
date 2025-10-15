package com.example.musebox.adapters;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import com.example.musebox.R;
import com.example.musebox.models.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Optimized Song adapter using ListAdapter with DiffUtil for efficient updates.
 * Features:
 * - DiffUtil for calculating minimal updates
 * - Glide for asynchronous image loading with caching
 * - ViewHolder pattern for view recycling
 */
public class SongAdapter extends ListAdapter<Song, SongAdapter.SongViewHolder> {

    private OnSongClickListener listener;
    private OnSongMenuListener menuListener;
    private MenuActionOverride menuActionOverride; // For custom menu actions (like in FavoritesActivity)
    private OnSongFileDeletedListener fileDeletedListener;

    public SongAdapter() {
        super(new SongDiffCallback());
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = getItem(position);

        // Check if the song file still exists before binding
        if (!isFileExists(song.getUri())) {
            android.util.Log.w("SongAdapter", "Song file no longer exists: " + song.getUri() + " - " + song.getTitle());
            
            // Notify the listener about the deleted file
            if (fileDeletedListener != null) {
                // Use Handler to post to main thread to avoid any potential issues
                new Handler(Looper.getMainLooper()).post(() -> 
                    fileDeletedListener.onSongFileDeleted(song, position)
                );
            }
            
            // Still show the song but indicate it's unavailable
            holder.tvTitle.setText(song.getTitle() + " (Unavailable)");
            holder.tvArtist.setText(song.getArtist());
            holder.tvDuration.setText("--:--");
            holder.ivAlbumArt.setImageResource(R.drawable.ic_music_note);
            
            // Disable click listeners for unavailable songs
            holder.itemView.setOnClickListener(null);
            holder.btnMenu.setOnClickListener(null);
            
            // Gray out the item to indicate it's unavailable
            holder.itemView.setAlpha(0.5f);
            return;
        }

        // Reset appearance for available songs
        holder.itemView.setAlpha(1.0f);
        
        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());

        long durationMs = song.getDuration();
        long minutes = (durationMs / 1000) / 60;
        long seconds = (durationMs / 1000) % 60;
        holder.tvDuration.setText(String.format("%d:%02d", minutes, seconds));

        // Load album art asynchronously with Glide (with caching)
        loadAlbumArt(holder.ivAlbumArt, song);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null)
                listener.onSongClick(song);
        });

        // Menu button click
        holder.btnMenu.setOnClickListener(v -> {
            // If custom menu action is set (like for FavoritesActivity), use it instead
            if (menuActionOverride != null) {
                menuActionOverride.onMenuAction(song, position);
                return;
            }

            // Default menu behavior
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.inflate(R.menu.menu_song_options);

            popup.setOnMenuItemClickListener(item -> {
                if (menuListener != null) {
                    int itemId = item.getItemId();
                    if (itemId == R.id.menu_add_to_queue) {
                        menuListener.onAddToQueue(song);
                        return true;
                    } else if (itemId == R.id.menu_add_to_favourite) {
                        menuListener.onAddToFavourite(song);
                        return true;
                    } else if (itemId == R.id.menu_add_to_playlist) {
                        menuListener.onAddToPlaylist(song);
                        return true;
                    } else if (itemId == R.id.menu_delete_song) {
                        menuListener.onDeleteSong(song, position);
                        return true;
                    }
                }
                return false;
            });

            popup.show();
        });
    }

    /**
     * Load album art asynchronously using Glide with caching.
     * This prevents blocking the main thread and caches images for better
     * performance.
     * Prioritizes custom album cover path over embedded audio file art.
     */
    private void loadAlbumArt(ImageView imageView, Song song) {
        // TODO: TEMPORARY LOG - Check album art loading process
        android.util.Log.d("AlbumArt", "Loading album art for: " + song.getTitle());

        // Check if song has a custom album cover path
        String albumCoverPath = song.getAlbumCoverPath();

        if (albumCoverPath != null && !albumCoverPath.isEmpty()) {
            android.util.Log.d("AlbumArt", "Using custom album cover path: " + albumCoverPath);
            // Load from custom album cover path (file path or URI)
            Glide.with(imageView.getContext())
                    .load(albumCoverPath)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(200, 200) // Resize for better performance
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_music_note) // Show default on error
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target,
                                boolean isFirstResource) {
                            android.util.Log.d("AlbumArt", "Custom album cover FAILED for: " + song.getTitle()
                                    + ", falling back to embedded art");
                            
                            // Use Handler to post fallback request to main thread to avoid Glide callback restriction
                            new Handler(Looper.getMainLooper()).post(() -> {
                                // Check if file still exists before attempting to load
                                if (isFileExists(song.getUri())) {
                                    loadEmbeddedAlbumArt(imageView, song.getUri());
                                } else {
                                    android.util.Log.w("AlbumArt", "Audio file no longer exists: " + song.getUri());
                                    // Just show default icon if file doesn't exist
                                    imageView.setImageResource(R.drawable.ic_music_note);
                                }
                            });
                            return true; // Indicate that we handled the failure
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
                                DataSource dataSource, boolean isFirstResource) {
                            return false; // Let Glide handle the success case
                        }
                    })
                    .centerCrop()
                    .into(imageView);
        } else {
            android.util.Log.d("AlbumArt",
                    "No custom album cover path, trying embedded art from URI: " + song.getUri());
            // No custom cover, check if file exists before loading embedded art
            if (isFileExists(song.getUri())) {
                loadEmbeddedAlbumArt(imageView, song.getUri());
            } else {
                android.util.Log.w("AlbumArt", "Audio file no longer exists: " + song.getUri());
                // Show default icon if file doesn't exist
                imageView.setImageResource(R.drawable.ic_music_note);
            }
        }
    }

    /**
     * Check if the audio file still exists on the device
     */
    private boolean isFileExists(String filePath) {
        try {
            if (filePath == null || filePath.isEmpty()) {
                return false;
            }
            
            Uri uri = Uri.parse(filePath);
            if ("file".equals(uri.getScheme())) {
                // For file:// URIs, check if file exists
                File file = new File(uri.getPath());
                return file.exists();
            } else if ("content".equals(uri.getScheme())) {
                // For content:// URIs, we assume they exist
                // (MediaStore should handle this case)
                return true;
            } else {
                // For other schemes, assume they exist
                return true;
            }
        } catch (Exception e) {
            android.util.Log.w("AlbumArt", "Error checking file existence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Load embedded album art from audio file
     */
    private void loadEmbeddedAlbumArt(ImageView imageView, String audioFilePath) {
        Uri audioUri = Uri.parse(audioFilePath);
        android.util.Log.d("AlbumArt", "Attempting to load embedded album art from URI: " + audioUri.toString());

        Glide.with(imageView.getContext())
                .asBitmap() // Explicitly request bitmap to properly extract embedded art
                .load(audioUri)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .override(200, 200) // Resize for better performance
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .centerCrop()
                .timeout(3000) // 3 second timeout to prevent hanging
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.Bitmap>() {
                    @Override
                    public boolean onLoadFailed(
                            @androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model,
                            com.bumptech.glide.request.target.Target<android.graphics.Bitmap> target,
                            boolean isFirstResource) {
                        android.util.Log.d("AlbumArt",
                                "✗ FAILED to load embedded album art from: " + audioUri.toString());
                        
                        // Log the reason for failure without causing crashes
                        if (e != null) {
                            android.util.Log.w("AlbumArt", "Glide load failed: " + e.getMessage());
                        }
                        
                        // Don't start any new Glide requests here - just let it show the error drawable
                        return false; // Let Glide handle showing error drawable
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.Bitmap resource, Object model,
                            com.bumptech.glide.request.target.Target<android.graphics.Bitmap> target,
                            com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        android.util.Log.d("AlbumArt", "✓ SUCCESS loaded embedded album art from: "
                                + audioUri.toString() + " (DataSource: " + dataSource + ")");
                        return false; // Let Glide handle the success case
                    }
                })
                .into(imageView);
    }

    // Helper methods for compatibility with existing code
    public void setSongs(List<Song> newSongs) {
        submitList(new ArrayList<>(newSongs));
    }

    public void addSongs(List<Song> newSongs) {
        if (newSongs == null || newSongs.isEmpty()) {
            return;
        }

        List<Song> currentList = getCurrentList();

        // Pre-allocate the list with the exact size needed to avoid resizing
        List<Song> updatedList = new ArrayList<>(currentList.size() + newSongs.size());
        updatedList.addAll(currentList);
        updatedList.addAll(newSongs);

        // Let ListAdapter handle the DiffUtil calculation and notifications efficiently
        submitList(updatedList);
    }

    public void removeSong(int position) {
        List<Song> currentList = new ArrayList<>(getCurrentList());
        if (position >= 0 && position < currentList.size()) {
            currentList.remove(position);
            submitList(currentList);
        }
    }

    public void clearSongs() {
        submitList(new ArrayList<>());
    }

    /**
     * Get all currently displayed songs in the adapter
     * @return List of songs currently shown
     */
    public List<Song> getAllSongs() {
        return new ArrayList<>(getCurrentList());
    }

    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    public interface OnSongMenuListener {
        void onAddToQueue(Song song);

        void onAddToFavourite(Song song);

        void onAddToPlaylist(Song song);

        void onDeleteSong(Song song, int position);
    }

    public interface MenuActionOverride {
        void onMenuAction(Song song, int position);
    }

    public interface OnSongFileDeletedListener {
        void onSongFileDeleted(Song song, int position);
    }

    public void setOnSongClickListener(OnSongClickListener listener) {
        this.listener = listener;
    }

    public void setOnSongMenuListener(OnSongMenuListener menuListener) {
        this.menuListener = menuListener;
    }

    public void setMenuActionOverride(MenuActionOverride override) {
        this.menuActionOverride = override;
    }

    public void setOnSongFileDeletedListener(OnSongFileDeletedListener listener) {
        this.fileDeletedListener = listener;
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAlbumArt;
        TextView tvTitle, tvArtist, tvDuration;
        ImageButton btnMenu;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAlbumArt = itemView.findViewById(R.id.ivAlbumArt);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            btnMenu = itemView.findViewById(R.id.btnMenu);
        }
    }
}
