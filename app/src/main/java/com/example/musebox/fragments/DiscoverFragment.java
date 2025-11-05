package com.example.musebox.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.adapters.GenreSectionAdapter;
import com.example.musebox.api.JamendoApiService;
import com.example.musebox.models.GenreSection;
import com.example.musebox.models.MusicRecommendation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiscoverFragment extends Fragment {

    private ImageButton btnBack;
    private LinearLayout searchBar;
    private RecyclerView recyclerGenreSections;
    private ProgressBar progressBarLoading;

    private GenreSectionAdapter genreSectionAdapter;
    private List<String> genres = Arrays.asList(
            "rock", "pop", "electronic", "jazz",
            "metal", "classical", "hiphop", "ambient",
            "indie", "blues");

    private int loadedGenresCount = 0;

    // Interface for communicating with parent activity
    public interface OnDiscoverFragmentListener {
        void onBackPressed();

        void onSearchClicked();

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        btnBack = view.findViewById(R.id.btnBack);
        searchBar = view.findViewById(R.id.searchBar);
        recyclerGenreSections = view.findViewById(R.id.recyclerGenreSections);
        progressBarLoading = view.findViewById(R.id.progressBarLoading);

        setupRecyclerView();
        setupClickListeners();
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

    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBackPressed();
            } else {
                requireActivity().onBackPressed();
            }
        });

        // Search bar
        searchBar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSearchClicked();
            } else {
                showSearchDialog();
            }
        });
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

    private void showSearchDialog() {
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Enter artist or song name");
        input.setPadding(50, 30, 50, 30);

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Search Music")
                .setMessage("Search for music on Jamendo:")
                .setView(input)
                .setPositiveButton("Search", (dialog, which) -> {
                    String query = input.getText().toString().trim();
                    if (!query.isEmpty()) {
                        searchMusic(query);
                    } else {
                        Toast.makeText(requireContext(), "Please enter a search term",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void searchMusic(String query) {
        progressBarLoading.setVisibility(View.VISIBLE);

        JamendoApiService.searchTracks(query, 20, new JamendoApiService.MusicRecommendationCallback() {
            @Override
            public void onSuccess(List<MusicRecommendation> recommendations) {
                if (getActivity() != null && isAdded()) {
                    progressBarLoading.setVisibility(View.GONE);
                    if (recommendations.isEmpty()) {
                        Toast.makeText(getContext(), "No results found for '" + query + "'",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // Show results as a new section at top
                        List<GenreSection> currentSections = new ArrayList<>();
                        currentSections.add(new GenreSection("Search: " + query, recommendations));
                        genreSectionAdapter.setGenreSections(currentSections);
                        recyclerGenreSections.smoothScrollToPosition(0);
                        Toast.makeText(getContext(), "Found " + recommendations.size() + " results",
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
}
