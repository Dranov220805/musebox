package com.example.musebox.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.models.Song;

import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    private List<Song> queue;

    public QueueAdapter(List<Song> queue) {
        this.queue = queue;
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

        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                queue.remove(pos);
                notifyItemRemoved(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return queue != null ? queue.size() : 0;
    }

    public void setQueue(List<Song> newQueue) {
        if (queue != null) {
            queue.clear();
            if (newQueue != null) {
                queue.addAll(newQueue);
            }
        } else {
            queue = newQueue;
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
        ImageButton btnRemove;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}
