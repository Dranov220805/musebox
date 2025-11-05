package com.example.musebox.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musebox.R;
import com.example.musebox.adapters.MusicCardAdapter;
import com.example.musebox.api.JamendoApiService;
import com.example.musebox.models.MusicRecommendation;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Bottom sheet fragment to display music details with play button and options
 */
public class MusicDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_MUSIC = "music";
    private static final String ARG_GENRE = "genre";

    private MusicRecommendation music;
    private String genre;

    private ImageView ivAlbumArt;
    private TextView tvTrackName;
    private TextView tvArtistName;
    private TextView tvGenre;
    private TextView tvDuration;
    private MaterialButton btnPlay;
    private ImageButton btnMenu;
    private ImageButton btnFavorite;
    private ImageButton btnAddToQueue;
    private ImageButton btnArtistProfile;
    private RecyclerView recyclerSimilar;
    private MusicCardAdapter similarAdapter;

    private OnMusicActionListener actionListener;

    public interface OnMusicActionListener {
        void onPlayMusic(MusicRecommendation music);

        void onAddToFavorites(MusicRecommendation music);

        void onAddToQueue(MusicRecommendation music);

        void onGoToArtist(String artistName);

        void onMusicSelected(MusicRecommendation music);
    }

    public static MusicDetailBottomSheet newInstance(MusicRecommendation music, String genre) {
        MusicDetailBottomSheet fragment = new MusicDetailBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MUSIC, music);
        args.putString(ARG_GENRE, genre);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            music = (MusicRecommendation) getArguments().getSerializable(ARG_MUSIC);
            genre = getArguments().getString(ARG_GENRE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_music_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        displayMusicInfo();
        setupClickListeners();
        loadSimilarMusic();
    }

    private void initViews(View view) {
        ivAlbumArt = view.findViewById(R.id.ivAlbumArt);
        tvTrackName = view.findViewById(R.id.tvTrackName);
        tvArtistName = view.findViewById(R.id.tvArtistName);
        tvGenre = view.findViewById(R.id.tvGenre);
        tvDuration = view.findViewById(R.id.tvDuration);
        btnPlay = view.findViewById(R.id.btnPlay);
        btnMenu = view.findViewById(R.id.btnMenu);
        btnFavorite = view.findViewById(R.id.btnFavorite);
        btnAddToQueue = view.findViewById(R.id.btnAddToQueue);
        btnArtistProfile = view.findViewById(R.id.btnArtistProfile);
        recyclerSimilar = view.findViewById(R.id.recyclerSimilar);

        // Setup RecyclerView for similar tracks
        similarAdapter = new MusicCardAdapter();
        recyclerSimilar.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL, false));
        recyclerSimilar.setAdapter(similarAdapter);
    }

    private void displayMusicInfo() {
        if (music == null)
            return;

        tvTrackName.setText(music.getTrackName());
        tvArtistName.setText(music.getArtistName());

        if (genre != null && !genre.isEmpty()) {
            tvGenre.setText(genre.substring(0, 1).toUpperCase() + genre.substring(1));
            tvGenre.setVisibility(View.VISIBLE);
        } else {
            tvGenre.setVisibility(View.GONE);
        }

        // Format duration
        int durationSeconds = music.getDuration();
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        tvDuration.setText(String.format("%d:%02d", minutes, seconds));

        // Load album art
        if (music.getImageUrl() != null && !music.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(music.getImageUrl())
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_music_note)
                    .centerCrop()
                    .into(ivAlbumArt);
        }
    }

    private void setupClickListeners() {
        btnPlay.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onPlayMusic(music);
            }
            dismiss();
        });

        btnFavorite.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onAddToFavorites(music);
            }
            Toast.makeText(getContext(), "Added to favorites", Toast.LENGTH_SHORT).show();
        });

        btnAddToQueue.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onAddToQueue(music);
            }
            Toast.makeText(getContext(), "Added to queue", Toast.LENGTH_SHORT).show();
        });

        btnArtistProfile.setOnClickListener(v -> {
            if (actionListener != null && music != null) {
                actionListener.onGoToArtist(music.getArtistName());
            }
            dismiss();
        });

        btnMenu.setOnClickListener(v -> showOptionsMenu());

        // Click on similar tracks
        similarAdapter.setOnMusicCardClickListener(selectedMusic -> {
            if (actionListener != null) {
                actionListener.onMusicSelected(selectedMusic);
            }
            // Update current music and reload
            music = selectedMusic;
            displayMusicInfo();
            loadSimilarMusic();
        });
    }

    private void loadSimilarMusic() {
        if (genre == null || genre.isEmpty())
            return;

        JamendoApiService.getTracksByGenre(genre, 20, new JamendoApiService.MusicRecommendationCallback() {
            @Override
            public void onSuccess(List<MusicRecommendation> recommendations) {
                if (getActivity() != null && isAdded()) {
                    similarAdapter.setMusicList(recommendations);
                }
            }

            @Override
            public void onError(String error) {
                // Silent fail
            }
        });
    }

    private void showOptionsMenu() {
        String[] options = { "Add to Playlist", "Share", "Download" };

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Toast.makeText(getContext(), "Add to Playlist (Coming soon)",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            Toast.makeText(getContext(), "Share (Coming soon)",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            Toast.makeText(getContext(), "Download (Coming soon)",
                                    Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    public void setActionListener(OnMusicActionListener listener) {
        this.actionListener = listener;
    }
}
