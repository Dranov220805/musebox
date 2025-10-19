package com.example.musebox.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musebox.R;
import com.example.musebox.models.Playlist;
import com.example.musebox.models.Song;

import java.io.File;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {
    private List<Playlist> playlists;
    private OnPlaylistClickListener listener;
    private OnPlaylistMenuClickListener menuClickListener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public interface OnPlaylistMenuClickListener {
        void onPlaylistMenuClick(Playlist playlist, View anchorView);
    }

    public PlaylistAdapter() {
    }

    public PlaylistAdapter(OnPlaylistClickListener listener) {
        this.listener = listener;
    }

    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists;
        notifyDataSetChanged();
    }

    public void setOnPlaylistClickListener(OnPlaylistClickListener listener) {
        this.listener = listener;
    }

    public void setOnPlaylistMenuClickListener(OnPlaylistMenuClickListener menuClickListener) {
        this.menuClickListener = menuClickListener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.bind(playlist, listener, menuClickListener);
    }

    @Override
    public int getItemCount() {
        return playlists == null ? 0 : playlists.size();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvDescription;
        private final ImageButton btnMenu;
        private final ImageView ivPlaylistIcon;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPlaylistName);
            tvDescription = itemView.findViewById(R.id.tvPlaylistDescription);
            btnMenu = itemView.findViewById(R.id.btnMenu);
            ivPlaylistIcon = itemView.findViewById(R.id.ivPlaylistIcon);
        }

        public void bind(final Playlist playlist, final OnPlaylistClickListener listener,
                final OnPlaylistMenuClickListener menuClickListener) {
            tvName.setText(playlist.getName());
            tvDescription.setText(playlist.getDescription());

            // Load playlist image from first song's album cover
            if (playlist.getSongs() != null && !playlist.getSongs().isEmpty()) {
                Song firstSong = playlist.getSongs().get(0);
                String albumCoverPath = firstSong.getAlbumCoverPath();

                if (albumCoverPath != null && !albumCoverPath.isEmpty()) {
                    File albumArtFile = new File(albumCoverPath);
                    if (albumArtFile.exists()) {
                        Glide.with(itemView.getContext())
                                .load(albumArtFile)
                                .placeholder(R.drawable.ic_playlist)
                                .error(R.drawable.ic_playlist)
                                .centerCrop()
                                .into(ivPlaylistIcon);
                    } else {
                        ivPlaylistIcon.setImageResource(R.drawable.ic_playlist);
                    }
                } else {
                    ivPlaylistIcon.setImageResource(R.drawable.ic_playlist);
                }
            } else {
                // No songs in playlist, use default icon
                ivPlaylistIcon.setImageResource(R.drawable.ic_playlist);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaylistClick(playlist);
                }
            });

            btnMenu.setOnClickListener(v -> {
                if (menuClickListener != null) {
                    menuClickListener.onPlaylistMenuClick(playlist, v);
                }
            });
        }
    }
}
