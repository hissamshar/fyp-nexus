package com.fyp.dao;

import com.fyp.model.SemesterDeadline;
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

public class SemesterDeadlineDAO {

    public List<SemesterDeadline> findAll(String jwt) throws Exception {
        List<SemesterDeadline> list = new ArrayList<>();
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/semester_deadlines?order=due_date.asc"))
                .GET();
        HttpResponse<String> res = SupabaseClient.sendAuthenticatedRequest(req, jwt);
        if (res.statusCode() == 200 && !res.body().equals("[]")) {
            for (JsonElement el : JsonParser.parseString(res.body()).getAsJsonArray()) list.add(mapRow(el.getAsJsonObject()));
        }
        return list;
    }

    public List<SemesterDeadline> findBySemester(String semester, String jwt) throws Exception {
        List<SemesterDeadline> list = new ArrayList<>();
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/semester_deadlines?semester=eq." + semester + "&order=due_date.asc"))
                .GET();
        HttpResponse<String> res = SupabaseClient.sendAuthenticatedRequest(req, jwt);
        if (res.statusCode() == 200 && !res.body().equals("[]")) {
            for (JsonElement el : JsonParser.parseString(res.body()).getAsJsonArray()) list.add(mapRow(el.getAsJsonObject()));
        }
        return list;
    }

    public UUID insert(SemesterDeadline d, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("semester", d.getSemester());
        json.addProperty("deadline_type", d.getDeadlineType());
        if (d.getDueDate() != null) json.addProperty("due_date", d.getDueDate().toString());
        json.addProperty("description", d.getDescription());
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/semester_deadlines?select=deadline_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> res = SupabaseClient.sendAuthenticatedRequest(req, jwt);
        if (res.statusCode() == 201) {
            return UUID.fromString(JsonParser.parseString(res.body()).getAsJsonArray().get(0).getAsJsonObject().get("deadline_id").getAsString());
        }
        throw new Exception("Failed to insert deadline: " + res.body());
    }

    public void update(SemesterDeadline d, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("semester", d.getSemester());
        json.addProperty("deadline_type", d.getDeadlineType());
        if (d.getDueDate() != null) json.addProperty("due_date", d.getDueDate().toString());
        json.addProperty("description", d.getDescription());
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/semester_deadlines?deadline_id=eq." + d.getDeadlineId()))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(req, jwt);
    }

    public void delete(UUID deadlineId, String jwt) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/semester_deadlines?deadline_id=eq." + deadlineId))
                .DELETE();
        SupabaseClient.sendAuthenticatedRequest(req, jwt);
    }

    private SemesterDeadline mapRow(JsonObject obj) {
        SemesterDeadline d = new SemesterDeadline();
        d.setDeadlineId(UUID.fromString(obj.get("deadline_id").getAsString()));
        d.setSemester(obj.has("semester") && !obj.get("semester").isJsonNull() ? obj.get("semester").getAsString() : "");
        d.setDeadlineType(obj.has("deadline_type") && !obj.get("deadline_type").isJsonNull() ? obj.get("deadline_type").getAsString() : "");
        if (obj.has("due_date") && !obj.get("due_date").isJsonNull()) {
            d.setDueDate(LocalDateTime.parse(obj.get("due_date").getAsString(), DateTimeFormatter.ISO_DATE_TIME));
        }
        d.setDescription(obj.has("description") && !obj.get("description").isJsonNull() ? obj.get("description").getAsString() : "");
        return d;
    }
}
