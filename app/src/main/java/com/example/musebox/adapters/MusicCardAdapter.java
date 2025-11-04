package com.example.musebox.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.musebox.R;
import com.example.musebox.models.MusicRecommendation;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class MusicCardAdapter extends RecyclerView.Adapter<MusicCardAdapter.ViewHolder> {

    private List<MusicRecommendation> musicList = new ArrayList<>();
    private OnMusicCardClickListener listener;

    public interface OnMusicCardClickListener {
        void onMusicCardClick(MusicRecommendation music);
    }

    public void setOnMusicCardClickListener(OnMusicCardClickListener listener) {
        this.listener = listener;
    }

    public void setMusicList(List<MusicRecommendation> musicList) {
        this.musicList = musicList != null ? musicList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_music_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MusicRecommendation music = musicList.get(position);
        holder.bind(music);
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView musicCard;
        private ImageView ivAlbumCover;
        private TextView tvTrackName;
        private TextView tvArtistName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            musicCard = itemView.findViewById(R.id.musicCard);
            ivAlbumCover = itemView.findViewById(R.id.ivAlbumCover);
            tvTrackName = itemView.findViewById(R.id.tvTrackName);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
        }

        public void bind(MusicRecommendation music) {
            tvTrackName.setText(music.getTrackName());
            tvArtistName.setText(music.getArtistName());

            // Load album cover with Glide
            if (music.getImageUrl() != null && !music.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(music.getImageUrl())
                        .apply(new RequestOptions()
                                .transform(new RoundedCorners(8))
                                .placeholder(R.drawable.ic_music_note)
                                .error(R.drawable.ic_music_note))
                        .into(ivAlbumCover);
            } else {
                ivAlbumCover.setImageResource(R.drawable.ic_music_note);
            }

            // Click listener
            musicCard.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMusicCardClick(music);
                }
            });
        }
    }
}
