package com.fyp.dao;

import com.fyp.model.Grade;
import com.fyp.model.GradeEntry;
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

public class GradeDAO {

    public Optional<Grade> findByProjectId(UUID projectId, String jwt) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/grades?project_id=eq." + projectId))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);

        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return Optional.of(mapGrade(array.get(0).getAsJsonObject()));
        }
        return Optional.empty();
    }

    public UUID insertGrade(UUID projectId, UUID rubricId, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("project_id", projectId.toString());
        if (rubricId != null) json.addProperty("rubric_id", rubricId.toString());
        json.addProperty("is_published", false);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/grades?select=grade_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);

        if (response.statusCode() == 201) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return UUID.fromString(array.get(0).getAsJsonObject().get("grade_id").getAsString());
        }
        throw new Exception("Failed to insert grade: " + response.body());
    }

    public void publishGrade(UUID gradeId, String letterGrade, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("is_published", true);
        json.addProperty("letter_grade", letterGrade);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/grades?grade_id=eq." + gradeId))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    public UUID insertGradeEntry(GradeEntry entry, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("score", entry.getScore());
        json.addProperty("comments", entry.getComments());
        json.addProperty("grade_id", entry.getGradeId().toString());
        json.addProperty("grader_id", entry.getGraderId().toString());
        if (entry.getCriterionId() != null) json.addProperty("criterion_id", entry.getCriterionId().toString());

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/grade_entries?select=entry_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);

        if (response.statusCode() == 201) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return UUID.fromString(array.get(0).getAsJsonObject().get("entry_id").getAsString());
        }
        throw new Exception("Failed to insert grade entry: " + response.body());
    }

    public List<GradeEntry> findEntriesByGradeId(UUID gradeId, String jwt) throws Exception {
        List<GradeEntry> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/grade_entries?grade_id=eq." + gradeId + "&order=graded_at.desc"))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);

        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                GradeEntry e = new GradeEntry();
                e.setEntryId(UUID.fromString(obj.get("entry_id").getAsString()));
                e.setScore(obj.get("score").getAsInt());
                e.setComments(obj.has("comments") && !obj.get("comments").isJsonNull() ? obj.get("comments").getAsString() : "");
                if (obj.has("graded_at") && !obj.get("graded_at").isJsonNull()) {
                    e.setGradedAt(LocalDateTime.parse(obj.get("graded_at").getAsString(), DateTimeFormatter.ISO_DATE_TIME));
                }
                e.setGradeId(UUID.fromString(obj.get("grade_id").getAsString()));
                e.setGraderId(UUID.fromString(obj.get("grader_id").getAsString()));
                if (obj.has("criterion_id") && !obj.get("criterion_id").isJsonNull()) {
                    e.setCriterionId(UUID.fromString(obj.get("criterion_id").getAsString()));
                }
                list.add(e);
            }
        }
        return list;
    }

    public double calculateWeightedTotal(UUID gradeId, String jwt) throws Exception {
        List<GradeEntry> entries = findEntriesByGradeId(gradeId, jwt);
        double total = 0;
        for (GradeEntry e : entries) {
            total += e.getScore();
        }
        return total;
    }

    private Grade mapGrade(JsonObject obj) {
        Grade g = new Grade();
        g.setGradeId(UUID.fromString(obj.get("grade_id").getAsString()));
        g.setLetterGrade(obj.has("letter_grade") && !obj.get("letter_grade").isJsonNull() ? obj.get("letter_grade").getAsString() : null);
        g.setPublished(obj.has("is_published") && obj.get("is_published").getAsBoolean());
        g.setProjectId(UUID.fromString(obj.get("project_id").getAsString()));
        if (obj.has("rubric_id") && !obj.get("rubric_id").isJsonNull()) {
            g.setRubricId(UUID.fromString(obj.get("rubric_id").getAsString()));
        }
        return g;
    }
}
