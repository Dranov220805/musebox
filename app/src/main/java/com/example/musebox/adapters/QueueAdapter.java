package com.example.musebox.adapters;

import android.net.Uri;
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
import com.example.musebox.models.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    private List<Song> queue;
    private OnQueueItemListener listener;

    public interface OnQueueItemListener {
        void onRemoveFromQueue(int position);
    }

    public QueueAdapter(List<Song> queue) {
        this.queue = queue;
    }

    public void setOnQueueItemListener(OnQueueItemListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_queue_song, parent, false);
        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        Song song = queue.get(position);

        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());

        // Load album art
        loadAlbumArt(holder.ivAlbumArt, song);

        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onRemoveFromQueue(pos);
            }
        });
    }

    /**
     * Load album art for the song using Glide
     */
    private void loadAlbumArt(ImageView imageView, Song song) {
        // Set default placeholder
        imageView.setImageResource(R.drawable.ic_music_note);

        if (song.getAlbumCoverPath() != null && !song.getAlbumCoverPath().isEmpty()) {
            File albumArtFile = new File(song.getAlbumCoverPath());
            if (albumArtFile.exists()) {
                Glide.with(imageView.getContext())
                        .load(albumArtFile)
                        .placeholder(R.drawable.ic_music_note)
                        .error(R.drawable.ic_music_note)
                        .centerCrop()
                        .into(imageView);
                return;
            }
        }

        // Try to load from content URI
        try {
            Uri contentUri = Uri.parse(song.getUri());
            Glide.with(imageView.getContext())
                    .load(contentUri)
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_music_note)
                    .centerCrop()
                    .into(imageView);
        } catch (Exception e) {
            imageView.setImageResource(R.drawable.ic_music_note);
        }
    }

    @Override
    public int getItemCount() {
        return queue != null ? queue.size() : 0;
    }

    public void setQueue(List<Song> newQueue) {
        if (queue != null) {
            queue.clear();
        } else {
            queue = new ArrayList<>();
        }

        if (newQueue != null) {
            queue.addAll(newQueue);
        }

        notifyDataSetChanged();
    }

    public void clearQueue() {
        if (queue != null) {
            queue.clear();
            notifyDataSetChanged();
        }
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvArtist;
        ImageView ivAlbumArt;
        ImageButton btnRemove;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            ivAlbumArt = itemView.findViewById(R.id.ivAlbumArt);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}
