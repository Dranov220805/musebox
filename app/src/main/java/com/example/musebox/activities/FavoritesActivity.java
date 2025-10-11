package com.example.musebox.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.adapters.SongAdapter;
import com.example.musebox.database.SongDatabaseHelper;
import com.example.musebox.models.Song;
import com.example.musebox.services.MusicService;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private TextView tvEmptyMessage;
    private SongAdapter adapter;
    private SongDatabaseHelper dbHelper;
    private List<Song> favoriteSongs = new ArrayList<>();

    private MusicService musicService;
    private boolean serviceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Favorite Songs");
        }

        recyclerView = findViewById(R.id.recyclerFavorites);
        emptyView = findViewById(R.id.emptyView);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);

        dbHelper = new SongDatabaseHelper(this);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter();
        adapter.setOnSongClickListener(this::onSongClicked);
        adapter.setMenuActionOverride((song, position) -> {
            // Override menu to show "Remove from Favorites" instead of "Delete Song"
            removeFromFavorites(song, position);
        });
        recyclerView.setAdapter(adapter);

        // Bind to MusicService
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        loadFavorites();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadFavorites() {
        new Thread(() -> {
            List<Song> songs = dbHelper.getFavoriteSongs();
            runOnUiThread(() -> {
                favoriteSongs.clear();
                favoriteSongs.addAll(songs);
                adapter.notifyDataSetChanged();

                if (favoriteSongs.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                    tvEmptyMessage.setText("No favorite songs yet");
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void onSongClicked(Song song) {
        if (serviceBound && !favoriteSongs.isEmpty()) {
            int position = favoriteSongs.indexOf(song);
            musicService.setPlaylist(favoriteSongs, position);

            Song currentSong = favoriteSongs.get(position);
            musicService.playSong(
                    android.net.Uri.parse(currentSong.getUri()),
                    currentSong.getTitle(),
                    currentSong.getArtist());

            // Open FullPlayerActivity
            Intent intent = new Intent(this, FullPlayerActivity.class);
            startActivity(intent);
        }
    }

    private void removeFromFavorites(Song song, int position) {
        new Thread(() -> {
            boolean success = dbHelper.removeFromFavorites(song.getId());
            runOnUiThread(() -> {
                if (success) {
                    favoriteSongs.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();

                    if (favoriteSongs.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                        tvEmptyMessage.setText("No favorite songs yet");
                    }
                }
            });
        }).start();
    }
}
