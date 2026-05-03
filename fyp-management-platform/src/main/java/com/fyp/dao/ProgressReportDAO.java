package com.fyp.dao;

import com.fyp.model.ProgressReport;
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

public class ProgressReportDAO {

    public List<ProgressReport> findByProjectId(UUID projectId, String jwt) throws Exception {
        List<ProgressReport> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/progress_reports?project_id=eq." + projectId + "&order=submitted_at.desc"))
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

    public UUID insert(ProgressReport r, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("work_done", r.getWorkDone());
        json.addProperty("issues_faced", r.getIssuesFaced());
        json.addProperty("next_steps", r.getNextSteps());
        json.addProperty("project_id", r.getProjectId().toString());
        json.addProperty("student_id", r.getStudentId().toString());

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/progress_reports?select=report_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 201) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return UUID.fromString(array.get(0).getAsJsonObject().get("report_id").getAsString());
        }
        throw new Exception("Failed to insert progress report: " + response.body());
    }

    private ProgressReport mapRow(JsonObject obj) {
        ProgressReport r = new ProgressReport();
        r.setReportId(UUID.fromString(obj.get("report_id").getAsString()));
        r.setWorkDone(obj.has("work_done") && !obj.get("work_done").isJsonNull() ? obj.get("work_done").getAsString() : "");
        r.setIssuesFaced(obj.has("issues_faced") && !obj.get("issues_faced").isJsonNull() ? obj.get("issues_faced").getAsString() : "");
        r.setNextSteps(obj.has("next_steps") && !obj.get("next_steps").isJsonNull() ? obj.get("next_steps").getAsString() : "");
        if (obj.has("submitted_at") && !obj.get("submitted_at").isJsonNull()) {
            r.setSubmittedAt(LocalDateTime.parse(obj.get("submitted_at").getAsString(), DateTimeFormatter.ISO_DATE_TIME));
        }
        r.setProjectId(UUID.fromString(obj.get("project_id").getAsString()));
        r.setStudentId(UUID.fromString(obj.get("student_id").getAsString()));
        return r;
    }

    public List<ProgressReport> findByProjectUser(UUID studentId, String jwt) throws Exception {
        List<ProgressReport> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/progress_reports?student_id=eq." + studentId + "&order=submitted_at.desc"))
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
