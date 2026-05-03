package com.fyp.dao;

import com.fyp.model.User;
import com.fyp.util.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST API DAO for fyp.users table.
 */
public class UserDAO {

    public Optional<User> findByEmail(String email, String jwt) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/users?email=eq." + email))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return Optional.of(mapRow(array.get(0).getAsJsonObject()));
        }
        return Optional.empty();
    }

    public Optional<User> findById(UUID userId, String jwt) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/users?user_id=eq." + userId))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return Optional.of(mapRow(array.get(0).getAsJsonObject()));
        }
        return Optional.empty();
    }

    public List<User> findAll(String jwt) throws Exception {
        List<User> users = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/users?order=name.asc"))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            for (JsonElement el : array) {
                users.add(mapRow(el.getAsJsonObject()));
            }
        }
        return users;
    }

    public List<User> findByRole(String role, String jwt) throws Exception {
        List<User> users = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/users?role=eq." + role + "&order=name.asc"))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            for (JsonElement el : array) {
                users.add(mapRow(el.getAsJsonObject()));
            }
        }
        return users;
    }

    public void updateProfile(UUID userId, String name, String email, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("email", email);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/users?user_id=eq." + userId))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    public void setActive(UUID userId, boolean active, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("is_active", active);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/users?user_id=eq." + userId))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    private User mapRow(JsonObject obj) {
        return new User(
            UUID.fromString(obj.get("user_id").getAsString()),
            obj.has("name") && !obj.get("name").isJsonNull() ? obj.get("name").getAsString() : "",
            obj.has("email") && !obj.get("email").isJsonNull() ? obj.get("email").getAsString() : "",
            "", // no password hash in REST
            obj.has("is_active") && obj.get("is_active").getAsBoolean(),
            true, // email verified handled by auth
            obj.has("role") && !obj.get("role").isJsonNull() ? obj.get("role").getAsString() : ""
        ) {};
    }
}
