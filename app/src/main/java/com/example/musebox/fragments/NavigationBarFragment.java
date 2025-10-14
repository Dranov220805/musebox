package com.example.musebox.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.example.musebox.fragments.HomeFragment;

import com.example.musebox.R;

public class NavigationBarFragment extends Fragment {

    public interface OnNavigationItemSelectedListener {
        void onNavigationItemSelected(String item);

        void onCreatePlaylistSelected();

        void onImportMusicSelected(Uri folderUri);

        void onUpdateAlbumArtSelected();
    }

    private OnNavigationItemSelectedListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnNavigationItemSelectedListener) {
            listener = (OnNavigationItemSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnNavigationItemSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_navigation_bar, container, false);

        LinearLayout navHome = view.findViewById(R.id.nav_home);
        LinearLayout navSearch = view.findViewById(R.id.nav_search);
        LinearLayout navCreate = view.findViewById(R.id.nav_create);
        LinearLayout navPlaylist = view.findViewById(R.id.nav_playlist);
        LinearLayout navProfile = view.findViewById(R.id.nav_profile);

        navHome.setOnClickListener(v -> {
            if (listener != null)
                listener.onNavigationItemSelected("home");
        });
        navSearch.setOnClickListener(v -> {
            if (listener != null)
                listener.onNavigationItemSelected("search");
        });
        navPlaylist.setOnClickListener(v -> {
            if (listener != null)
                listener.onNavigationItemSelected("playlist");
        });
        navProfile.setOnClickListener(v -> {
            if (listener != null)
                listener.onNavigationItemSelected("profile");
        });
        navCreate.setOnClickListener(v -> showCreateDialog());

        return view;
    }

    private void showCreateDialog() {
        if (getContext() == null)
            return;

        // Inflate custom dialog layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_options, null);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        // Make dialog background transparent for rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Setup click listeners for the cards
        dialogView.findViewById(R.id.btn_create_playlist).setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null)
                listener.onCreatePlaylistSelected();
        });

        dialogView.findViewById(R.id.btn_import_music).setOnClickListener(v -> {
            dialog.dismiss();
            // Directly call import with null URI since MediaStore scans all device music
            if (listener != null)
                listener.onImportMusicSelected(null);
        });

        dialogView.findViewById(R.id.btn_update_album_art).setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null)
                listener.onUpdateAlbumArtSelected();
        });

        dialog.show();
    }
}
