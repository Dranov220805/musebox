package com.example.musebox.adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import com.example.musebox.models.Song;

/**
 * DiffUtil callback for efficiently calculating differences between Song lists.
 * This enables the RecyclerView to update only changed items instead of the
 * entire list.
 */
public class SongDiffCallback extends DiffUtil.ItemCallback<Song> {

    @Override
    public boolean areItemsTheSame(@NonNull Song oldItem, @NonNull Song newItem) {
        // Compare by unique ID
        return oldItem.getId().equals(newItem.getId());
    }

    @Override
    public boolean areContentsTheSame(@NonNull Song oldItem, @NonNull Song newItem) {
        // Compare all relevant fields including album cover
        return oldItem.getTitle().equals(newItem.getTitle()) &&
                oldItem.getArtist().equals(newItem.getArtist()) &&
                oldItem.getUri().equals(newItem.getUri()) &&
                oldItem.getDuration() == newItem.getDuration() &&
                oldItem.isFavorite() == newItem.isFavorite() &&
                ((oldItem.getAlbumCoverPath() == null && newItem.getAlbumCoverPath() == null) ||
                        (oldItem.getAlbumCoverPath() != null
                                && oldItem.getAlbumCoverPath().equals(newItem.getAlbumCoverPath())));
    }
}
