package com.fyp.dao;

import com.fyp.model.Project;
import com.fyp.enums.ProjectStatus;
import com.fyp.util.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.*;

public class ProjectDAO {

    public Optional<Project> findById(UUID projectId, String jwt) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/projects?project_id=eq." + projectId))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return Optional.of(mapRow(array.get(0).getAsJsonObject()));
        }
        return Optional.empty();
    }

    public Optional<Project> findByProposalId(UUID proposalId, String jwt) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/projects?proposal_id=eq." + proposalId))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return Optional.of(mapRow(array.get(0).getAsJsonObject()));
        }
        return Optional.empty();
    }

    public List<Project> findAll(String jwt) throws Exception {
        List<Project> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/projects?order=created_at.desc"))
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

    public List<Project> findByStatus(ProjectStatus status, String jwt) throws Exception {
        List<Project> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/projects?status=eq." + status.name() + "&order=created_at.desc"))
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

    public List<Project> findBySupervisorId(UUID supervisorId, String jwt) throws Exception {
        List<Project> list = new ArrayList<>();
        // In REST, we join supervisor_assignments
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/supervisor_assignments?select=projects(*)&supervisor_id=eq." + supervisorId))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            for (JsonElement el : array) {
                JsonObject assignmentObj = el.getAsJsonObject();
                if (assignmentObj.has("projects") && !assignmentObj.get("projects").isJsonNull()) {
                    list.add(mapRow(assignmentObj.getAsJsonObject("projects")));
                }
            }
        }
        // Client side sort since the inner join makes it hard to sort by project created_at
        list.sort(Comparator.comparing(Project::getStartDate).reversed());
        return list;
    }

    public UUID insert(UUID proposalId, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("proposal_id", proposalId.toString());
        json.addProperty("start_date", LocalDate.now().toString());
        json.addProperty("status", "INITIATED");

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/projects?select=project_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 201) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return UUID.fromString(array.get(0).getAsJsonObject().get("project_id").getAsString());
        }
        throw new Exception("Failed to insert project: " + response.body());
    }

    public void updateStatus(UUID projectId, ProjectStatus status, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("status", status.name());

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/projects?project_id=eq." + projectId))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    public void updateRepoUrl(UUID projectId, String repoUrl, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("repo_url", repoUrl);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/projects?project_id=eq." + projectId))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    public Map<String, Integer> countByStatus(String jwt) throws Exception {
        Map<String, Integer> map = new LinkedHashMap<>();
        // In REST API without an RPC to do group by, we can just fetch all and aggregate locally
        // Or if performance matters, use an RPC. Local aggregation for MVP:
        List<Project> all = findAll(jwt);
        for (Project p : all) {
            map.put(p.getStatus().name(), map.getOrDefault(p.getStatus().name(), 0) + 1);
        }
        return map;
    }

    public void linkIndustryProblem(UUID projectId, UUID problemId, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("problem_id", problemId.toString());

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/projects?project_id=eq." + projectId))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    private Project mapRow(JsonObject obj) {
        Project p = new Project();
        p.setProjectId(UUID.fromString(obj.get("project_id").getAsString()));
        if (obj.has("start_date") && !obj.get("start_date").isJsonNull()) {
            p.setStartDate(LocalDate.parse(obj.get("start_date").getAsString()));
        }
        if (obj.has("end_date") && !obj.get("end_date").isJsonNull()) {
            p.setEndDate(LocalDate.parse(obj.get("end_date").getAsString()));
        }
        p.setRepoUrl(obj.has("repo_url") && !obj.get("repo_url").isJsonNull() ? obj.get("repo_url").getAsString() : null);
        p.setStatus(ProjectStatus.valueOf(obj.get("status").getAsString()));
        p.setProposalId(UUID.fromString(obj.get("proposal_id").getAsString()));
        return p;
    }
}
