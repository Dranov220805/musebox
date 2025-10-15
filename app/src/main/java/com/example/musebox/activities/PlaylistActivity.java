package com.example.musebox.activities;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musebox.R;
import com.example.musebox.adapters.PlaylistAdapter;
import com.example.musebox.database.PlaylistDatabaseHelper;
import com.example.musebox.models.Playlist;
import java.util.List;

public class PlaylistActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private PlaylistAdapter adapter;
    private PlaylistDatabaseHelper dbHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        recyclerView = findViewById(R.id.recyclerPlaylists);
        dbHelper = new PlaylistDatabaseHelper(this);
        adapter = new PlaylistAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadPlaylists();
    }

    private void loadPlaylists() {
        List<Playlist> playlists = dbHelper.getAllPlaylists();
        adapter.setPlaylists(playlists);
    }
}
