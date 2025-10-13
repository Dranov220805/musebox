package com.example.musebox.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.adapters.SongAdapter;
import com.example.musebox.database.PlaylistDatabaseHelper;
import com.example.musebox.models.Song;

import java.util.List;

public class PlaylistDetailFragment extends Fragment {

    private static final String ARG_PLAYLIST_ID = "playlist_id";
    private static final String ARG_PLAYLIST_NAME = "playlist_name";

    private int playlistId;
    private String playlistName;

    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private PlaylistDatabaseHelper dbHelper;

    public static PlaylistDetailFragment newInstance(String playlistId, String playlistName) {
        PlaylistDetailFragment fragment = new PlaylistDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PLAYLIST_ID, Integer.parseInt(playlistId));
        args.putString(ARG_PLAYLIST_NAME, playlistName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            playlistId = getArguments().getInt(ARG_PLAYLIST_ID);
            playlistName = getArguments().getString(ARG_PLAYLIST_NAME);
        }
        dbHelper = new PlaylistDatabaseHelper(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist_detail, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewSongs);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        songAdapter = new SongAdapter();
        recyclerView.setAdapter(songAdapter);

        loadPlaylistSongs();

        return view;
    }

    private void loadPlaylistSongs() {
        List<Song> songs = dbHelper.getSongsInPlaylist(String.valueOf(playlistId));
        songAdapter.setSongs(songs);
    }
}



