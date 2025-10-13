package com.example.musebox.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.adapters.QueueAdapter;
import com.example.musebox.models.Song;
import com.example.musebox.services.MusicService;

import java.util.ArrayList;
import java.util.List;

public class QueueFragment extends Fragment {

    private RecyclerView recyclerQueue;
    private LinearLayout emptyQueue;
    private TextView tvCurrentTitle, tvCurrentArtist;
    private ImageButton btnBack, btnClearQueue;
    private QueueAdapter adapter;

    private MusicService musicService;
    private boolean isBound = false;

    // Interface for communicating with parent activity
    public interface OnQueueFragmentListener {
        void onBackPressed();

        MusicService getMusicService();

        boolean isMusicServiceBound();
    }

    private OnQueueFragmentListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnQueueFragmentListener) {
            listener = (OnQueueFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnQueueFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_queue, container, false);

        // Initialize views
        recyclerQueue = view.findViewById(R.id.recyclerQueue);
        emptyQueue = view.findViewById(R.id.emptyQueue);
        tvCurrentTitle = view.findViewById(R.id.tvCurrentTitle);
        tvCurrentArtist = view.findViewById(R.id.tvCurrentArtist);
        btnBack = view.findViewById(R.id.btnBack);
        btnClearQueue = view.findViewById(R.id.btnClearQueue);

        // Setup RecyclerView
        recyclerQueue.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new QueueAdapter(new ArrayList<>());
        recyclerQueue.setAdapter(adapter);

        // Set queue item listener
        adapter.setOnQueueItemListener(position -> {
            if (musicService != null) {
                // Calculate actual index in playlist (current + 1 + position)
                int actualIndex = musicService.getCurrentSongIndex() + 1 + position;
                if (musicService.removeFromQueue(actualIndex)) {
                    loadQueue();
                }
            }
        });

        // Back button
        btnBack.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBackPressed();
            }
        });

        // Clear queue button
        btnClearQueue.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.clearQueue();
                loadQueue();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Get music service from parent activity
        if (listener != null && listener.isMusicServiceBound()) {
            musicService = listener.getMusicService();
            isBound = true;
            loadQueue();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        musicService = null;
        isBound = false;
    }

    private void loadQueue() {
        if (musicService == null)
            return;

        // Get current playlist
        List<Song> playlist = musicService.getPlaylist();
        int currentIndex = musicService.getCurrentSongIndex();
        Song currentSong = musicService.getCurrentSong();

        // Update currently playing card
        if (currentSong != null) {
            tvCurrentTitle.setText(currentSong.getTitle());
            tvCurrentArtist.setText(currentSong.getArtist());
        } else {
            tvCurrentTitle.setText("No song playing");
            tvCurrentArtist.setText("Unknown Artist");
        }

        // Get queue (all songs after current)
        List<Song> queue = new ArrayList<>();
        if (playlist != null && !playlist.isEmpty() && currentIndex >= 0 && currentIndex < playlist.size() - 1) {
            for (int i = currentIndex + 1; i < playlist.size(); i++) {
                queue.add(playlist.get(i));
            }
        }

        // Always update the adapter first
        adapter.setQueue(queue);

        // Then update visibility based on the queue
        if (queue.isEmpty()) {
            recyclerQueue.setVisibility(View.GONE);
            emptyQueue.setVisibility(View.VISIBLE);
        } else {
            recyclerQueue.setVisibility(View.VISIBLE);
            emptyQueue.setVisibility(View.GONE);
        }
    }

    // Public method to refresh queue from parent activity
    public void refreshQueue() {
        if (listener != null && listener.isMusicServiceBound()) {
            musicService = listener.getMusicService();
            loadQueue();
        }
    }
}