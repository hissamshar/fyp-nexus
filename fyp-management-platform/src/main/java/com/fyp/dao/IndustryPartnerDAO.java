package com.fyp.dao;

import com.fyp.model.IndustryPartner;
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

public class IndustryPartnerDAO {

    public Optional<IndustryPartner> findByUserId(UUID userId, String jwt) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/industry_partners?select=partner_id,company_name,contact_email,users(user_id,name,email,is_active,is_email_verified)&user_id=eq." + userId))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return Optional.of(mapRow(array.get(0).getAsJsonObject()));
        }
        return Optional.empty();
    }

    public List<IndustryPartner> findAll(String jwt) throws Exception {
        List<IndustryPartner> list = new ArrayList<>();
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/industry_partners?select=partner_id,company_name,contact_email,users(user_id,name,email,is_active,is_email_verified)"))
                .GET();
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 200 && !response.body().equals("[]")) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("users") && obj.get("users").getAsJsonObject().get("is_active").getAsBoolean()) {
                    list.add(mapRow(obj));
                }
            }
        }
        return list;
    }

    public UUID insert(UUID userId, String companyName, String contactEmail, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("user_id", userId.toString());
        json.addProperty("company_name", companyName);
        json.addProperty("contact_email", contactEmail);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/industry_partners?select=partner_id"))
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        HttpResponse<String> response = SupabaseClient.sendAuthenticatedRequest(request, jwt);
        
        if (response.statusCode() == 201) {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            return UUID.fromString(array.get(0).getAsJsonObject().get("partner_id").getAsString());
        }
        throw new Exception("Failed to insert partner: " + response.body());
    }

    private IndustryPartner mapRow(JsonObject obj) {
        JsonObject userObj = obj.getAsJsonObject("users");
        return new IndustryPartner(
            UUID.fromString(userObj.get("user_id").getAsString()),
            userObj.has("name") && !userObj.get("name").isJsonNull() ? userObj.get("name").getAsString() : "",
            userObj.has("email") && !userObj.get("email").isJsonNull() ? userObj.get("email").getAsString() : "",
            "", // no password hash
            userObj.has("is_active") && userObj.get("is_active").getAsBoolean(),
            userObj.has("is_email_verified") && userObj.get("is_email_verified").getAsBoolean(),
            UUID.fromString(obj.get("partner_id").getAsString()),
            obj.has("company_name") && !obj.get("company_name").isJsonNull() ? obj.get("company_name").getAsString() : "",
            obj.has("contact_email") && !obj.get("contact_email").isJsonNull() ? obj.get("contact_email").getAsString() : ""
        );
    }
}
