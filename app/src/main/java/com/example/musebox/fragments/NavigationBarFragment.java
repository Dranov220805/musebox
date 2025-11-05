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
import android.widget.ImageView;
import android.widget.TextView;

import com.example.musebox.R;

public class NavigationBarFragment extends Fragment {

    public interface OnNavigationItemSelectedListener {
        void onNavigationItemSelected(String item);
    }

    private OnNavigationItemSelectedListener listener;
    private LinearLayout navHome, navCreate, navExplore, navLibrary, navOffline;
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
        navCreate = view.findViewById(R.id.nav_create);
        navExplore = view.findViewById(R.id.nav_explore);
        navLibrary = view.findViewById(R.id.nav_library);
        navOffline = view.findViewById(R.id.nav_offline);

        navHome.setOnClickListener(v -> {
            setSelected("home");
            if (listener != null)
                listener.onNavigationItemSelected("home");
        });

        navCreate.setOnClickListener(v -> {
            setSelected("create");
            if (listener != null)
                listener.onNavigationItemSelected("create");
        });

        navExplore.setOnClickListener(v -> {
            setSelected("explore");
            if (listener != null)
                listener.onNavigationItemSelected("explore");
        });

        navLibrary.setOnClickListener(v -> {
            setSelected("library");
            if (listener != null)
                listener.onNavigationItemSelected("library");
        });

        navOffline.setOnClickListener(v -> {
            setSelected("offline");
            if (listener != null)
                listener.onNavigationItemSelected("offline");
        });

        // Set initial selection
        setSelected(currentSelection);

        return view;
    }

    public void setSelected(String item) {
        currentSelection = item;

        // Reset all selections
        setNavItemSelected(navHome, false);
        setNavItemSelected(navCreate, false);
        setNavItemSelected(navExplore, false);
        setNavItemSelected(navLibrary, false);
        setNavItemSelected(navOffline, false);

        // Set current selection
        switch (item) {
            case "home":
                setNavItemSelected(navHome, true);
                break;
            case "create":
                setNavItemSelected(navCreate, true);
                break;
            case "explore":
                setNavItemSelected(navExplore, true);
                break;
            case "library":
                setNavItemSelected(navLibrary, true);
                break;
            case "offline":
                setNavItemSelected(navOffline, true);
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
}
