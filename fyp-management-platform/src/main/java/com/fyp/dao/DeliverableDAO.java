package com.fyp.dao;

import com.fyp.model.Deliverable;
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

public class DeliverableDAO {

    public List<Deliverable> findByMilestoneId(UUID milestoneId, String jwt) throws Exception {
        List<Deliverable> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/deliverables?milestone_id=eq." + milestoneId + "&order=upload_timestamp.desc"))
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

    public List<Deliverable> findByStudentId(UUID studentId, String jwt) throws Exception {
        List<Deliverable> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/deliverables?student_id=eq." + studentId + "&order=upload_timestamp.desc"))
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

    public UUID insert(Deliverable d, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("file_name", d.getFileName());
        json.addProperty("file_type", d.getFileType());
        json.addProperty("file_path", d.getFilePath());
        json.addProperty("milestone_id", d.getMilestoneId().toString());
        json.addProperty("student_id", d.getStudentId().toString());

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/deliverables?select=file_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 201) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return UUID.fromString(array.get(0).getAsJsonObject().get("file_id").getAsString());
        }
        throw new Exception("Failed to insert deliverable: " + response.body());
    }

    public void delete(UUID fileId, String jwt) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/deliverables?file_id=eq." + fileId))
                .DELETE();
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    private Deliverable mapRow(JsonObject obj) {
        Deliverable d = new Deliverable();
        d.setFileId(UUID.fromString(obj.get("file_id").getAsString()));
        d.setFileName(obj.has("file_name") && !obj.get("file_name").isJsonNull() ? obj.get("file_name").getAsString() : "");
        d.setFileType(obj.has("file_type") && !obj.get("file_type").isJsonNull() ? obj.get("file_type").getAsString() : "");
        d.setFilePath(obj.has("file_path") && !obj.get("file_path").isJsonNull() ? obj.get("file_path").getAsString() : "");
        d.setMilestoneId(UUID.fromString(obj.get("milestone_id").getAsString()));
        d.setStudentId(UUID.fromString(obj.get("student_id").getAsString()));
        if (obj.has("upload_timestamp") && !obj.get("upload_timestamp").isJsonNull()) {
            d.setUploadTimestamp(LocalDateTime.parse(obj.get("upload_timestamp").getAsString(), DateTimeFormatter.ISO_DATE_TIME));
        }
        return d;
    }
}
