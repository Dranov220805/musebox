package com.example.musebox.api;

import android.os.Handler;
import android.os.Looper;

import com.example.musebox.models.MusicRecommendation;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for fetching music from Jamendo API
 * Jamendo provides free music that can be streamed legally
 * API Documentation: https://developer.jamendo.com/v3.0
 */
public class JamendoApiService {

    // Get your API key from: https://developer.jamendo.com/
    private static final String CLIENT_ID = "c5b4b61e"; // Replace with your actual client ID
    private static final String BASE_URL = "https://api.jamendo.com/v3.0";

    public interface MusicRecommendationCallback {
        void onSuccess(List<MusicRecommendation> recommendations);

        void onError(String error);
    }

    /**
     * Get popular tracks
     * 
     * @param limit    Number of tracks to fetch (max 200)
     * @param callback Callback to handle results
     */
    public static void getPopularTracks(int limit, MusicRecommendationCallback callback) {
        new Thread(() -> {
            try {
                String url = BASE_URL + "/tracks/?client_id=" + CLIENT_ID +
                        "&format=json" +
                        "&limit=" + limit +
                        "&order=popularity_week" +
                        "&include=musicinfo";

                String response = ApiClient.get(url);
                List<MusicRecommendation> recommendations = parseTracksResponse(response);

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!recommendations.isEmpty()) {
                        callback.onSuccess(recommendations);
                    } else {
                        callback.onError("No tracks found");
                    }
                });

            } catch (IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Network error: " + e.getMessage()));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Error: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Search tracks by name or artist
     */
    public static void searchTracks(String query, int limit, MusicRecommendationCallback callback) {
        new Thread(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                String url = BASE_URL + "/tracks/?client_id=" + CLIENT_ID +
                        "&format=json" +
                        "&limit=" + limit +
                        "&search=" + encodedQuery +
                        "&include=musicinfo";

                String response = ApiClient.get(url);
                List<MusicRecommendation> recommendations = parseTracksResponse(response);

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!recommendations.isEmpty()) {
                        callback.onSuccess(recommendations);
                    } else {
                        callback.onError("No tracks found");
                    }
                });

            } catch (IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Network error: " + e.getMessage()));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Error: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Get tracks by genre
     */
    public static void getTracksByGenre(String genre, int limit, MusicRecommendationCallback callback) {
        new Thread(() -> {
            try {
                String url = BASE_URL + "/tracks/?client_id=" + CLIENT_ID +
                        "&format=json" +
                        "&limit=" + limit +
                        "&tags=" + genre +
                        "&order=popularity_month" +
                        "&include=musicinfo";

                String response = ApiClient.get(url);
                List<MusicRecommendation> recommendations = parseTracksResponse(response);

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!recommendations.isEmpty()) {
                        callback.onSuccess(recommendations);
                    } else {
                        callback.onError("No tracks found");
                    }
                });

            } catch (IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Network error: " + e.getMessage()));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Error: " + e.getMessage()));
            }
        }).start();
    }

    private static List<MusicRecommendation> parseTracksResponse(String jsonResponse) {
        List<MusicRecommendation> recommendations = new ArrayList<>();

        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();

            if (root.has("results")) {
                JsonArray results = root.getAsJsonArray("results");

                for (int i = 0; i < results.size(); i++) {
                    JsonObject track = results.get(i).getAsJsonObject();

                    String trackName = track.get("name").getAsString();
                    String artistName = track.get("artist_name").getAsString();
                    String albumName = track.get("album_name").getAsString();
                    String audioUrl = track.get("audio").getAsString();
                    String imageUrl = track.get("image").getAsString();
                    int duration = track.get("duration").getAsInt();

                    MusicRecommendation recommendation = new MusicRecommendation(
                            trackName,
                            artistName,
                            albumName,
                            audioUrl,
                            imageUrl,
                            duration,
                            "jamendo");

                    recommendations.add(recommendation);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return recommendations;
    }
}
