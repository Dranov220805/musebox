package com.example.musebox.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.models.GenreSection;
import com.example.musebox.models.MusicRecommendation;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying genre sections with horizontal scrolling music cards
 * (Spotify-like design with multiple horizontal rows)
 */
public class GenreSectionAdapter extends RecyclerView.Adapter<GenreSectionAdapter.GenreSectionViewHolder> {

    private List<GenreSection> genreSections = new ArrayList<>();
    private OnMusicClickListener musicClickListener;
    private OnSeeAllClickListener seeAllClickListener;

    public interface OnMusicClickListener {
        void onMusicClick(MusicRecommendation music, String genre);
    }

    public interface OnSeeAllClickListener {
        void onSeeAllClick(String genre);
    }

    public void setGenreSections(List<GenreSection> sections) {
        this.genreSections = sections;
        notifyDataSetChanged();
    }

    public void setOnMusicClickListener(OnMusicClickListener listener) {
        this.musicClickListener = listener;
    }

    public void setOnSeeAllClickListener(OnSeeAllClickListener listener) {
        this.seeAllClickListener = listener;
    }

    @NonNull
    @Override
    public GenreSectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_genre_section, parent, false);
        return new GenreSectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GenreSectionViewHolder holder, int position) {
        GenreSection section = genreSections.get(position);
        holder.bind(section);
    }

    @Override
    public int getItemCount() {
        return genreSections.size();
    }

    class GenreSectionViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvGenreTitle;
        private final TextView tvSeeAll;
        private final RecyclerView recyclerMusic;
        private final MusicCardAdapter musicAdapter;

        public GenreSectionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGenreTitle = itemView.findViewById(R.id.tvGenreTitle);
            tvSeeAll = itemView.findViewById(R.id.tvSeeAll);
            recyclerMusic = itemView.findViewById(R.id.recyclerMusic);

            // Setup horizontal RecyclerView
            LinearLayoutManager layoutManager = new LinearLayoutManager(
                    itemView.getContext(),
                    LinearLayoutManager.HORIZONTAL,
                    false);
            recyclerMusic.setLayoutManager(layoutManager);

            musicAdapter = new MusicCardAdapter();
            recyclerMusic.setAdapter(musicAdapter);
        }

        public void bind(GenreSection section) {
            // Capitalize first letter of genre
            String displayGenre = section.getGenreName().substring(0, 1).toUpperCase()
                    + section.getGenreName().substring(1);
            tvGenreTitle.setText(displayGenre);

            // Set music list
            musicAdapter.setMusicList(section.getMusicList());

            // Set click listeners
            musicAdapter.setOnMusicCardClickListener(music -> {
                if (musicClickListener != null) {
                    musicClickListener.onMusicClick(music, section.getGenreName());
                }
            });

            tvSeeAll.setOnClickListener(v -> {
                if (seeAllClickListener != null) {
                    seeAllClickListener.onSeeAllClick(section.getGenreName());
                }
            });
        }
    }
}
