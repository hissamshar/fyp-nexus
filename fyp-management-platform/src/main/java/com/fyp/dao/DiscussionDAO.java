package com.fyp.dao;

import com.fyp.model.DiscussionBoard;
import com.fyp.model.DiscussionThread;
import com.fyp.model.Post;
import com.fyp.util.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DiscussionDAO {

    // ── Board ──────────────────────────────────────────────────────────────────

    public Optional<DiscussionBoard> findBoardByProjectId(UUID projectId, String jwt) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/discussion_boards?project_id=eq." + projectId))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            JsonObject obj = array.get(0).getAsJsonObject();
            DiscussionBoard b = new DiscussionBoard();
            b.setBoardId(UUID.fromString(obj.get("board_id").getAsString()));
            b.setPrivate(obj.has("is_private") && obj.get("is_private").getAsBoolean());
            b.setProjectId(projectId);
            return Optional.of(b);
        }
        return Optional.empty();
    }

    public UUID insertBoard(UUID projectId, boolean isPrivate, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("project_id", projectId.toString());
        json.addProperty("is_private", isPrivate);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/discussion_boards?select=board_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 201) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return UUID.fromString(array.get(0).getAsJsonObject().get("board_id").getAsString());
        }
        throw new Exception("Failed to insert discussion board: " + response.body());
    }

    // ── Threads ────────────────────────────────────────────────────────────────

    public List<DiscussionThread> findThreadsByBoardId(UUID boardId, String jwt) throws Exception {
        List<DiscussionThread> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/discussion_threads?board_id=eq." + boardId + "&order=is_pinned.desc,created_at.desc"))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                DiscussionThread t = new DiscussionThread();
                t.setThreadId(UUID.fromString(obj.get("thread_id").getAsString()));
                t.setTopic(obj.has("topic") && !obj.get("topic").isJsonNull() ? obj.get("topic").getAsString() : "");
                t.setLocked(obj.has("is_locked") && obj.get("is_locked").getAsBoolean());
                t.setPinned(obj.has("is_pinned") && obj.get("is_pinned").getAsBoolean());
                t.setBoardId(boardId);
                list.add(t);
            }
        }
        return list;
    }

    public UUID insertThread(UUID boardId, String topic, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("board_id", boardId.toString());
        json.addProperty("topic", topic);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/discussion_threads?select=thread_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 201) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return UUID.fromString(array.get(0).getAsJsonObject().get("thread_id").getAsString());
        }
        throw new Exception("Failed to insert thread: " + response.body());
    }

    public void lockThread(UUID threadId, boolean locked, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("is_locked", locked);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/discussion_threads?thread_id=eq." + threadId))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    public void pinThread(UUID threadId, boolean pinned, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("is_pinned", pinned);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/discussion_threads?thread_id=eq." + threadId))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    // ── Posts ──────────────────────────────────────────────────────────────────

    public List<Post> findPostsByThreadId(UUID threadId, String jwt) throws Exception {
        List<Post> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/posts?thread_id=eq." + threadId + "&is_deleted=eq.false&order=timestamp.asc"))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            for (JsonElement el : array) {
                list.add(mapPost(el.getAsJsonObject()));
            }
        }
        return list;
    }

    public List<Post> searchPosts(UUID boardId, String query, String jwt) throws Exception {
        List<Post> list = new ArrayList<>();
        String encodedKw = URLEncoder.encode("*" + query + "*", StandardCharsets.UTF_8);
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/discussion_threads?select=posts(*)&board_id=eq." + boardId))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray threadArray = JsonParser.parseString(response.body()).getAsJsonArray();
            for (JsonElement tEl : threadArray) {
                JsonObject tObj = tEl.getAsJsonObject();
                if (tObj.has("posts") && !tObj.get("posts").isJsonNull()) {
                    JsonArray postsArr = tObj.getAsJsonArray("posts");
                    for (JsonElement pEl : postsArr) {
                        JsonObject pObj = pEl.getAsJsonObject();
                        if (!pObj.get("is_deleted").getAsBoolean() && 
                            pObj.get("content").getAsString().toLowerCase().contains(query.toLowerCase())) {
                            list.add(mapPost(pObj));
                        }
                    }
                }
            }
        }
        list.sort(Comparator.comparing(Post::getTimestamp).reversed());
        return list;
    }

    public UUID insertPost(UUID threadId, UUID authorId, String content, String attachmentPath, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("thread_id", threadId.toString());
        json.addProperty("author_id", authorId.toString());
        json.addProperty("content", content);
        if (attachmentPath != null) json.addProperty("attachment_path", attachmentPath);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/posts?select=post_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 201) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return UUID.fromString(array.get(0).getAsJsonObject().get("post_id").getAsString());
        }
        throw new Exception("Failed to insert post: " + response.body());
    }

    public void softDeletePost(UUID postId, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("is_deleted", true);
        json.addProperty("content", "[deleted]");

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/posts?post_id=eq." + postId))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    private Post mapPost(JsonObject obj) {
        Post p = new Post();
        p.setPostId(UUID.fromString(obj.get("post_id").getAsString()));
        p.setContent(obj.has("content") && !obj.get("content").isJsonNull() ? obj.get("content").getAsString() : "");
        if (obj.has("timestamp") && !obj.get("timestamp").isJsonNull()) {
            p.setTimestamp(LocalDateTime.parse(obj.get("timestamp").getAsString(), DateTimeFormatter.ISO_DATE_TIME));
        }
        p.setThreadId(UUID.fromString(obj.get("thread_id").getAsString()));
        p.setAuthorId(UUID.fromString(obj.get("author_id").getAsString()));
        p.setAttachmentPath(obj.has("attachment_path") && !obj.get("attachment_path").isJsonNull() ? obj.get("attachment_path").getAsString() : null);
        p.setDeleted(obj.has("is_deleted") && obj.get("is_deleted").getAsBoolean());
        return p;
    }
}
