package com.example.musebox;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.module.AppGlideModule;

@com.bumptech.glide.annotation.GlideModule
public final class GlideModule extends AppGlideModule {

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        // Set Glide log level to reduce verbose error logging
        builder.setLogLevel(Log.ERROR);
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}