package com.fyp.dao;

import com.fyp.model.Rubric;
import com.fyp.model.RubricCriterion;
import com.fyp.util.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class RubricDAO {

    public Optional<Rubric> findById(UUID rubricId, String jwt) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/rubrics?rubric_id=eq." + rubricId))
                .GET();
        HttpResponse<String> res = SupabaseClient.sendAuthenticatedRequest(req, jwt);
        if (res.statusCode() == 200 && !res.body().equals("[]")) {
            Rubric r = mapRubric(JsonParser.parseString(res.body()).getAsJsonArray().get(0).getAsJsonObject());
            r.setCriteria(findCriteriaByRubricId(rubricId, jwt));
            return Optional.of(r);
        }
        return Optional.empty();
    }

    public List<Rubric> findAll(String jwt) throws Exception {
        List<Rubric> list = new ArrayList<>();
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/rubrics?order=created_at.desc"))
                .GET();
        HttpResponse<String> res = SupabaseClient.sendAuthenticatedRequest(req, jwt);
        if (res.statusCode() == 200 && !res.body().equals("[]")) {
            for (JsonElement el : JsonParser.parseString(res.body()).getAsJsonArray()) {
                list.add(mapRubric(el.getAsJsonObject()));
            }
        }
        return list;
    }

    public UUID insertRubric(String name, int totalPoints, String version, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("total_points", totalPoints);
        json.addProperty("version", version);
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/rubrics?select=rubric_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> res = SupabaseClient.sendAuthenticatedRequest(req, jwt);
        if (res.statusCode() == 201) {
            return UUID.fromString(JsonParser.parseString(res.body()).getAsJsonArray().get(0).getAsJsonObject().get("rubric_id").getAsString());
        }
        throw new Exception("Failed to insert rubric: " + res.body());
    }

    public UUID insertCriterion(RubricCriterion c, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("criterion_name", c.getCriterionName());
        json.addProperty("max_score", c.getMaxScore());
        json.addProperty("description", c.getDescription());
        json.addProperty("rubric_id", c.getRubricId().toString());
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/rubric_criteria?select=criterion_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> res = SupabaseClient.sendAuthenticatedRequest(req, jwt);
        if (res.statusCode() == 201) {
            return UUID.fromString(JsonParser.parseString(res.body()).getAsJsonArray().get(0).getAsJsonObject().get("criterion_id").getAsString());
        }
        throw new Exception("Failed to insert criterion: " + res.body());
    }

    public List<RubricCriterion> findCriteriaByRubricId(UUID rubricId, String jwt) throws Exception {
        List<RubricCriterion> list = new ArrayList<>();
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/rubric_criteria?rubric_id=eq." + rubricId + "&order=criterion_name.asc"))
                .GET();
        HttpResponse<String> res = SupabaseClient.sendAuthenticatedRequest(req, jwt);
        if (res.statusCode() == 200 && !res.body().equals("[]")) {
            for (JsonElement el : JsonParser.parseString(res.body()).getAsJsonArray()) {
                JsonObject obj = el.getAsJsonObject();
                RubricCriterion c = new RubricCriterion();
                c.setCriterionId(UUID.fromString(obj.get("criterion_id").getAsString()));
                c.setCriterionName(obj.has("criterion_name") && !obj.get("criterion_name").isJsonNull() ? obj.get("criterion_name").getAsString() : "");
                c.setMaxScore(obj.has("max_score") && !obj.get("max_score").isJsonNull() ? obj.get("max_score").getAsInt() : 0);
                c.setDescription(obj.has("description") && !obj.get("description").isJsonNull() ? obj.get("description").getAsString() : "");
                c.setRubricId(rubricId);
                list.add(c);
            }
        }
        return list;
    }

    public Optional<Rubric> findByProjectId(UUID projectId, String jwt) throws Exception {
        // Find rubric assigned to project
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/rubrics?project_id=eq." + projectId))
                .GET();
        HttpResponse<String> res = SupabaseClient.sendAuthenticatedRequest(req, jwt);
        if (res.statusCode() == 200 && !res.body().equals("[]")) {
            Rubric r = mapRubric(JsonParser.parseString(res.body()).getAsJsonArray().get(0).getAsJsonObject());
            r.setCriteria(findCriteriaByRubricId(r.getRubricId(), jwt));
            return Optional.of(r);
        }
        return Optional.empty();
    }

    private Rubric mapRubric(JsonObject obj) {
        Rubric r = new Rubric();
        r.setRubricId(UUID.fromString(obj.get("rubric_id").getAsString()));
        r.setTotalPoints(obj.has("total_points") && !obj.get("total_points").isJsonNull() ? obj.get("total_points").getAsInt() : 0);
        r.setVersion(obj.has("version") && !obj.get("version").isJsonNull() ? obj.get("version").getAsString() : "");
        r.setName(obj.has("name") && !obj.get("name").isJsonNull() ? obj.get("name").getAsString() : "");
        return r;
    }
}
