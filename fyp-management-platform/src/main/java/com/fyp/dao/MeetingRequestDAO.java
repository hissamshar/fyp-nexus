package com.fyp.dao;

import com.fyp.model.MeetingRequest;
import com.fyp.enums.MeetingStatus;
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

public class MeetingRequestDAO {

    public List<MeetingRequest> findByStudentId(UUID studentId, String jwt) throws Exception {
        List<MeetingRequest> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/meeting_requests?student_id=eq." + studentId + "&order=proposed_time.desc"))
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

    public List<MeetingRequest> findBySupervisorId(UUID supervisorId, String jwt) throws Exception {
        List<MeetingRequest> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/meeting_requests?supervisor_id=eq." + supervisorId + "&order=proposed_time.desc"))
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

    public UUID insert(MeetingRequest m, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        if (m.getProposedTime() != null) json.addProperty("proposed_time", m.getProposedTime().toString());
        json.addProperty("location", m.getLocation());
        json.addProperty("agenda", m.getAgenda());
        json.addProperty("status", MeetingStatus.REQUESTED.name());
        json.addProperty("student_id", m.getStudentId().toString());
        json.addProperty("supervisor_id", m.getSupervisorId().toString());

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/meeting_requests?select=request_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);

        if (response.statusCode() == 201) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return UUID.fromString(array.get(0).getAsJsonObject().get("request_id").getAsString());
        }
        throw new Exception("Failed to insert meeting request: " + response.body());
    }

    public void updateStatus(UUID requestId, MeetingStatus status, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("status", status.name());

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/meeting_requests?request_id=eq." + requestId))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    public void reschedule(UUID requestId, LocalDateTime counterTime, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("status", "RESCHEDULED");
        if (counterTime != null) json.addProperty("counter_time", counterTime.toString());

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/meeting_requests?request_id=eq." + requestId))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    private MeetingRequest mapRow(JsonObject obj) {
        MeetingRequest m = new MeetingRequest();
        m.setRequestId(UUID.fromString(obj.get("request_id").getAsString()));
        if (obj.has("proposed_time") && !obj.get("proposed_time").isJsonNull()) {
            m.setProposedTime(LocalDateTime.parse(obj.get("proposed_time").getAsString(), DateTimeFormatter.ISO_DATE_TIME));
        }
        m.setLocation(obj.has("location") && !obj.get("location").isJsonNull() ? obj.get("location").getAsString() : "");
        m.setAgenda(obj.has("agenda") && !obj.get("agenda").isJsonNull() ? obj.get("agenda").getAsString() : "");
        m.setStatus(MeetingStatus.valueOf(obj.get("status").getAsString()));
        m.setStudentId(UUID.fromString(obj.get("student_id").getAsString()));
        m.setSupervisorId(UUID.fromString(obj.get("supervisor_id").getAsString()));
        if (obj.has("counter_time") && !obj.get("counter_time").isJsonNull()) {
            m.setCounterTime(LocalDateTime.parse(obj.get("counter_time").getAsString(), DateTimeFormatter.ISO_DATE_TIME));
        }
        return m;
    }
}
