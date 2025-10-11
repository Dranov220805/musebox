package com.example.musebox.activities;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.musebox.R;
import com.example.musebox.database.SongDatabaseHelper;
import com.example.musebox.fragments.*;
import com.example.musebox.models.Song;
import com.example.musebox.services.MusicService;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity
        implements NavigationBarFragment.OnNavigationItemSelectedListener {

    private SongDatabaseHelper dbHelper;

    private MusicService musicService;
    private boolean isBound = false;

    // Mini player views
    private View miniPlayer;
    private TextView tvSongTitle;
    private ImageButton btnPlayPause, btnSpeed;
    private SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        miniPlayer = findViewById(R.id.includeMiniPlayer);
        tvSongTitle = miniPlayer.findViewById(R.id.txtSongTitle);
        btnPlayPause = miniPlayer.findViewById(R.id.btnPlayPause);
//        btnSpeed = miniPlayer.findViewById(R.id.btnSpeed);

        btnPlayPause.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.pauseOrResume();
                if (musicService.isPlaying()) {
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                } else {
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                }
            }
        });

//        btnSpeed.setOnClickListener(v -> {
//            if (musicService != null) {
//                float newSpeed = musicService.getPlaybackSpeed() == 1.0f ? 1.5f : 1.0f;
//                musicService.setPlaybackSpeed(newSpeed);
//                btnSpeed.setText(newSpeed + "x");
//            }
//        });

        dbHelper = new SongDatabaseHelper(this);

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
    public void onNavigationItemSelected(String item) {
        Fragment selected = null;
        switch (item) {
            case "home": selected = new HomeFragment(); break;
            case "search": selected = new SearchFragment(); break;
            case "playlist": selected = new PlaylistFragment(); break;
            case "profile": selected = new ProfileFragment(); break;
        }

        if (selected != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_container, selected)
                    .commit();
        }
    }

    @Override
    public void onCreatePlaylistSelected() {
        new AlertDialog.Builder(this)
                .setTitle("Create Playlist")
                .setMessage("Feature coming soon!")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onImportMusicSelected(Uri folderUri) {
        Toast.makeText(this, "Importing music from: " + folderUri, Toast.LENGTH_SHORT).show();
        // Try using MediaStore first
        importMusicFromMediaStore(folderUri);
    }

    /**
     * Use MediaStore for system-wide indexed songs if possible,
     * otherwise fall back to manual recursive scan.
     */
    private void importMusicFromMediaStore(Uri folderUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Importing Music");
        builder.setCancelable(false);

        final View progressView = getLayoutInflater().inflate(R.layout.dialog_import_progress, null);
        builder.setView(progressView);

        AlertDialog dialog = builder.create();
        dialog.show();

        ProgressBar progressBar = progressView.findViewById(R.id.progressBar);
        TextView textStatus = progressView.findViewById(R.id.textStatus);
        TextView textCount = progressView.findViewById(R.id.textCount);

        new Thread(() -> {
            List<Song> importedSongs = new ArrayList<>();

            try {
                ContentResolver resolver = getContentResolver();
                Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

                String[] projection = {
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.DURATION
                };

                try (Cursor cursor = resolver.query(uri, projection, null, null, null)) {
                    if (cursor == null || cursor.getCount() == 0) {
                        // If MediaStore gives nothing, fall back
                        runOnUiThread(() -> {
                            Toast.makeText(this, "No MediaStore data, scanning manually...", Toast.LENGTH_SHORT).show();
                        });
                        dialog.dismiss();
                        importMusicFromFolder(folderUri);
                        return;
                    }

                    int total = cursor.getCount();
                    int imported = 0;
                    runOnUiThread(() -> {
                        progressBar.setMax(total);
                        textStatus.setText("Importing from MediaStore...");
                        textCount.setText("0 / " + total);
                    });

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                        String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                        String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                        String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                        long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));

                        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                        Song song = new Song(title, artist, contentUri.toString(), (int) duration);
                        dbHelper.addSong(song);
                        importedSongs.add(song);

                        imported++;
                        int finalImported = imported;
                        runOnUiThread(() -> {
                            textStatus.setText("Importing: " + title);
                            textCount.setText(finalImported + " / " + total);
                            progressBar.setProgress(finalImported);
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error using MediaStore. Falling back to folder scan.", Toast.LENGTH_SHORT).show();
                });
                dialog.dismiss();
                importMusicFromFolder(folderUri);
                return;
            }

            runOnUiThread(() -> {
                dialog.dismiss();
                Toast.makeText(this, "MediaStore import complete!", Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    /**
     * Manual SAF-based recursive scanner — used only if MediaStore fails
     */
    private void importMusicFromFolder(Uri treeUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Scanning Folder");
        builder.setCancelable(false);

        final View progressView = getLayoutInflater().inflate(R.layout.dialog_import_progress, null);
        builder.setView(progressView);

        AlertDialog dialog = builder.create();
        dialog.show();

        ProgressBar progressBar = progressView.findViewById(R.id.progressBar);
        TextView textStatus = progressView.findViewById(R.id.textStatus);
        TextView textCount = progressView.findViewById(R.id.textCount);

        new Thread(() -> {
            List<Song> importedSongs = new ArrayList<>();
            int total = countAudioFiles(treeUri);
            final int totalFiles = Math.max(total, 1);

            runOnUiThread(() -> {
                progressBar.setMax(totalFiles);
                textStatus.setText("Scanning...");
                textCount.setText("0 / " + totalFiles);
            });

            scanFolderRecursively(treeUri, importedSongs, new ImportProgressCallback() {
                int imported = 0;
                @Override
                public void onSongDetected(String name) {
                    imported++;
                    int finalImported = imported;
                    runOnUiThread(() -> {
                        textStatus.setText("Importing: " + name);
                        textCount.setText(finalImported + " / " + totalFiles);
                        progressBar.setProgress(finalImported);
                    });
                }
            });

            runOnUiThread(() -> {
                dialog.dismiss();
                Toast.makeText(
                        HomeActivity.this,
                        "Imported " + importedSongs.size() + " songs successfully!",
                        Toast.LENGTH_LONG
                ).show();
            });
        }).start();
    }

    private void scanFolderRecursively(Uri folderUri, List<Song> importedSongs, ImportProgressCallback callback) {
        ContentResolver resolver = getContentResolver();
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                folderUri, DocumentsContract.getTreeDocumentId(folderUri)
        );

        try (Cursor cursor = resolver.query(childrenUri,
                new String[]{
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                },
                null, null, null)) {

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String docId = cursor.getString(0);
                    String displayName = cursor.getString(1);
                    String mimeType = cursor.getString(2);

                    Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId);

                    if (mimeType == null) continue;

                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                        scanFolderRecursively(fileUri, importedSongs, callback);
                    } else if (mimeType.startsWith("audio/")) {
                        Song song = new Song(displayName, "Unknown Artist", fileUri.toString(), 0);
                        dbHelper.addSong(song);
                        importedSongs.add(song);
                        if (callback != null) callback.onSongDetected(displayName);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int countAudioFiles(Uri folderUri) {
        int count = 0;
        ContentResolver resolver = getContentResolver();
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                folderUri, DocumentsContract.getTreeDocumentId(folderUri)
        );

        try (Cursor cursor = resolver.query(childrenUri,
                new String[]{
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                },
                null, null, null)) {

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String docId = cursor.getString(0);
                    String mimeType = cursor.getString(1);
                    Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId);

                    if (mimeType == null) continue;

                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                        count += countAudioFiles(fileUri);
                    } else if (mimeType.startsWith("audio/")) {
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    private interface ImportProgressCallback {
        void onSongDetected(String name);
    }
}