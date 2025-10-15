package com.example.musebox.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple adapter for displaying artist names in filter dialog
 */
public class ArtistFilterAdapter extends RecyclerView.Adapter<ArtistFilterAdapter.ArtistViewHolder> {

    private List<String> artists;
    private OnArtistClickListener listener;

    public interface OnArtistClickListener {
        void onArtistClick(String artist);
    }

    public ArtistFilterAdapter(List<String> artists) {
        this.artists = artists != null ? artists : new ArrayList<>();
    }

    public void setOnArtistClickListener(OnArtistClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_artist_filter, parent, false);
        return new ArtistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
        String artist = artists.get(position);
        holder.tvArtistName.setText(artist);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onArtistClick(artist);
            }
        });
    }

    @Override
    public int getItemCount() {
        return artists.size();
    }

    static class ArtistViewHolder extends RecyclerView.ViewHolder {
        TextView tvArtistName;

        ArtistViewHolder(View itemView) {
            super(itemView);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
        }
    }
}
