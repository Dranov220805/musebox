package com.example.musebox.fragments;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.adapters.GenreCardAdapter;
import com.example.musebox.adapters.MusicCardAdapter;
import com.example.musebox.api.JamendoApiService;
import com.example.musebox.models.MusicRecommendation;

import java.util.Arrays;
import java.util.List;

public class DiscoverFragment extends Fragment {

    private ImageButton btnBack;
    private LinearLayout searchBar;
    private RecyclerView recyclerPopular;
    private RecyclerView recyclerGenres;
    private RecyclerView recyclerRecommended;
    private ProgressBar progressBarLoading;

    private MusicCardAdapter popularAdapter;
    private MusicCardAdapter recommendedAdapter;
    private GenreCardAdapter genreAdapter;

    // Interface for communicating with parent activity
    public interface OnDiscoverFragmentListener {
        void onBackPressed();

        void onSearchClicked();

        void onMusicSelected(MusicRecommendation music);

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
        recyclerPopular = view.findViewById(R.id.recyclerPopular);
        recyclerGenres = view.findViewById(R.id.recyclerGenres);
        recyclerRecommended = view.findViewById(R.id.recyclerRecommended);
        progressBarLoading = view.findViewById(R.id.progressBarLoading);

        setupRecyclerViews();
        setupClickListeners();
        loadData();

        return view;
    }

    private void setupRecyclerViews() {
        // Popular tracks - horizontal scroll
        popularAdapter = new MusicCardAdapter();
        popularAdapter.setOnMusicCardClickListener(music -> {
            if (listener != null) {
                listener.onMusicSelected(music);
            }
        });
        LinearLayoutManager popularLayoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL, false);
        recyclerPopular.setLayoutManager(popularLayoutManager);
        recyclerPopular.setAdapter(popularAdapter);

        // Genres - vertical grid (2 columns for portrait, 4 columns for landscape)
        genreAdapter = new GenreCardAdapter();
        genreAdapter.setOnGenreCardClickListener(genre -> {
            if (listener != null) {
                listener.onGenreSelected(genre);
            } else {
                // If no listener, show genre tracks directly
                showGenreTracks(genre);
            }
        });

        // Determine grid columns based on orientation
        int orientation = getResources().getConfiguration().orientation;
        int spanCount = (orientation == Configuration.ORIENTATION_LANDSCAPE) ? 4 : 2;
        GridLayoutManager genreLayoutManager = new GridLayoutManager(getContext(), spanCount);
        recyclerGenres.setLayoutManager(genreLayoutManager);
        recyclerGenres.setAdapter(genreAdapter);

        // Recommended - horizontal scroll
        recommendedAdapter = new MusicCardAdapter();
        recommendedAdapter.setOnMusicCardClickListener(music -> {
            if (listener != null) {
                listener.onMusicSelected(music);
            }
        });
        LinearLayoutManager recommendedLayoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL, false);
        recyclerRecommended.setLayoutManager(recommendedLayoutManager);
        recyclerRecommended.setAdapter(recommendedAdapter);
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

    private void loadData() {
        loadPopularTracks();
        loadGenres();
        loadRecommendedTracks();
    }

    private void loadPopularTracks() {
        progressBarLoading.setVisibility(View.VISIBLE);

        JamendoApiService.getPopularTracks(20, new JamendoApiService.MusicRecommendationCallback() {
            @Override
            public void onSuccess(List<MusicRecommendation> recommendations) {
                if (getActivity() != null && isAdded()) {
                    progressBarLoading.setVisibility(View.GONE);
                    popularAdapter.setMusicList(recommendations);
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    progressBarLoading.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error loading popular tracks: " + error,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadGenres() {
        // Set preset genres
        List<String> genres = Arrays.asList(
                "rock", "pop", "electronic", "jazz",
                "metal", "classical", "hiphop", "ambient");
        genreAdapter.setGenreList(genres);
    }

    private void loadRecommendedTracks() {
        // Load a different set of tracks for recommended section
        JamendoApiService.getTracksByGenre("electronic", 20,
                new JamendoApiService.MusicRecommendationCallback() {
                    @Override
                    public void onSuccess(List<MusicRecommendation> recommendations) {
                        if (getActivity() != null && isAdded()) {
                            recommendedAdapter.setMusicList(recommendations);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        // Silent fail for recommended section
                    }
                });
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
                        // Show results in popular section
                        popularAdapter.setMusicList(recommendations);
                        // Scroll to top
                        recyclerPopular.smoothScrollToPosition(0);
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

    private void showGenreTracks(String genre) {
        progressBarLoading.setVisibility(View.VISIBLE);

        JamendoApiService.getTracksByGenre(genre, 20, new JamendoApiService.MusicRecommendationCallback() {
            @Override
            public void onSuccess(List<MusicRecommendation> recommendations) {
                if (getActivity() != null && isAdded()) {
                    progressBarLoading.setVisibility(View.GONE);
                    if (recommendations.isEmpty()) {
                        Toast.makeText(getContext(), "No tracks found in genre '" + genre + "'",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // Show results in popular section
                        popularAdapter.setMusicList(recommendations);
                        // Scroll to top
                        recyclerPopular.smoothScrollToPosition(0);
                        String displayGenre = genre.substring(0, 1).toUpperCase() + genre.substring(1);
                        Toast.makeText(getContext(), displayGenre + " tracks loaded",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    progressBarLoading.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error loading genre: " + error,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
