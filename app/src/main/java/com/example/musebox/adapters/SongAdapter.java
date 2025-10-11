package com.example.musebox.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musebox.R;
import com.example.musebox.models.Song;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songs;
    private OnSongClickListener listener;
    private OnSongMenuListener menuListener;
    private MenuActionOverride menuActionOverride; // For custom menu actions (like in FavoritesActivity)

    public SongAdapter(List<Song> songs) {
        this.songs = songs;
    }

    public SongAdapter(List<Song> songs, OnSongClickListener listener) {
        this.songs = songs;
        this.listener = listener;
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
        Song song = songs.get(position);

        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());

        long durationMs = song.getDuration();
        long minutes = (durationMs / 1000) / 60;
        long seconds = (durationMs / 1000) % 60;
        holder.tvDuration.setText(String.format("%d:%02d", minutes, seconds));

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

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    public void setSongs(List<Song> newSongs) {
        if (songs != null) {
            songs.clear();
            songs.addAll(newSongs);
        } else {
            songs = newSongs;
        }
        notifyDataSetChanged();
    }

    public void addSongs(List<Song> newSongs) {
        if (songs == null) {
            songs = new ArrayList<>();
        }
        int startPosition = songs.size();
        songs.addAll(newSongs);
        notifyItemRangeInserted(startPosition, newSongs.size());
    }

    public void removeSong(int position) {
        if (songs != null && position >= 0 && position < songs.size()) {
            songs.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, songs.size());
        }
    }

    public void clearSongs() {
        if (songs != null) {
            songs.clear();
            notifyDataSetChanged();
        }
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
        TextView tvTitle, tvArtist, tvDuration;
        ImageButton btnMenu;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            btnMenu = itemView.findViewById(R.id.btnMenu);
        }
    }
}