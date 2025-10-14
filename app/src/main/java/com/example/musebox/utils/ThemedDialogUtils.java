package com.example.musebox.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.musebox.R;

public class ThemedDialogUtils {

    public interface OnDialogClickListener {
        void onPositiveClick();

        default void onNegativeClick() {
        }
    }

    public interface OnListItemClickListener {
        void onItemSelected(int position);
    }

    /**
     * Show a themed list dialog for selecting from multiple options
     */
    public static void showListDialog(Context context,
            String title,
            String[] items,
            int selectedIndex,
            int iconRes,
            int iconTintRes,
            OnListItemClickListener listener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        
        if (iconRes != 0) {
            builder.setIcon(iconRes);
        }
        
        builder.setSingleChoiceItems(items, selectedIndex, (dialog, which) -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onItemSelected(which);
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Show a simple themed dialog with title, message, and action buttons
     */
    public static void showSimpleDialog(Context context,
            String title,
            String message,
            int iconRes,
            int iconTintRes,
            String positiveText,
            String negativeText,
            OnDialogClickListener listener) {

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_simple, null);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        // Make dialog background transparent for rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Set content
        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        TextView messageView = dialogView.findViewById(R.id.dialog_message);
        ImageView iconView = dialogView.findViewById(R.id.dialog_icon);
        View iconContainer = dialogView.findViewById(R.id.icon_container);
        TextView positiveBtn = dialogView.findViewById(R.id.btn_positive);
        TextView negativeBtn = dialogView.findViewById(R.id.btn_negative);

        titleView.setText(title);
        messageView.setText(message);
        iconView.setImageResource(iconRes);
        iconView.setColorFilter(context.getColor(iconTintRes));

        // Set positive button
        if (positiveText != null) {
            positiveBtn.setText(positiveText);
            positiveBtn.setOnClickListener(v -> {
                dialog.dismiss();
                if (listener != null)
                    listener.onPositiveClick();
            });
        }

        // Set negative button
        if (negativeText != null) {
            negativeBtn.setText(negativeText);
            negativeBtn.setVisibility(View.VISIBLE);
            negativeBtn.setOnClickListener(v -> {
                dialog.dismiss();
                if (listener != null)
                    listener.onNegativeClick();
            });
        } else {
            negativeBtn.setVisibility(View.GONE);
        }

        dialog.show();
    }

    /**
     * Show info dialog with green theme
     */
    public static void showInfoDialog(Context context,
            String title,
            String message,
            OnDialogClickListener listener) {
        showSimpleDialog(context, title, message,
                R.drawable.ic_music_note, R.color.white,
                "OK", null, listener);
    }

    /**
     * Show warning dialog with orange theme
     */
    public static void showWarningDialog(Context context,
            String title,
            String message,
            String positiveText,
            String negativeText,
            OnDialogClickListener listener) {
        showSimpleDialog(context, title, message,
                R.drawable.ic_warning, R.color.warning,
                positiveText, negativeText, listener);
    }

    /**
     * Show error dialog with red theme
     */
    public static void showErrorDialog(Context context,
            String title,
            String message,
            OnDialogClickListener listener) {
        showSimpleDialog(context, title, message,
                R.drawable.ic_error, R.color.error,
                "OK", null, listener);
    }
}