package com.example.musebox.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musebox.adapters.PlaylistAdapter;

import com.example.musebox.R;
import com.example.musebox.database.PlaylistDatabaseHelper;
import com.example.musebox.models.Playlist;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class PlaylistFragment extends Fragment {
    private RecyclerView recyclerView;
    private PlaylistAdapter adapter;
    private PlaylistDatabaseHelper dbHelper;
    private FloatingActionButton fabCreate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        recyclerView = view.findViewById(R.id.recyclerPlaylists);
        fabCreate = view.findViewById(R.id.fabCreatePlaylist);
        dbHelper = new PlaylistDatabaseHelper(requireContext());

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    adapter = new PlaylistAdapter();
    recyclerView.setAdapter(adapter);

        loadPlaylists();

        fabCreate.setOnClickListener(v -> showCreatePlaylistDialog());

        return view;
    }

    private void loadPlaylists() {
        List<Playlist> playlists = dbHelper.getAllPlaylists();
        adapter.setPlaylists(playlists);
    }

    private void showCreatePlaylistDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_playlist, null);
        final android.widget.EditText etName = dialogView.findViewById(R.id.etPlaylistName);
        final android.widget.EditText etDesc = dialogView.findViewById(R.id.etPlaylistDescription);

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Create Playlist");
        builder.setView(dialogView);
        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Playlist name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            dbHelper.createPlaylist(name, desc);
            loadPlaylists();
            Toast.makeText(requireContext(), "Playlist created!", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // ...existing code...
}
