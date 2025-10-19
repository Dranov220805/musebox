package com.example.musebox.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.musebox.R;
import com.example.musebox.activities.LoginActivity;
import com.example.musebox.database.SongDatabaseHelper;
import com.example.musebox.utils.SessionManager;
import com.example.musebox.utils.ThemedDialogUtils;
import com.google.android.material.card.MaterialCardView;

public class ProfileFragment extends Fragment {

    private ImageView ivAvatar;
    private TextView tvUsername, tvEmail, tvCurrentTheme;
    private MaterialCardView cardUploadAvatar, cardTheme, cardHomeWidget, cardLanguage, 
                            cardMusicFolder, cardClearCache, cardClearData, cardLogout;
    
    private SessionManager sessionManager;
    private SongDatabaseHelper dbHelper;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize session manager
        sessionManager = new SessionManager(requireContext());
        
        // Initialize database helper
        dbHelper = new SongDatabaseHelper(requireContext());
        
        // Initialize image picker launcher
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        ivAvatar.setImageURI(imageUri);
                        // Here you could save the image URI to preferences or upload to server
                        Toast.makeText(getContext(), "Avatar updated successfully!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
        
        // Initialize permission launcher
        permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openImagePicker();
                } else {
                    Toast.makeText(getContext(), "Permission denied. Cannot access gallery.", Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        loadUserData();
        setupClickListeners();
    }

    private void initViews(View view) {
        ivAvatar = view.findViewById(R.id.ivAvatar);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvCurrentTheme = view.findViewById(R.id.tvCurrentTheme);
        
        cardUploadAvatar = view.findViewById(R.id.cardUploadAvatar);
        cardTheme = view.findViewById(R.id.cardTheme);
        cardHomeWidget = view.findViewById(R.id.cardHomeWidget);
        cardLanguage = view.findViewById(R.id.cardLanguage);
        cardMusicFolder = view.findViewById(R.id.cardMusicFolder);
        cardClearCache = view.findViewById(R.id.cardClearCache);
        cardClearData = view.findViewById(R.id.cardClearData);
        cardLogout = view.findViewById(R.id.cardLogout);
    }

    private void loadUserData() {
        if (sessionManager.isLoggedIn()) {
            String username = sessionManager.getUserName();
            String email = sessionManager.getUserEmail();
            
            tvUsername.setText(username != null ? username : "Unknown User");
            tvEmail.setText(email != null ? email : "No email");
        } else {
            tvUsername.setText("Guest User");
            tvEmail.setText("Not logged in");
        }
    }

    private void setupClickListeners() {
        cardUploadAvatar.setOnClickListener(v -> handleUploadAvatar());
        cardTheme.setOnClickListener(v -> handleThemeSelection());
        cardHomeWidget.setOnClickListener(v -> handleFutureFeature("Home Screen Widget"));
        cardLanguage.setOnClickListener(v -> handleFutureFeature("App Language"));
        cardMusicFolder.setOnClickListener(v -> handleFutureFeature("Local Music Search Folder"));
        cardClearCache.setOnClickListener(v -> handleClearCache());
        cardClearData.setOnClickListener(v -> handleClearData());
        cardLogout.setOnClickListener(v -> handleLogout());
    }

    private void handleUploadAvatar() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) 
                == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void handleThemeSelection() {
        String[] themeOptions = {"Light Mode", "Dark Mode", "Use System Setting"};
        int currentSelection = getCurrentThemeSelection();
        
        ThemedDialogUtils.showListDialog(
            requireContext(),
            "Select Theme",
            themeOptions,
            currentSelection,
            R.drawable.ic_palette,
            R.color.musebox_green,
            (selectedIndex) -> {
                String selectedTheme = themeOptions[selectedIndex];
                tvCurrentTheme.setText(selectedTheme.toLowerCase());
                
                // Here you would implement theme changing logic
                // For example: ThemeManager.setTheme(selectedIndex);
                Toast.makeText(getContext(), "Theme set to: " + selectedTheme, Toast.LENGTH_SHORT).show();
            }
        );
    }

    private int getCurrentThemeSelection() {
        // This would check your current theme preference
        // For now, return 2 (Use System Setting) as default
        return 2;
    }

    private void handleFutureFeature(String featureName) {
        ThemedDialogUtils.showSimpleDialog(
            requireContext(),
            "Coming Soon",
            featureName + " will be available in a future update. Stay tuned!",
            R.drawable.ic_info,
            R.color.musebox_green,
            "OK",
            null,
            new ThemedDialogUtils.OnDialogClickListener() {
                @Override
                public void onPositiveClick() {
                    // Dialog will close automatically
                }
            }
        );
    }

