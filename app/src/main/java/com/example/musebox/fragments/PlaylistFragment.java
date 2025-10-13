package com.example.musebox.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.adapters.PlaylistAdapter;
import com.example.musebox.database.PlaylistDatabaseHelper;
import com.example.musebox.models.Playlist;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class PlaylistFragment extends Fragment implements PlaylistAdapter.OnPlaylistClickListener {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private PlaylistAdapter playlistAdapter;
    private PlaylistDatabaseHelper dbHelper;
    private List<Playlist> playlists;

    public PlaylistFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewPlaylists);
        fabAdd = view.findViewById(R.id.fabAddPlaylist);

        dbHelper = new PlaylistDatabaseHelper(requireContext());
        playlists = new ArrayList<>(dbHelper.getAllPlaylists());

        playlistAdapter = new PlaylistAdapter(requireContext(), playlists, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(playlistAdapter);

        fabAdd.setOnClickListener(v -> showCreatePlaylistDialog());

        if (playlists.isEmpty()) {
            Toast.makeText(getContext(), "No playlists yet. Create one!", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    // === CREATE PLAYLIST ===
    private void showCreatePlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("New Playlist");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter playlist name");
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            Playlist playlist = new Playlist(name);
            if (dbHelper.createPlaylist(playlist)) {
                playlists.add(0, playlist);
                playlistAdapter.notifyItemInserted(0);
                recyclerView.scrollToPosition(0);
                Toast.makeText(getContext(), "Playlist created", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Playlist name already exists", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // === PLAYLIST CLICK HANDLERS ===
    @Override
    public void onPlaylistClick(Playlist playlist) {
        Fragment detailFragment = PlaylistDetailFragment.newInstance(playlist.getId(), playlist.getName());
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onRenamePlaylist(Playlist playlist) {
        showRenameDialog(playlist);
    }

    @Override
    public void onDeletePlaylist(Playlist playlist) {
        if (dbHelper.deletePlaylist(playlist.getId())) {
            int pos = playlists.indexOf(playlist);
            playlists.remove(playlist);
            playlistAdapter.notifyItemRemoved(pos);
            Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
        }
    }

    // === RENAME DIALOG ===
    private void showRenameDialog(Playlist playlist) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Rename Playlist");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(playlist.getName());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newName.equals(playlist.getName())) {
                if (dbHelper.renamePlaylist(playlist.getId(), newName)) {
                    playlist.setName(newName);
                    playlistAdapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Renamed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Playlist name already exists", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}

