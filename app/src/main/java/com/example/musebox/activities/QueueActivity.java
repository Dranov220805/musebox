package com.example.musebox.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.adapters.QueueAdapter;
import com.example.musebox.models.Song;
import com.example.musebox.services.MusicService;

import java.util.ArrayList;
import java.util.List;

public class QueueActivity extends AppCompatActivity {

    private RecyclerView recyclerQueue;
    private LinearLayout emptyQueue;
    private TextView tvCurrentTitle, tvCurrentArtist;
    private ImageButton btnBack, btnClearQueue;
    private QueueAdapter adapter;
    
    private MusicService musicService;
    private boolean isBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            isBound = true;
            loadQueue();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);

        // Initialize views
        recyclerQueue = findViewById(R.id.recyclerQueue);
        emptyQueue = findViewById(R.id.emptyQueue);
        tvCurrentTitle = findViewById(R.id.tvCurrentTitle);
        tvCurrentArtist = findViewById(R.id.tvCurrentArtist);
        btnBack = findViewById(R.id.btnBack);
        btnClearQueue = findViewById(R.id.btnClearQueue);

        // Setup RecyclerView
        recyclerQueue.setLayoutManager(new LinearLayoutManager(this));
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
        btnBack.setOnClickListener(v -> finish());

        // Clear queue button
        btnClearQueue.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.clearQueue();
                loadQueue();
            }
        });

        // Bind to MusicService
        Intent serviceIntent = new Intent(this, MusicService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void loadQueue() {
        if (musicService == null) return;
        
        // Get current playlist
        List<Song> playlist = musicService.getPlaylist();
        int currentIndex = musicService.getCurrentSongIndex();
        Song currentSong = musicService.getCurrentSong();
        
        if (currentSong != null) {
            // Update currently playing card
            tvCurrentTitle.setText(currentSong.getTitle());
            tvCurrentArtist.setText(currentSong.getArtist());
        }
        
        // Get queue (all songs after current)
        List<Song> queue = new ArrayList<>();
        if (currentIndex >= 0 && currentIndex < playlist.size() - 1) {
            for (int i = currentIndex + 1; i < playlist.size(); i++) {
                queue.add(playlist.get(i));
            }
        }
        
        if (queue.isEmpty()) {
            updateEmptyState();
        } else {
            adapter.setQueue(queue);
            recyclerQueue.setVisibility(View.VISIBLE);
            emptyQueue.setVisibility(View.GONE);
        }
    }

    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            recyclerQueue.setVisibility(View.GONE);
            emptyQueue.setVisibility(View.VISIBLE);
        } else {
            recyclerQueue.setVisibility(View.VISIBLE);
            emptyQueue.setVisibility(View.GONE);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}
