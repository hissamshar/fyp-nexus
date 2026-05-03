package com.fyp.dao;

import com.fyp.model.Student;
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

public class StudentDAO {

    public Optional<Student> findByUserId(UUID userId, String jwt) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/students?select=student_id,department,cgpa,users(user_id,name,email,is_active)&user_id=eq." + userId))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return Optional.of(mapRow(array.get(0).getAsJsonObject()));
        }
        return Optional.empty();
    }

    public Optional<Student> findByStudentId(UUID studentId, String jwt) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/students?select=student_id,department,cgpa,users(user_id,name,email,is_active)&student_id=eq." + studentId))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return Optional.of(mapRow(array.get(0).getAsJsonObject()));
        }
        return Optional.empty();
    }

    public List<Student> findAll(String jwt) throws Exception {
        List<Student> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/students?select=student_id,department,cgpa,users(user_id,name,email,is_active)"))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            for (JsonElement el : array) {
                list.add(mapRow(el.getAsJsonObject()));
            }
        }
        return list;
    }

    public UUID insert(UUID userId, String department, double cgpa, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("user_id", userId.toString());
        json.addProperty("department", department);
        json.addProperty("cgpa", cgpa);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/students?select=student_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 201) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return UUID.fromString(array.get(0).getAsJsonObject().get("student_id").getAsString());
        }
        throw new Exception("Failed to insert student: " + response.body());
    }

    public void update(UUID studentId, String department, double cgpa, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("department", department);
        json.addProperty("cgpa", cgpa);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/students?student_id=eq." + studentId))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    private Student mapRow(JsonObject obj) {
        JsonObject userObj = obj.getAsJsonObject("users");
        return new Student(
            UUID.fromString(userObj.get("user_id").getAsString()),
            userObj.has("name") && !userObj.get("name").isJsonNull() ? userObj.get("name").getAsString() : "",
            userObj.has("email") && !userObj.get("email").isJsonNull() ? userObj.get("email").getAsString() : "",
            "", // no password hash
            userObj.has("is_active") && userObj.get("is_active").getAsBoolean(),
            true, // email verification owned by Supabase Auth
            UUID.fromString(obj.get("student_id").getAsString()),
            obj.has("department") && !obj.get("department").isJsonNull() ? obj.get("department").getAsString() : "",
            obj.has("cgpa") && !obj.get("cgpa").isJsonNull() ? obj.get("cgpa").getAsDouble() : 0.0
        );
    }
}