    private void handleClearCache() {
        ThemedDialogUtils.showSimpleDialog(
            requireContext(),
            "Clear Cache",
            "This will clear temporary files and cached data. Your music library will not be affected.",
            R.drawable.ic_clear_cache,
            R.color.musebox_green,
            "Clear",
            "Cancel",
            new ThemedDialogUtils.OnDialogClickListener() {
                @Override
                public void onPositiveClick() {
                    performClearCache();
                }
            }
        );
    }

    private void performClearCache() {
        try {
            // Clear Glide cache
            if (getContext() != null) {
                new Thread(() -> {
                    try {
                        Glide.get(getContext()).clearDiskCache();
                    } catch (Exception e) {
                        android.util.Log.w("ProfileFragment", "Failed to clear Glide disk cache: " + e.getMessage());
                    }
                }).start();
                
                // Clear memory cache on main thread
                Glide.get(getContext()).clearMemory();
            }
            
            // Clear app cache directory
            clearAppCache();
            
            Toast.makeText(getContext(), "Cache cleared successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("ProfileFragment", "Failed to clear cache: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Failed to clear cache: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleClearData() {
        ThemedDialogUtils.showSimpleDialog(
            requireContext(),
            "Clear All Data",
            "This will remove all songs from your library. This action cannot be undone. Are you sure?",
            R.drawable.ic_delete_sweep,
            android.R.color.holo_red_dark,
            "Clear All",
            "Cancel",
            new ThemedDialogUtils.OnDialogClickListener() {
                @Override
                public void onPositiveClick() {
                    performClearData();
                }
            }
        );
    }

    private void performClearData() {
        // Show progress indicator
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getContext());
        progressDialog.setMessage("Clearing all data...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Perform clearing in background thread
        new Thread(() -> {
            try {
                // Clear all songs from the database
                dbHelper.deleteAllSongs();
                
                // Also clear favorites table manually since deleteAllSongs() doesn't clear it
                clearAllFavorites();
                
                // Also clear Glide cache to remove cached album art
                if (getContext() != null) {
                    try {
                        Glide.get(getContext()).clearDiskCache();
                    } catch (Exception e) {
                        android.util.Log.w("ProfileFragment", "Failed to clear Glide disk cache: " + e.getMessage());
                    }
                }
                
                // Clear app cache directory as well
                clearAppCache();
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        
                        // Clear memory cache on main thread
                        if (getContext() != null) {
                            Glide.get(getContext()).clearMemory();
                        }
                        
                        Toast.makeText(getContext(), "All music library data cleared successfully!", Toast.LENGTH_SHORT).show();
                        
                        // Refresh the home fragment if it exists
                        refreshHomeFragment();
                    });
                }
                
            } catch (Exception e) {
                android.util.Log.e("ProfileFragment", "Failed to clear data: " + e.getMessage(), e);
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Failed to clear data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }
    
    private void clearAllFavorites() {
        try {
            // Clear all favorites from the database
            android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("favorites", null, null);
        } catch (Exception e) {
            android.util.Log.w("ProfileFragment", "Failed to clear favorites: " + e.getMessage());
        }
    }
    
    private void clearAppCache() {
        try {
            java.io.File cacheDir = requireContext().getCacheDir();
            if (cacheDir != null && cacheDir.isDirectory()) {
                deleteDirectory(cacheDir);
            }
        } catch (Exception e) {
            android.util.Log.w("ProfileFragment", "Failed to clear app cache: " + e.getMessage());
        }
    }
    
    private boolean deleteDirectory(java.io.File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDirectory(new java.io.File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        }
        return false;
    }
    
    private void refreshHomeFragment() {
        try {
            if (getActivity() instanceof com.example.musebox.activities.HomeActivity) {
                // Try to refresh the home fragment
                androidx.fragment.app.Fragment homeFragment = getActivity().getSupportFragmentManager()
                    .findFragmentByTag("HOME_FRAGMENT");
                
                if (homeFragment instanceof com.example.musebox.fragments.HomeFragment) {
                    ((com.example.musebox.fragments.HomeFragment) homeFragment).refreshSongs();
                }
            }
        } catch (Exception e) {
            android.util.Log.w("ProfileFragment", "Failed to refresh home fragment: " + e.getMessage());
        }
    }

    private void handleLogout() {
        ThemedDialogUtils.showSimpleDialog(
            requireContext(),
            "Log Out",
            "Are you sure you want to log out? You'll need to sign in again to access your account.",
            R.drawable.ic_logout,
            android.R.color.holo_red_dark,
            "Log Out",
            "Cancel",
            new ThemedDialogUtils.OnDialogClickListener() {
                @Override
                public void onPositiveClick() {
                    performLogout();
                }
            }
        );
    }

    private void performLogout() {
        // Clear session
        sessionManager.logout();
        
        // Navigate to login activity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        
        if (getActivity() != null) {
            getActivity().finish();
        }
        
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
    }
}