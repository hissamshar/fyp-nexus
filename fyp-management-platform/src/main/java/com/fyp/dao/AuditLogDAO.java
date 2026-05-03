package com.fyp.dao;

import com.fyp.model.AuditLogEntry;
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

public class AuditLogDAO {

    public List<AuditLogEntry> findAll(int limit, String jwt) throws Exception {
        List<AuditLogEntry> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/audit_logs?order=timestamp.desc&limit=" + limit))
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

    public List<AuditLogEntry> findByUserId(UUID userId, int limit, String jwt) throws Exception {
        List<AuditLogEntry> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/audit_logs?user_id=eq." + userId + "&order=timestamp.desc&limit=" + limit))
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

    public List<AuditLogEntry> findByAction(String action, String jwt) throws Exception {
        List<AuditLogEntry> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/audit_logs?action=eq." + action + "&order=timestamp.desc&limit=500"))
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

    private AuditLogEntry mapRow(JsonObject obj) {
        AuditLogEntry e = new AuditLogEntry();
        e.setLogId(UUID.fromString(obj.get("log_id").getAsString()));
        if (obj.has("user_id") && !obj.get("user_id").isJsonNull()) {
            e.setUserId(UUID.fromString(obj.get("user_id").getAsString()));
        }
        e.setAction(obj.has("action") && !obj.get("action").isJsonNull() ? obj.get("action").getAsString() : "");
        e.setIpAddress(obj.has("ip_address") && !obj.get("ip_address").isJsonNull() ? obj.get("ip_address").getAsString() : null);
        if (obj.has("timestamp") && !obj.get("timestamp").isJsonNull()) {
            e.setTimestamp(LocalDateTime.parse(obj.get("timestamp").getAsString(), DateTimeFormatter.ISO_DATE_TIME));
        }
        e.setDetails(obj.has("details") && !obj.get("details").isJsonNull() ? obj.get("details").getAsString() : null);
        return e;
    }
}
