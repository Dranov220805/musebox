package com.example.musebox.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class GenreCardAdapter extends RecyclerView.Adapter<GenreCardAdapter.ViewHolder> {

    private List<String> genreList = new ArrayList<>();
    private OnGenreCardClickListener listener;

    // Preset colors for genres (Spotify-like)
    private static final List<Integer> GENRE_COLORS = Arrays.asList(
            Color.parseColor("#1DB954"), // Spotify Green
            Color.parseColor("#E13300"), // Red
            Color.parseColor("#E8115B"), // Pink
            Color.parseColor("#509BF5"), // Blue
            Color.parseColor("#AF2896"), // Purple
            Color.parseColor("#8D67AB"), // Light Purple
            Color.parseColor("#DC148C"), // Magenta
            Color.parseColor("#F59B23"), // Orange
            Color.parseColor("#477D95"), // Teal
            Color.parseColor("#D11583") // Deep Pink
    );

    public interface OnGenreCardClickListener {
        void onGenreCardClick(String genre);
    }

    public void setOnGenreCardClickListener(OnGenreCardClickListener listener) {
        this.listener = listener;
    }

    public void setGenreList(List<String> genreList) {
        this.genreList = genreList != null ? genreList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_genre_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String genre = genreList.get(position);
        holder.bind(genre, position);
    }

    @Override
    public int getItemCount() {
        return genreList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView genreCard;
        private View genreOverlay;
        private TextView tvGenreName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            genreCard = itemView.findViewById(R.id.genreCard);
            genreOverlay = itemView.findViewById(R.id.genreOverlay);
            tvGenreName = itemView.findViewById(R.id.tvGenreName);
        }

        public void bind(String genre, int position) {
            // Capitalize first letter of genre
            String displayGenre = genre.substring(0, 1).toUpperCase() + genre.substring(1);
            tvGenreName.setText(displayGenre);

            // Set color from preset list (cycle through colors)
            int color = GENRE_COLORS.get(position % GENRE_COLORS.size());
            genreOverlay.setBackgroundColor(color);

            // Click listener
            genreCard.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onGenreCardClick(genre);
                }
            });
        }
    }
}
