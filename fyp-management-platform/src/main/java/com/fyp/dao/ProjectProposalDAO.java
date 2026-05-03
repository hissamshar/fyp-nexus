package com.fyp.dao;

import com.fyp.model.ProjectProposal;
import com.fyp.enums.ProposalStatus;
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

public class ProjectProposalDAO {

    public Optional<ProjectProposal> findById(UUID proposalId, String jwt) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/project_proposals?proposal_id=eq." + proposalId))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return Optional.of(mapRow(array.get(0).getAsJsonObject()));
        }
        return Optional.empty();
    }

    public List<ProjectProposal> findByStudentId(UUID studentId, String jwt) throws Exception {
        List<ProjectProposal> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/project_proposals?student_id=eq." + studentId + "&order=created_at.desc"))
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

    public List<ProjectProposal> findBySupervisorId(UUID supervisorId, String jwt) throws Exception {
        List<ProjectProposal> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/project_proposals?select=*,students(users(name))&supervisor_id=eq." + supervisorId + "&order=created_at.desc"))
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

    public List<ProjectProposal> findByStatus(ProposalStatus status, String jwt) throws Exception {
        List<ProjectProposal> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/project_proposals?status=eq." + status.name() + "&order=created_at.desc"))
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

    public List<ProjectProposal> findAll(String jwt) throws Exception {
        List<ProjectProposal> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/project_proposals?order=created_at.desc"))
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

    public UUID insert(ProjectProposal p, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("title", p.getTitle());
        json.addProperty("abstract", p.getAbstract());
        json.addProperty("objectives", p.getObjectives());
        json.addProperty("methodology", p.getMethodology());
        json.addProperty("expected_outcomes", p.getExpectedOutcome());
        json.addProperty("description", p.getDescription());
        if (p.getSubmissionDate() != null) json.addProperty("submission_date", p.getSubmissionDate().toString());
        json.addProperty("status", p.getStatus().name());
        json.addProperty("student_id", p.getStudentId().toString());
        if (p.getSupervisorId() != null) json.addProperty("supervisor_id", p.getSupervisorId().toString());

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/project_proposals?select=proposal_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 201) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return UUID.fromString(array.get(0).getAsJsonObject().get("proposal_id").getAsString());
        }
        throw new Exception("Failed to insert proposal: " + response.body());
    }

    public void updateStatus(UUID proposalId, ProposalStatus status, String rejectionComment, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("status", status.name());
        json.addProperty("rejection_comment", rejectionComment);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/project_proposals?proposal_id=eq." + proposalId))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    public void update(ProjectProposal p, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("title", p.getTitle());
        json.addProperty("abstract", p.getAbstract());
        json.addProperty("objectives", p.getObjectives());
        json.addProperty("methodology", p.getMethodology());
        json.addProperty("expected_outcomes", p.getExpectedOutcome());
        json.addProperty("description", p.getDescription());
        json.addProperty("status", p.getStatus().name());
        if (p.getSupervisorId() != null) json.addProperty("supervisor_id", p.getSupervisorId().toString());

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/project_proposals?proposal_id=eq." + p.getProposalId()))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    private ProjectProposal mapRow(JsonObject obj) {
        ProjectProposal p = new ProjectProposal();
        p.setProposalId(UUID.fromString(obj.get("proposal_id").getAsString()));
        p.setTitle(obj.has("title") && !obj.get("title").isJsonNull() ? obj.get("title").getAsString() : "");
        p.setAbstract(obj.has("abstract") && !obj.get("abstract").isJsonNull() ? obj.get("abstract").getAsString() : "");
        p.setObjectives(obj.has("objectives") && !obj.get("objectives").isJsonNull() ? obj.get("objectives").getAsString() : "");
        p.setMethodology(obj.has("methodology") && !obj.get("methodology").isJsonNull() ? obj.get("methodology").getAsString() : "");
        p.setExpectedOutcome(obj.has("expected_outcomes") && !obj.get("expected_outcomes").isJsonNull() ? obj.get("expected_outcomes").getAsString() : "");
        p.setDescription(obj.has("description") && !obj.get("description").isJsonNull() ? obj.get("description").getAsString() : "");
        if (obj.has("submission_date") && !obj.get("submission_date").isJsonNull()) {
            p.setSubmissionDate(LocalDate.parse(obj.get("submission_date").getAsString()));
        }
        p.setStatus(ProposalStatus.valueOf(obj.get("status").getAsString()));
        p.setRejectionComment(obj.has("rejection_comment") && !obj.get("rejection_comment").isJsonNull() ? obj.get("rejection_comment").getAsString() : null);
        p.setStudentId(UUID.fromString(obj.get("student_id").getAsString()));
        if (obj.has("supervisor_id") && !obj.get("supervisor_id").isJsonNull()) {
            p.setSupervisorId(UUID.fromString(obj.get("supervisor_id").getAsString()));
        }
        
        // Extract student name if joined
        if (obj.has("students") && !obj.get("students").isJsonNull()) {
            JsonObject studentObj = obj.getAsJsonObject("students");
            if (studentObj.has("users") && !studentObj.get("users").isJsonNull()) {
                p.setStudentName(studentObj.getAsJsonObject("users").get("name").getAsString());
            }
        }
        return p;
    }
}
