package com.fyp.dao;

import com.fyp.model.Examiner;
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

public class ExaminerDAO {

    public Optional<Examiner> findByUserId(UUID userId, String jwt) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/examiners?select=examiner_id,expertise,is_external,users(user_id,name,email,is_active,is_email_verified)&user_id=eq." + userId))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return Optional.of(mapRow(array.get(0).getAsJsonObject()));
        }
        return Optional.empty();
    }

    public List<Examiner> findAll(String jwt) throws Exception {
        List<Examiner> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/examiners?select=examiner_id,expertise,is_external,users(user_id,name,email,is_active,is_email_verified)"))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("users") && obj.get("users").getAsJsonObject().get("is_active").getAsBoolean()) {
                    list.add(mapRow(obj));
                }
            }
        }
        return list;
    }

    public UUID insert(UUID userId, boolean isExternal, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("user_id", userId.toString());
        json.addProperty("is_external", isExternal);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/examiners?select=examiner_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 201) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return UUID.fromString(array.get(0).getAsJsonObject().get("examiner_id").getAsString());
        }
        throw new Exception("Failed to insert examiner: " + response.body());
    }

    public void assignToProject(UUID projectId, UUID examinerId, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("project_id", projectId.toString());
        json.addProperty("examiner_id", examinerId.toString());

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/examiner_assignments"))
                .header("Prefer", "resolution=ignore-duplicates") // Emulates ON CONFLICT DO NOTHING
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    public List<Examiner> findByProjectId(UUID projectId, String jwt) throws Exception {
        List<Examiner> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/examiner_assignments?select=examiners(examiner_id,expertise,is_external,users(user_id,name,email,is_active,is_email_verified))&project_id=eq." + projectId))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            for (JsonElement el : array) {
                JsonObject assignmentObj = el.getAsJsonObject();
                if (assignmentObj.has("examiners") && !assignmentObj.get("examiners").isJsonNull()) {
                    list.add(mapRow(assignmentObj.getAsJsonObject("examiners")));
                }
            }
        }
        return list;
    }

    private Examiner mapRow(JsonObject obj) {
        JsonObject userObj = obj.getAsJsonObject("users");
        
        List<String> expertise = new ArrayList<>();
        if (obj.has("expertise") && !obj.get("expertise").isJsonNull()) {
            JsonArray expArr = obj.getAsJsonArray("expertise");
            for (JsonElement exp : expArr) {
                expertise.add(exp.getAsString());
            }
        }
        
        return new Examiner(
            UUID.fromString(userObj.get("user_id").getAsString()),
            userObj.has("name") && !userObj.get("name").isJsonNull() ? userObj.get("name").getAsString() : "",
            userObj.has("email") && !userObj.get("email").isJsonNull() ? userObj.get("email").getAsString() : "",
            "", // no password hash
            userObj.has("is_active") && userObj.get("is_active").getAsBoolean(),
            userObj.has("is_email_verified") && userObj.get("is_email_verified").getAsBoolean(),
            UUID.fromString(obj.get("examiner_id").getAsString()),
            expertise,
            obj.has("is_external") && obj.get("is_external").getAsBoolean()
        );
    }
}
