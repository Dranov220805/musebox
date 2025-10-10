package com.example.musebox.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import com.example.musebox.R;
import com.example.musebox.fragments.*;

public class HomeActivity extends AppCompatActivity
        implements NavigationBarFragment.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        applySystemBarBehavior();

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
    protected void onResume() {
        super.onResume();
        applySystemBarBehavior();
    }

    /**
     * Adjust system bar visibility depending on orientation and user settings.
     */
    private void applySystemBarBehavior() {
        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        boolean navBarHiddenBySystem = isNavigationBarHiddenBySystem();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Always let system insets adjust layout automatically
            getWindow().setDecorFitsSystemWindows(true);

            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller == null) return;

            if (isLandscape) {
                // fully immersive (hide both)
                getWindow().setDecorFitsSystemWindows(false); // prevent content being under bars
                controller.hide(WindowInsets.Type.navigationBars() | WindowInsets.Type.statusBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            } else {
                // status bar visible, nav bar depends on system setting
                getWindow().setDecorFitsSystemWindows(true);
                controller.show(WindowInsets.Type.statusBars());

                if (navBarHiddenBySystem) {
                    getWindow().setDecorFitsSystemWindows(false);
                    controller.hide(WindowInsets.Type.navigationBars());
                } else {
                    controller.show(WindowInsets.Type.navigationBars());
                }
            }
        } else {
            View decorView = getWindow().getDecorView();
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

            if (isLandscape) {
                // Always hide both bars
                flags |= View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            } else {
                // Portrait
                if (navBarHiddenBySystem) {
                    flags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
                }
            }

            decorView.setSystemUiVisibility(flags);
        }
    }

    /**
     * Detect if navigation bar is hidden by system setting.
     */
    private boolean isNavigationBarHiddenBySystem() {
        try {
            int value = Settings.Global.getInt(getContentResolver(), "navigationbar_is_min", 0);
            return value == 1;
        } catch (Exception e) {
            return false;
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