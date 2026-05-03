package com.fyp.dao;

import com.fyp.model.Notification;
import com.fyp.enums.NotificationType;
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

public class NotificationDAO {

    public List<Notification> findByRecipient(UUID recipientId, String jwt) throws Exception {
        List<Notification> list = new ArrayList<>();
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/notifications?recipient_id=eq." + recipientId + "&order=created_at.desc"))
                .GET();
        HttpResponse<String> res = SupabaseClient.sendAuthenticatedRequest(req, jwt);
        if (res.statusCode() == 200 && !res.body().equals("[]")) {
            for (JsonElement el : JsonParser.parseString(res.body()).getAsJsonArray()) list.add(mapRow(el.getAsJsonObject()));
        }
        return list;
    }

    public List<Notification> findUnread(UUID recipientId, String jwt) throws Exception {
        List<Notification> list = new ArrayList<>();
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/notifications?recipient_id=eq." + recipientId + "&is_read=eq.false&order=created_at.desc"))
                .GET();
        HttpResponse<String> res = SupabaseClient.sendAuthenticatedRequest(req, jwt);
        if (res.statusCode() == 200 && !res.body().equals("[]")) {
            for (JsonElement el : JsonParser.parseString(res.body()).getAsJsonArray()) list.add(mapRow(el.getAsJsonObject()));
        }
        return list;
    }

    public int countUnread(UUID recipientId, String jwt) throws Exception {
        return findUnread(recipientId, jwt).size();
    }

    public UUID insert(String message, NotificationType type, UUID recipientId, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("message", message);
        json.addProperty("type", type.name());
        json.addProperty("recipient_id", recipientId.toString());
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/notifications?select=notif_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> res = SupabaseClient.sendAuthenticatedRequest(req, jwt);
        if (res.statusCode() == 201) {
            return UUID.fromString(JsonParser.parseString(res.body()).getAsJsonArray().get(0).getAsJsonObject().get("notif_id").getAsString());
        }
        throw new Exception("Failed to insert notification: " + res.body());
    }

    public void markAsRead(UUID notifId, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("is_read", true);
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/notifications?notif_id=eq." + notifId))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(req, jwt);
    }

    public void markAllAsRead(UUID recipientId, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("is_read", true);
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/notifications?recipient_id=eq." + recipientId))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(req, jwt);
    }

    private Notification mapRow(JsonObject obj) {
        Notification n = new Notification();
        n.setNotifId(UUID.fromString(obj.get("notif_id").getAsString()));
        n.setMessage(obj.has("message") && !obj.get("message").isJsonNull() ? obj.get("message").getAsString() : "");
        n.setType(NotificationType.valueOf(obj.get("type").getAsString()));
        n.setRead(obj.has("is_read") && obj.get("is_read").getAsBoolean());
        n.setRecipientId(UUID.fromString(obj.get("recipient_id").getAsString()));
        if (obj.has("created_at") && !obj.get("created_at").isJsonNull()) {
            n.setCreatedAt(LocalDateTime.parse(obj.get("created_at").getAsString(), DateTimeFormatter.ISO_DATE_TIME));
        }
        return n;
    }
}
