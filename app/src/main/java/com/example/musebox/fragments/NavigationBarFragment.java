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
import android.widget.ImageView;
import android.widget.TextView;

import com.example.musebox.R;

public class NavigationBarFragment extends Fragment {

    public interface OnNavigationItemSelectedListener {
        void onNavigationItemSelected(String item);

        void onCreatePlaylistSelected();

        void onImportMusicSelected(Uri folderUri);

        void onUpdateAlbumArtSelected();
    }

    private OnNavigationItemSelectedListener listener;
    private LinearLayout navHome, navSearch, navCreate, navPlaylist, navProfile;
    private String currentSelection = "home";

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

        navHome = view.findViewById(R.id.nav_home);
        navSearch = view.findViewById(R.id.nav_search);
        navCreate = view.findViewById(R.id.nav_create);
        navPlaylist = view.findViewById(R.id.nav_playlist);
        navProfile = view.findViewById(R.id.nav_profile);

        navHome.setOnClickListener(v -> {
            setSelected("home");
            if (listener != null)
                listener.onNavigationItemSelected("home");
        });
        navSearch.setOnClickListener(v -> {
            setSelected("search");
            if (listener != null)
                listener.onNavigationItemSelected("search");
        });
        navPlaylist.setOnClickListener(v -> {
            setSelected("playlist");
            if (listener != null)
                listener.onNavigationItemSelected("playlist");
        });
        navProfile.setOnClickListener(v -> {
            setSelected("profile");
            if (listener != null)
                listener.onNavigationItemSelected("profile");
        });
        navCreate.setOnClickListener(v -> showCreateDialog());

        // Set initial selection
        setSelected(currentSelection);

        return view;
    }

    private void setSelected(String item) {
        currentSelection = item;

        // Reset all selections
        setNavItemSelected(navHome, false);
        setNavItemSelected(navSearch, false);
        setNavItemSelected(navCreate, false);
        setNavItemSelected(navPlaylist, false);
        setNavItemSelected(navProfile, false);

        // Set current selection
        switch (item) {
            case "home":
                setNavItemSelected(navHome, true);
                break;
            case "search":
                setNavItemSelected(navSearch, true);
                break;
            case "playlist":
                setNavItemSelected(navPlaylist, true);
                break;
            case "profile":
                setNavItemSelected(navProfile, true);
                break;
        }
    }

    private void setNavItemSelected(LinearLayout navItem, boolean selected) {
        if (navItem == null)
            return;

        // Get the ImageView and TextView children
        for (int i = 0; i < navItem.getChildCount(); i++) {
            View child = navItem.getChildAt(i);
            if (child instanceof ImageView || child instanceof TextView) {
                child.setSelected(selected);
            }
        }
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
