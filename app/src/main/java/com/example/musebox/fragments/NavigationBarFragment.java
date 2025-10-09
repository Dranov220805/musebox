package com.example.musebox.fragments;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.example.musebox.R;

public class NavigationBarFragment extends Fragment {

    // === Define interface for communication with Activity ===
    public interface OnNavigationItemSelectedListener {
        void onNavigationItemSelected(String item);
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
        LinearLayout navPlaylist = view.findViewById(R.id.nav_playlist);
        LinearLayout navProfile = view.findViewById(R.id.nav_profile);

        // === Setup click listeners to notify HomeActivity ===
        navHome.setOnClickListener(v -> {
            if (listener != null) listener.onNavigationItemSelected("home");
        });

        navSearch.setOnClickListener(v -> {
            if (listener != null) listener.onNavigationItemSelected("search");
        });

        navPlaylist.setOnClickListener(v -> {
            if (listener != null) listener.onNavigationItemSelected("playlist");
        });

        navProfile.setOnClickListener(v -> {
            if (listener != null) listener.onNavigationItemSelected("profile");
        });

        return view;
    }
}