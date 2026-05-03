package com.fyp.util;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

/**
 * HTTP Client for interacting with Supabase REST API.
 */
public class SupabaseClient {

    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();
    public static final Gson gson = new Gson();

    private static String SUPABASE_URL;
    private static String SUPABASE_KEY;

    static {
        try {
            Properties props = loadProperties();
            SUPABASE_URL = props.getProperty("supabase.url");
            SUPABASE_KEY = props.getProperty("supabase.key");
            
            if (SUPABASE_URL == null || SUPABASE_KEY == null) {
                System.err.println("[SupabaseClient] Warning: supabase.url or supabase.key missing from config.properties");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Properties loadProperties() throws IOException {
        Properties props = new Properties();
        java.io.File file = new java.io.File("config.properties");
        if (file.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                props.load(fis);
                return props;
            }
        }
        try (InputStream is = SupabaseClient.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is != null) props.load(is);
        }
        return props;
    }

    /**
     * Executes an HTTP request to Supabase.
     */
    public static HttpResponse<String> sendRequest(HttpRequest.Builder requestBuilder) throws Exception {
        HttpRequest request = requestBuilder
                .header("apikey", SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Accept-Profile", "fyp")
                .header("Content-Profile", "fyp")
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Executes an authenticated HTTP request to Supabase.
     */
    public static HttpResponse<String> sendAuthenticatedRequest(HttpRequest.Builder requestBuilder, String jwtToken) throws Exception {
        if (jwtToken != null && !jwtToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + jwtToken);
        }
        return sendRequest(requestBuilder);
    }

    public static String getBaseUrl() {
        return SUPABASE_URL;
    }
}
