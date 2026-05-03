package com.fyp.dao;

import com.fyp.model.IndustryProblem;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IndustryProblemDAO {

    public List<IndustryProblem> findAll(String jwt) throws Exception {
        List<IndustryProblem> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/industry_problems?is_active=eq.true&order=created_at.desc"))
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

    public List<IndustryProblem> findByPartnerId(UUID partnerId, String jwt) throws Exception {
        List<IndustryProblem> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/industry_problems?partner_id=eq." + partnerId + "&order=created_at.desc"))
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

    public UUID insert(IndustryProblem p, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("title", p.getTitle());
        json.addProperty("description", p.getDescription());
        json.addProperty("domain", p.getDomain());
        json.addProperty("contact_email", p.getContactEmail());
        json.addProperty("partner_id", p.getPartnerId().toString());

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/industry_problems?select=problem_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 201) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return UUID.fromString(array.get(0).getAsJsonObject().get("problem_id").getAsString());
        }
        throw new Exception("Failed to insert problem: " + response.body());
    }

    public void adopt(UUID problemId, UUID studentId, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("adopted_by", studentId.toString());

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/industry_problems?problem_id=eq." + problemId))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    public List<IndustryProblem> searchByKeyword(String keyword, String jwt) throws Exception {
        List<IndustryProblem> list = new ArrayList<>();
        // PostgREST ilike operator
        String encodedKw = URLEncoder.encode("*" + keyword + "*", StandardCharsets.UTF_8);
        String filter = "or=(title.ilike." + encodedKw + ",description.ilike." + encodedKw + ",domain.ilike." + encodedKw + ")";
        
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/industry_problems?is_active=eq.true&" + filter + "&order=created_at.desc"))
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

    private IndustryProblem mapRow(JsonObject obj) {
        IndustryProblem p = new IndustryProblem();
        p.setProblemId(UUID.fromString(obj.get("problem_id").getAsString()));
        p.setTitle(obj.has("title") && !obj.get("title").isJsonNull() ? obj.get("title").getAsString() : "");
        p.setDescription(obj.has("description") && !obj.get("description").isJsonNull() ? obj.get("description").getAsString() : "");
        p.setDomain(obj.has("domain") && !obj.get("domain").isJsonNull() ? obj.get("domain").getAsString() : "");
        p.setContactEmail(obj.has("contact_email") && !obj.get("contact_email").isJsonNull() ? obj.get("contact_email").getAsString() : "");
        p.setPartnerId(UUID.fromString(obj.get("partner_id").getAsString()));
        if (obj.has("adopted_by") && !obj.get("adopted_by").isJsonNull()) {
            p.setAdoptedBy(UUID.fromString(obj.get("adopted_by").getAsString()));
        }
        p.setActive(obj.has("is_active") && obj.get("is_active").getAsBoolean());
        return p;
    }
}
