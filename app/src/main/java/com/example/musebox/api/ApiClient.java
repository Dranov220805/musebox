package com.example.musebox.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Base API client with OkHttp for making HTTP requests
 */
public class ApiClient {

    private static OkHttpClient client;

    /**
     * Get singleton OkHttp client instance
     */
    public static OkHttpClient getClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }

    /**
     * Make a GET request and return response body as string
     */
    public static String get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body() != null ? response.body().string() : "";
        }
    }

    /**
     * Make a GET request with custom headers
     */
    public static String get(String url, String headerName, String headerValue) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader(headerName, headerValue)
                .build();

        try (Response response = getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body() != null ? response.body().string() : "";
        }
    }
}
