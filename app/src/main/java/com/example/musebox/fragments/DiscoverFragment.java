package com.example.musebox.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.adapters.GenreSectionAdapter;
import com.example.musebox.api.JamendoApiService;
import com.example.musebox.database.SongDatabaseHelper;
import com.example.musebox.models.GenreSection;
import com.example.musebox.models.MusicRecommendation;
import com.example.musebox.models.Song;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DiscoverFragment extends Fragment {

    private ImageButton btnBack;
    private ImageButton btnSearch;
    private ImageButton btnProfile;
    private ImageButton btnNotifications;
    private ImageButton btnSearchBack;
    private ImageButton btnVoiceSearch;
    private LinearLayout searchBar;
    private LinearLayout searchBarContainer;
    private EditText etSearchInput;
    private HorizontalScrollView genreChipsScrollView;
    private ChipGroup chipGroupGenres;
    private TextView tvSectionTitle;
    private TextView tvPlayAll;
    private RecyclerView recyclerGenreSections;
    private ProgressBar progressBarLoading;
    private SongDatabaseHelper dbHelper;

    private GenreSectionAdapter genreSectionAdapter;
    private List<String> genres = Arrays.asList(
            "Podcasts", "Workout", "Energize", "Feel good", "Relax",
            "Commute", "Party", "Romance", "Focus", "Sad", "Sleep");

    private int loadedGenresCount = 0;
    private List<GenreSection> originalGenreSections = new ArrayList<>();
    private boolean isShowingSearchResults = false;
    private String selectedGenre = null;

    private ActivityResultLauncher<Intent> speechRecognizerLauncher;
    private ActivityResultLauncher<String> audioPermissionLauncher;

    // Interface for communicating with parent activity
    public interface OnDiscoverFragmentListener {
        void onBackPressed();

        void onSearchClicked();

        void onProfileClicked();

        void onMusicSelected(MusicRecommendation music, String genre);

        void onGenreSelected(String genre);
    }

    private OnDiscoverFragmentListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnDiscoverFragmentListener) {
            listener = (OnDiscoverFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnDiscoverFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize speech recognizer launcher
        speechRecognizerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            String spokenText = matches.get(0);
                            etSearchInput.setText(spokenText);
                            performSearch(spokenText);
                        }
                    }
                });

        // Initialize audio permission launcher
        audioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startVoiceRecognition();
                    } else {
                        Toast.makeText(getContext(), "Microphone permission required for voice search",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize database helper
        dbHelper = new SongDatabaseHelper(requireContext());

        // Initialize views
        btnBack = view.findViewById(R.id.btnBack);
        btnSearch = view.findViewById(R.id.btnSearch);
        btnProfile = view.findViewById(R.id.btnProfile);
        btnNotifications = view.findViewById(R.id.btnNotifications);
        btnSearchBack = view.findViewById(R.id.btnSearchBack);
        btnVoiceSearch = view.findViewById(R.id.btnVoiceSearch);
        searchBar = view.findViewById(R.id.searchBar);
        searchBarContainer = view.findViewById(R.id.searchBarContainer);
        etSearchInput = view.findViewById(R.id.etSearchInput);
        genreChipsScrollView = view.findViewById(R.id.genreChipsScrollView);
        chipGroupGenres = view.findViewById(R.id.chipGroupGenres);
        tvSectionTitle = view.findViewById(R.id.tvSectionTitle);
        tvPlayAll = view.findViewById(R.id.tvPlayAll);
        recyclerGenreSections = view.findViewById(R.id.recyclerGenreSections);
        progressBarLoading = view.findViewById(R.id.progressBarLoading);

        setupGenreChips();
        setupRecyclerView();
        setupClickListeners();
        setupSearchInput();
        loadGenreSections();

        return view;
    }

    private void setupRecyclerView() {
        genreSectionAdapter = new GenreSectionAdapter();
        recyclerGenreSections.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerGenreSections.setAdapter(genreSectionAdapter);

        // Set click listeners
        genreSectionAdapter.setOnMusicClickListener((music, genre) -> {
            showMusicDetail(music, genre);
        });

        genreSectionAdapter.setOnSeeAllClickListener(genre -> {
            if (listener != null) {
                listener.onGenreSelected(genre);
            } else {
                Toast.makeText(getContext(), "See all " + genre + " tracks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupGenreChips() {
        if (chipGroupGenres == null) {
            // ChipGroup not available in layout, skip setup
            return;
        }

        chipGroupGenres.removeAllViews();

        for (String genre : genres) {
            Chip chip = new Chip(requireContext());
            chip.setText(genre);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.spotify_gray);
            chip.setTextColor(getResources().getColor(R.color.white, null));
            chip.setCheckedIconVisible(false);

            chip.setOnClickListener(v -> {
                if (chip.isChecked()) {
                    selectedGenre = genre;
                    filterByGenre(genre);
                } else {
                    selectedGenre = null;
                    restoreOriginalSections();
                }
            });

            chipGroupGenres.addView(chip);
        }
    }

    private void setupSearchInput() {
        if (etSearchInput == null) {
            return;
        }

        // Handle Enter key press
        etSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String query = etSearchInput.getText().toString().trim();
                if (!query.isEmpty()) {
                    performSearch(query);
                    hideKeyboard();
                }
                return true;
            }
            return false;
        });
    }

    private void setupClickListeners() {
        // Notifications button
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Notifications", Toast.LENGTH_SHORT).show();
            });
        }

        // Back button (hidden but kept for compatibility)
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (isShowingSearchResults) {
                    // Restore original genre sections instead of navigating away
                    restoreOriginalSections();
                } else if (listener != null) {
                    listener.onBackPressed();
                } else {
                    requireActivity().onBackPressed();
                }
            });
        }

        // Search button
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                // Show search bar container
                showSearchBar();
            });
        }

        // Search back button
        if (btnSearchBack != null) {
            btnSearchBack.setOnClickListener(v -> {
                hideSearchBar();
            });
        }

        // Voice search button
        if (btnVoiceSearch != null) {
            btnVoiceSearch.setOnClickListener(v -> {
                checkAudioPermissionAndStartVoiceRecognition();
            });
        }

        // Profile button
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProfileClicked();
                } else {
                    Toast.makeText(getContext(), "Profile", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Search bar (hidden but kept for compatibility)
        if (searchBar != null) {
            searchBar.setOnClickListener(v -> {
                // Show search bar
                showSearchBar();
            });
        }
    }

    private void loadGenreSections() {
        progressBarLoading.setVisibility(View.VISIBLE);
        List<GenreSection> sections = new ArrayList<>();
        loadedGenresCount = 0;

        for (String genre : genres) {
            JamendoApiService.getTracksByGenre(genre, 10, new JamendoApiService.MusicRecommendationCallback() {
                @Override
                public void onSuccess(List<MusicRecommendation> recommendations) {
                    if (getActivity() != null && isAdded() && !recommendations.isEmpty()) {
                        sections.add(new GenreSection(genre, recommendations));
                        loadedGenresCount++;

                        // Update adapter when all genres are loaded or after each load
                        genreSectionAdapter.setGenreSections(new ArrayList<>(sections));

                        if (loadedGenresCount >= genres.size()) {
                            progressBarLoading.setVisibility(View.GONE);
                            // Store original sections once all genres are loaded
                            originalGenreSections = new ArrayList<>(sections);
                            isShowingSearchResults = false;
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    loadedGenresCount++;
                    if (loadedGenresCount >= genres.size()) {
                        if (getActivity() != null && isAdded()) {
                            progressBarLoading.setVisibility(View.GONE);
                        }
                    }
                }
            });
        }
    }

    private void showMusicDetail(MusicRecommendation music, String genre) {
        MusicDetailBottomSheet bottomSheet = MusicDetailBottomSheet.newInstance(music, genre);
        bottomSheet.setActionListener(new MusicDetailBottomSheet.OnMusicActionListener() {
            @Override
            public void onPlayMusic(MusicRecommendation music) {
                if (listener != null) {
                    listener.onMusicSelected(music, genre);
                }
                Toast.makeText(getContext(), "Playing: " + music.getTrackName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAddToFavorites(MusicRecommendation music) {
                Toast.makeText(getContext(), "Added to favorites", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAddToQueue(MusicRecommendation music) {
                Toast.makeText(getContext(), "Added to queue", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onGoToArtist(String artistName) {
                Toast.makeText(getContext(), "Go to artist: " + artistName, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMusicSelected(MusicRecommendation music) {
                // Recursive call to show detail for newly selected music
                showMusicDetail(music, genre);
            }
        });
        bottomSheet.show(getParentFragmentManager(), "MusicDetailBottomSheet");
    }

    // Old search dialog method - kept for backward compatibility but not used
    // Now using the inline search bar instead
    /*
     * private void showSearchDialog() {
     * android.widget.EditText input = new
     * android.widget.EditText(requireContext());
     * input.setHint("Enter artist or song name");
     * input.setPadding(50, 30, 50, 30);
     * 
     * new android.app.AlertDialog.Builder(requireContext())
     * .setTitle("Search Music")
     * .setMessage("Search for music (offline first, then online):")
     * .setView(input)
     * .setPositiveButton("Search", (dialog, which) -> {
     * String query = input.getText().toString().trim();
     * if (!query.isEmpty()) {
     * searchMusic(query);
     * } else {
     * Toast.makeText(requireContext(), "Please enter a search term",
     * Toast.LENGTH_SHORT).show();
     * }
     * })
     * .setNegativeButton("Cancel", null)
     * .show();
     * }
     */

    private void searchMusic(String query) {
        progressBarLoading.setVisibility(View.VISIBLE);

        // First, search offline songs
        List<Song> offlineSongs = dbHelper.searchSongs(query, "title");

        if (!offlineSongs.isEmpty()) {
            // Found offline results - convert to MusicRecommendation and display
            List<MusicRecommendation> recommendations = new ArrayList<>();
            for (Song song : offlineSongs) {
                // MusicRecommendation constructor: trackName, artistName, albumName, audioUrl,
                // imageUrl, duration, source
                MusicRecommendation rec = new MusicRecommendation(
                        song.getTitle(), // trackName
                        song.getArtist(), // artistName
                        "Local", // albumName
                        song.getUri(), // audioUrl
                        song.getAlbumCoverPath(), // imageUrl
                        (int) (song.getDuration() / 1000), // duration in seconds
                        "offline"); // source
                recommendations.add(rec);
            }

            if (getActivity() != null && isAdded()) {
                progressBarLoading.setVisibility(View.GONE);

                // Show offline results
                List<GenreSection> currentSections = new ArrayList<>();
                currentSections.add(new GenreSection("Offline: " + query, recommendations));
                genreSectionAdapter.setGenreSections(currentSections);
                recyclerGenreSections.smoothScrollToPosition(0);
                isShowingSearchResults = true;
                Toast.makeText(getContext(), "Found " + recommendations.size() + " offline results",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // No offline results - search online via Jamendo API
            searchOnline(query);
        }
    }

    private void searchOnline(String query) {
        JamendoApiService.searchTracks(query, 20, new JamendoApiService.MusicRecommendationCallback() {
            @Override
            public void onSuccess(List<MusicRecommendation> recommendations) {
                if (getActivity() != null && isAdded()) {
                    progressBarLoading.setVisibility(View.GONE);
                    if (recommendations.isEmpty()) {
                        Toast.makeText(getContext(), "No results found for '" + query + "'",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // Show online results
                        List<GenreSection> currentSections = new ArrayList<>();
                        currentSections.add(new GenreSection("Online: " + query, recommendations));
                        genreSectionAdapter.setGenreSections(currentSections);
                        recyclerGenreSections.smoothScrollToPosition(0);
                        isShowingSearchResults = true;
                        Toast.makeText(getContext(), "Found " + recommendations.size() + " online results",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    progressBarLoading.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Search error: " + error,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void restoreOriginalSections() {
        if (!originalGenreSections.isEmpty()) {
            genreSectionAdapter.setGenreSections(new ArrayList<>(originalGenreSections));
            recyclerGenreSections.smoothScrollToPosition(0);
            isShowingSearchResults = false;
            selectedGenre = null;
            if (chipGroupGenres != null) {
                chipGroupGenres.clearCheck();
            }
            if (tvSectionTitle != null) {
                tvSectionTitle.setText("Music videos for you");
            }
            Toast.makeText(getContext(), "Back to Discover", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSearchBar() {
        if (searchBarContainer != null) {
            searchBarContainer.setVisibility(View.VISIBLE);
            if (etSearchInput != null) {
                etSearchInput.requestFocus();
                showKeyboard();
            }
        }
    }

    private void hideSearchBar() {
        if (searchBarContainer != null) {
            searchBarContainer.setVisibility(View.GONE);
            if (etSearchInput != null) {
                etSearchInput.setText("");
            }
            hideKeyboard();
            if (isShowingSearchResults) {
                restoreOriginalSections();
            }
        }
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && etSearchInput != null) {
            imm.showSoftInput(etSearchInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getView() != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

    private void performSearch(String query) {
        searchMusic(query);
    }

    private void filterByGenre(String genre) {
        if (originalGenreSections.isEmpty()) {
            Toast.makeText(getContext(), "Loading genres...", Toast.LENGTH_SHORT).show();
            return;
        }

        List<GenreSection> filtered = new ArrayList<>();
        for (GenreSection section : originalGenreSections) {
            if (section.getGenreName().equalsIgnoreCase(genre)) {
                filtered.add(section);
            }
        }

        if (!filtered.isEmpty()) {
            genreSectionAdapter.setGenreSections(filtered);
            recyclerGenreSections.smoothScrollToPosition(0);
            if (tvSectionTitle != null) {
                tvSectionTitle.setText(genre + " for you");
            }
            Toast.makeText(getContext(), "Showing " + genre, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "No " + genre + " tracks available", Toast.LENGTH_SHORT).show();
            if (chipGroupGenres != null) {
                chipGroupGenres.clearCheck();
            }
            selectedGenre = null;
        }
    }

    private void checkAudioPermissionAndStartVoiceRecognition() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecognition();
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search...");

        try {
            speechRecognizerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Voice search not available on this device",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
