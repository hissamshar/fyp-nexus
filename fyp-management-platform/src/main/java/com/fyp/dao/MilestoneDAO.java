package com.fyp.dao;

import com.fyp.model.Milestone;
import com.fyp.enums.MilestoneStatus;
import com.fyp.util.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MilestoneDAO {

    public Optional<Milestone> findById(UUID milestoneId, String jwt) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/milestones?milestone_id=eq." + milestoneId))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return Optional.of(mapRow(array.get(0).getAsJsonObject()));
        }
        return Optional.empty();
    }

    public List<Milestone> findByProjectId(UUID projectId, String jwt) throws Exception {
        List<Milestone> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/milestones?project_id=eq." + projectId + "&order=deadline.asc"))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            for (JsonElement el : array) {
                list.add(mapRow(el.getAsJsonObject()));
            }
        }
        return list;
    }

    public UUID insert(Milestone m, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("title", m.getTitle());
        json.addProperty("description", m.getDescription());
        if (m.getDeadline() != null) json.addProperty("deadline", m.getDeadline().toString());
        json.addProperty("weightage", m.getWeightage());
        json.addProperty("status", m.getStatus().name());
        json.addProperty("project_id", m.getProjectId().toString());

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/milestones?select=milestone_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 201) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return UUID.fromString(array.get(0).getAsJsonObject().get("milestone_id").getAsString());
        }
        throw new Exception("Failed to insert milestone: " + response.body());
    }

    public void updateStatus(UUID milestoneId, MilestoneStatus status, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("status", status.name());

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/milestones?milestone_id=eq." + milestoneId))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    public void update(Milestone m, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("title", m.getTitle());
        json.addProperty("description", m.getDescription());
        if (m.getDeadline() != null) json.addProperty("deadline", m.getDeadline().toString());
        json.addProperty("weightage", m.getWeightage());
        json.addProperty("status", m.getStatus().name());

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/milestones?milestone_id=eq." + m.getMilestoneId()))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    public void delete(UUID milestoneId, String jwt) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/milestones?milestone_id=eq." + milestoneId))
                .DELETE();
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    public int[] getProgressCounts(UUID projectId, String jwt) throws Exception {
        List<Milestone> milestones = findByProjectId(projectId, jwt);
        int total = milestones.size();
        int completed = 0;
        for (Milestone m : milestones) {
            if (m.getStatus() == MilestoneStatus.COMPLETED) {
                completed++;
            }
        }
        return new int[]{completed, total};
    }

    private Milestone mapRow(JsonObject obj) {
        Milestone m = new Milestone();
        m.setMilestoneId(UUID.fromString(obj.get("milestone_id").getAsString()));
        m.setTitle(obj.has("title") && !obj.get("title").isJsonNull() ? obj.get("title").getAsString() : "");
        m.setDescription(obj.has("description") && !obj.get("description").isJsonNull() ? obj.get("description").getAsString() : "");
        if (obj.has("deadline") && !obj.get("deadline").isJsonNull()) {
            String deadlineStr = obj.get("deadline").getAsString();
            try {
                // Supabase returns TIMESTAMPTZ in ISO_OFFSET_DATE_TIME format
                m.setDeadline(java.time.OffsetDateTime.parse(deadlineStr).toLocalDateTime());
            } catch (Exception e) {
                // Fallback for simple ISO format
                m.setDeadline(java.time.LocalDateTime.parse(deadlineStr.split("\\+")[0].replace(" ", "T")));
            }
        }
        m.setWeightage(obj.has("weightage") && !obj.get("weightage").isJsonNull() ? obj.get("weightage").getAsInt() : 0);
        m.setStatus(MilestoneStatus.valueOf(obj.get("status").getAsString()));
        m.setProjectId(UUID.fromString(obj.get("project_id").getAsString()));
        return m;
    }

    public List<Milestone> findAll(String jwt) throws Exception {
        List<Milestone> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/milestones?order=deadline.asc"))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            for (JsonElement el : array) {
                list.add(mapRow(el.getAsJsonObject()));
            }
        }
        return list;
    }
}
