package com.example.musebox.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import com.example.musebox.R;
import com.example.musebox.fragments.*;

public class HomeActivity extends AppCompatActivity
        implements NavigationBarFragment.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_container, new HomeFragment())
                    .commit();

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.navigation_container, new NavigationBarFragment())
                    .commit();
        }
    }

    @Override
    public void onNavigationItemSelected(String item) {
        Fragment selected = null;

        switch (item) {
            case "home":
                selected = new HomeFragment();
                break;
            case "search":
                selected = new SearchFragment();
                break;
            case "playlist":
                selected = new PlaylistFragment();
                break;
            case "profile":
                selected = new ProfileFragment();
                break;
        }

        if (selected != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_container, selected)
                    .commit();
        }
    }
}