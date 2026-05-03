package com.fyp.dao;

import com.fyp.model.Admin;
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

public class AdminDAO {

    public Optional<Admin> findByUserId(UUID userId, String jwt) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/admins?select=admin_id,permissions,users(user_id,name,email,is_active,is_email_verified)&user_id=eq." + userId))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return Optional.of(mapRow(array.get(0).getAsJsonObject()));
        }
        return Optional.empty();
    }

    public UUID insert(UUID userId, List<String> permissions, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("user_id", userId.toString());
        
        JsonArray permArray = new JsonArray();
        for (String p : permissions) {
            permArray.add(p);
        }
        json.add("permissions", permArray);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/admins?select=admin_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 201) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return UUID.fromString(array.get(0).getAsJsonObject().get("admin_id").getAsString());
        }
        throw new Exception("Failed to insert admin: " + response.body());
    }

    private Admin mapRow(JsonObject obj) {
        JsonObject userObj = obj.getAsJsonObject("users");
        
        List<String> perms = new ArrayList<>();
        if (obj.has("permissions") && !obj.get("permissions").isJsonNull()) {
            JsonArray arr = obj.getAsJsonArray("permissions");
            for (JsonElement el : arr) {
                perms.add(el.getAsString());
            }
        }
        
        return new Admin(
            UUID.fromString(userObj.get("user_id").getAsString()),
            userObj.has("name") && !userObj.get("name").isJsonNull() ? userObj.get("name").getAsString() : "",
            userObj.has("email") && !userObj.get("email").isJsonNull() ? userObj.get("email").getAsString() : "",
            "", // no password hash
            userObj.has("is_active") && userObj.get("is_active").getAsBoolean(),
            userObj.has("is_email_verified") && userObj.get("is_email_verified").getAsBoolean(),
            UUID.fromString(obj.get("admin_id").getAsString()),
            perms
        );
    }
}
