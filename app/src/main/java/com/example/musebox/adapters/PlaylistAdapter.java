package com.example.musebox.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musebox.R;
import com.example.musebox.models.Playlist;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private final Context context;
    private final List<Playlist> playlists;
    private final OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
        void onRenamePlaylist(Playlist playlist);
        void onDeletePlaylist(Playlist playlist);
    }

    public PlaylistAdapter(Context context, List<Playlist> playlists, OnPlaylistClickListener listener) {
        this.context = context;
        this.playlists = playlists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.bind(playlist);
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    class PlaylistViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPlaylistIcon;
        TextView txtPlaylistName, txtPlaylistDetails;
        ImageButton btnPlaylistMenu;

        PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPlaylistIcon = itemView.findViewById(R.id.ivPlaylistIcon);
            txtPlaylistName = itemView.findViewById(R.id.txtPlaylistName);
            txtPlaylistDetails = itemView.findViewById(R.id.txtPlaylistDetails);
            btnPlaylistMenu = itemView.findViewById(R.id.btnPlaylistMenu);
        }

        void bind(Playlist playlist) {
            txtPlaylistName.setText(playlist.getName());

            String date = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(playlist.getCreatedAt());
            txtPlaylistDetails.setText(playlist.getSongCount() + " songs • " + date);

            itemView.setOnClickListener(v -> listener.onPlaylistClick(playlist));
            btnPlaylistMenu.setOnClickListener(v -> showPopupMenu(v, playlist));
        }

        private void showPopupMenu(View view, Playlist playlist) {
            PopupMenu popup = new PopupMenu(context, view);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.menu_playlist_options, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> handleMenuClick(item, playlist));
            popup.show();
        }

        private boolean handleMenuClick(MenuItem item, Playlist playlist) {
            int id = item.getItemId();
            if (id == R.id.action_rename) {
                listener.onRenamePlaylist(playlist);
                return true;
            } else if (id == R.id.action_delete) {
                confirmDelete(playlist);
                return true;
            }
            return false;
        }

        private void confirmDelete(Playlist playlist) {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Playlist")
                    .setMessage("Are you sure you want to delete \"" + playlist.getName() + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> listener.onDeletePlaylist(playlist))
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }
}
