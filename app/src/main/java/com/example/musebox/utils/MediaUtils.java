package com.example.musebox.utils;

public class MediaUtils {
    public static String formatDuration(long durationMs) {
        long minutes = (durationMs / 1000) / 60;
        long seconds = (durationMs / 1000) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}