package com.example.musebox.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.musebox.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet fragment for create options (Create Playlist / Import Music)
 */
public class CreateBottomSheetFragment extends BottomSheetDialogFragment {

    private LinearLayout layoutCreatePlaylist;
    private LinearLayout layoutImportMusic;

    private OnCreateActionListener actionListener;

    public interface OnCreateActionListener {
        void onCreatePlaylist();

        void onImportMusic();
    }

    public static CreateBottomSheetFragment newInstance() {
        return new CreateBottomSheetFragment();
    }

    public void setOnCreateActionListener(OnCreateActionListener listener) {
        this.actionListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_create, container, false);

        // Initialize views
        layoutCreatePlaylist = view.findViewById(R.id.layoutCreatePlaylist);
        layoutImportMusic = view.findViewById(R.id.layoutImportMusic);

        // Set click listeners
        layoutCreatePlaylist.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onCreatePlaylist();
            }
            dismiss();
        });

        layoutImportMusic.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onImportMusic();
            }
            dismiss();
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Expand the bottom sheet fully by default (especially important in landscape
        // mode)
        Dialog dialog = getDialog();
        if (dialog != null) {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
            FrameLayout bottomSheet = bottomSheetDialog
                    .findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }
}
