package com.example.musebox.adapters;

import android.net.Uri;
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
import com.example.musebox.R;
import com.example.musebox.models.Song;

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

        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());

        long durationMs = song.getDuration();
        long minutes = (durationMs / 1000) / 60;
        long seconds = (durationMs / 1000) % 60;
        holder.tvDuration.setText(String.format("%d:%02d", minutes, seconds));

        // Load album art asynchronously with Glide (with caching)
        loadAlbumArt(holder.ivAlbumArt, song.getUri());

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
     * This prevents blocking the main thread and caches images for better performance.
     */
    private void loadAlbumArt(ImageView imageView, String audioFilePath) {
        // Try to load album art from audio file
        Uri audioUri = Uri.parse(audioFilePath);
        
        Glide.with(imageView.getContext())
                .load(audioUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache both original & resized image
                .placeholder(R.drawable.ic_music_note) // Show placeholder while loading
                .error(R.drawable.ic_music_note) // Show default icon on error
                .centerCrop()
                .into(imageView);
    }

    // Helper methods for compatibility with existing code
    public void setSongs(List<Song> newSongs) {
        submitList(new ArrayList<>(newSongs));
    }

    public void addSongs(List<Song> newSongs) {
        List<Song> currentList = new ArrayList<>(getCurrentList());
        currentList.addAll(newSongs);
        submitList(currentList);
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

    public void setOnSongClickListener(OnSongClickListener listener) {
        this.listener = listener;
    }

    public void setOnSongMenuListener(OnSongMenuListener menuListener) {
        this.menuListener = menuListener;
    }

    public void setMenuActionOverride(MenuActionOverride override) {
        this.menuActionOverride = override;
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
