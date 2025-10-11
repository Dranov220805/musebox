package com.example.musebox.fragments;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.adapters.SongAdapter;
import com.example.musebox.database.SongDatabaseHelper;
import com.example.musebox.models.Song;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerSongs;
    private LinearLayout emptyView;
    private Button btnImport;
    private SongAdapter adapter;
    private SongDatabaseHelper dbHelper;

    private static final int REQUEST_PERMISSION = 200;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerSongs = view.findViewById(R.id.recyclerSongs);
        emptyView = view.findViewById(R.id.emptyView);
        btnImport = view.findViewById(R.id.btnImport);

        dbHelper = new SongDatabaseHelper(requireContext());

        recyclerSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SongAdapter(new ArrayList<>());
        recyclerSongs.setAdapter(adapter);

        btnImport.setOnClickListener(v -> checkPermissionAndScan());

        loadSongsFromDatabase();
        return view;
    }

    private void checkPermissionAndScan() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        } else {
            scanDeviceForAudioFiles();
        }
    }

    private void scanDeviceForAudioFiles() {
        Toast.makeText(requireContext(), "Scanning for songs...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            ContentResolver resolver = requireContext().getContentResolver();
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

            String[] projection = {
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DURATION
            };

            Cursor cursor = resolver.query(uri, projection, null, null, null);
            if (cursor == null) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "No music found!", Toast.LENGTH_SHORT).show());
                return;
            }

            List<Song> importedSongs = new ArrayList<>();
            while (cursor.moveToNext()) {
                String title = cursor.getString(0);
                String artist = cursor.getString(1);
                String path = cursor.getString(2);
                int duration = cursor.getInt(3);

                // Skip non-existent files
                if (path == null) continue;

                Song song = new Song(title, artist, path, duration);
                dbHelper.addSong(song);
                importedSongs.add(song);
            }
            cursor.close();

            requireActivity().runOnUiThread(() -> {
                if (importedSongs.isEmpty()) {
                    Toast.makeText(requireContext(), "No songs found on device", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(),
                            "Imported " + importedSongs.size() + " songs from device!",
                            Toast.LENGTH_LONG).show();
                }
                loadSongsFromDatabase();
            });
        }).start();
    }

    private void loadSongsFromDatabase() {
        List<Song> songs = dbHelper.getAllSongs();
        if (songs.isEmpty()) {
            recyclerSongs.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerSongs.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            adapter.setSongs(songs);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanDeviceForAudioFiles();
            } else {
                Toast.makeText(requireContext(),
                        "Permission denied! Cannot scan for songs.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}