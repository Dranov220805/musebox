package com.example.musebox.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.adapters.SongAdapter;
import com.example.musebox.database.PlaylistDatabaseHelper;
import com.example.musebox.models.Playlist;
import com.example.musebox.models.Song;

import java.util.List;

public class PlaylistDetailActivity extends AppCompatActivity {
    private TextView tvTitle, tvDescription;
    private RecyclerView recyclerSongs;
    private SongAdapter songAdapter;
    private PlaylistDatabaseHelper dbHelper;
    private ImageButton btnAddSong;
    private String playlistId;
    private Playlist playlist;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        tvTitle = findViewById(R.id.tvPlaylistTitle);
        tvDescription = findViewById(R.id.tvPlaylistDescription);
        recyclerSongs = findViewById(R.id.recyclerPlaylistSongs);
        btnAddSong = findViewById(R.id.btnAddSong);
        dbHelper = new PlaylistDatabaseHelper(this);

        playlistId = getIntent().getStringExtra("playlist_id");
        if (playlistId == null) {
            Toast.makeText(this, "Playlist not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadPlaylist();

        btnAddSong.setOnClickListener(v -> showAddSongDialog());
    }

    private void loadPlaylist() {
        playlist = dbHelper.getPlaylistWithSongs(playlistId);
        if (playlist == null) {
            Toast.makeText(this, "Playlist not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        tvTitle.setText(playlist.getName());
        tvDescription.setText(playlist.getDescription());
        songAdapter = new SongAdapter(playlist.getSongs(), new SongAdapter.SongClickListener() {
            @Override
            public void onSongClick(Song song) {
                showRemoveSongDialog(song);
            }
        });
        recyclerSongs.setLayoutManager(new LinearLayoutManager(this));
        recyclerSongs.setAdapter(songAdapter);
    }

    private void showAddSongDialog() {
        // TODO: Implement dialog to select and add songs to playlist
        Toast.makeText(this, "Add song dialog coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void showRemoveSongDialog(Song song) {
        // TODO: Implement dialog to confirm and remove song from playlist
        Toast.makeText(this, "Remove song dialog coming soon!", Toast.LENGTH_SHORT).show();
    }
}
