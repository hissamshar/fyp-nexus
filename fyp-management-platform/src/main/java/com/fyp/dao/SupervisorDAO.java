package com.fyp.dao;

import com.fyp.model.Supervisor;
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

public class SupervisorDAO {

    public Optional<Supervisor> findByUserId(UUID userId, String jwt) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/supervisors?select=supervisor_id,employee_id,research_area,slots_available,users(user_id,name,email,is_active)&user_id=eq." + userId))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return Optional.of(mapRow(array.get(0).getAsJsonObject()));
        }
        return Optional.empty();
    }

    public Optional<Supervisor> findBySupervisorId(UUID supervisorId, String jwt) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/supervisors?select=supervisor_id,employee_id,research_area,slots_available,users(user_id,name,email,is_active)&supervisor_id=eq." + supervisorId))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return Optional.of(mapRow(array.get(0).getAsJsonObject()));
        }
        return Optional.empty();
    }

    public List<Supervisor> findAll(String jwt) throws Exception {
        List<Supervisor> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/supervisors?select=supervisor_id,employee_id,research_area,slots_available,users(user_id,name,email,is_active)"))
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

    public List<Supervisor> findWithAvailableSlots(String jwt) throws Exception {
        List<Supervisor> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/supervisors?select=supervisor_id,employee_id,research_area,slots_available,users(user_id,name,email,is_active)&slots_available=gt.0&order=slots_available.desc"))
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

    public UUID insert(UUID userId, String employeeId, String researchArea, int slotsAvailable, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("user_id", userId.toString());
        json.addProperty("employee_id", employeeId);
        json.addProperty("research_area", researchArea);
        json.addProperty("slots_available", slotsAvailable);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/supervisors?select=supervisor_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 201) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return UUID.fromString(array.get(0).getAsJsonObject().get("supervisor_id").getAsString());
        }
        throw new Exception("Failed to insert supervisor: " + response.body());
    }

    public void update(UUID supervisorId, String researchArea, int slotsAvailable, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("research_area", researchArea);
        json.addProperty("slots_available", slotsAvailable);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/supervisors?supervisor_id=eq." + supervisorId))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    // Note: Decrement and Increment require atomic operations which are tricky in pure REST without RPC.
    // For simplicity in this rewrite, we fetch current, modify, and update.
    public void decrementSlots(UUID supervisorId, String jwt) throws Exception {
        Optional<Supervisor> supOpt = findBySupervisorId(supervisorId, jwt);
        if (supOpt.isPresent()) {
            int newSlots = Math.max(0, supOpt.get().getSlotsAvailable() - 1);
            update(supervisorId, supOpt.get().getResearchArea(), newSlots, jwt);
        }
    }

    public void incrementSlots(UUID supervisorId, String jwt) throws Exception {
        Optional<Supervisor> supOpt = findBySupervisorId(supervisorId, jwt);
        if (supOpt.isPresent()) {
            int newSlots = supOpt.get().getSlotsAvailable() + 1;
            update(supervisorId, supOpt.get().getResearchArea(), newSlots, jwt);
        }
    }

    private Supervisor mapRow(JsonObject obj) {
        JsonObject userObj = obj.getAsJsonObject("users");
        return new Supervisor(
            UUID.fromString(userObj.get("user_id").getAsString()),
            userObj.has("name") && !userObj.get("name").isJsonNull() ? userObj.get("name").getAsString() : "",
            userObj.has("email") && !userObj.get("email").isJsonNull() ? userObj.get("email").getAsString() : "",
            "", // no password hash
            userObj.has("is_active") && userObj.get("is_active").getAsBoolean(),
            true, // email verification owned by Supabase Auth
            UUID.fromString(obj.get("supervisor_id").getAsString()),
            obj.has("employee_id") && !obj.get("employee_id").isJsonNull() ? obj.get("employee_id").getAsString() : "",
            obj.has("research_area") && !obj.get("research_area").isJsonNull() ? obj.get("research_area").getAsString() : "",
            obj.has("slots_available") && !obj.get("slots_available").isJsonNull() ? obj.get("slots_available").getAsInt() : 0
        );
    }
}
