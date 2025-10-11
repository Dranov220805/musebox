package com.example.musebox.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.models.Song;

import java.util.List;

public class FavoriteSongAdapter extends RecyclerView.Adapter<FavoriteSongAdapter.FavoriteViewHolder> {

    private List<Song> favoriteSongs;
    private OnFavoriteSongClickListener listener;

    public interface OnFavoriteSongClickListener {
        void onFavoriteSongClick(Song song);
    }

    public FavoriteSongAdapter(List<Song> favoriteSongs) {
        this.favoriteSongs = favoriteSongs;
    }

    public void setOnFavoriteSongClickListener(OnFavoriteSongClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_song, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        Song song = favoriteSongs.get(position);
        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteSongClick(song);
            }
        });
    }

    @Override
    public int getItemCount() {
        return favoriteSongs != null ? favoriteSongs.size() : 0;
    }

    public void updateFavorites(List<Song> newFavorites) {
        this.favoriteSongs = newFavorites;
        notifyDataSetChanged();
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvArtist;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
        }
    }
}
