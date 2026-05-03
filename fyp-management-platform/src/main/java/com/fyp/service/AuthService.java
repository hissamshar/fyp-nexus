package com.fyp.service;

import com.fyp.dao.*;
import com.fyp.model.*;
import com.fyp.util.SessionManager;
import com.fyp.util.SupabaseClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class AuthService {

    /**
     * Login with email + password. Stores typed profile in SessionManager.
     */
    public static boolean login(String email, String password) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("email", email);
            json.addProperty("password", password);

            HttpRequest.Builder request = HttpRequest.newBuilder()
                    .uri(URI.create(SupabaseClient.getBaseUrl() + "/auth/v1/token?grant_type=password"))
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()));

            HttpResponse<String> response = SupabaseClient.sendRequest(request);

            if (response.statusCode() == 200) {
                JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
                String token = body.get("access_token").getAsString();
                JsonObject userObj = body.getAsJsonObject("user");
                UUID userId = UUID.fromString(userObj.get("id").getAsString());

                return fetchAndStoreProfile(userId, token);
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Register a new user via Supabase Auth.
     * Returns "AUTO_LOGGED_IN" if email confirmation is disabled, or the userId string otherwise.
     */
    public static String signup(String email, String password, String name, String role) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("email", email);
            json.addProperty("password", password);

            JsonObject meta = new JsonObject();
            meta.addProperty("name", name);
            meta.addProperty("role", role);
            json.add("data", meta);

            HttpRequest.Builder request = HttpRequest.newBuilder()
                    .uri(URI.create(SupabaseClient.getBaseUrl() + "/auth/v1/signup"))
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()));

            HttpResponse<String> response = SupabaseClient.sendRequest(request);
            System.out.println("[AuthService] Signup Response Code: " + response.statusCode());
            System.out.println("[AuthService] Signup Response Body: " + response.body());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();

                // Email confirmation disabled — session returned immediately
                if (body.has("access_token") && !body.get("access_token").isJsonNull()) {
                    String token = body.get("access_token").getAsString();
                    UUID userId = UUID.fromString(body.getAsJsonObject("user").get("id").getAsString());
                    fetchAndStoreProfile(userId, token);
                    return "AUTO_LOGGED_IN";
                }
                if (body.has("session") && !body.get("session").isJsonNull()) {
                    JsonObject session = body.getAsJsonObject("session");
                    String token = session.get("access_token").getAsString();
                    UUID userId = UUID.fromString(session.getAsJsonObject("user").get("id").getAsString());
                    fetchAndStoreProfile(userId, token);
                    return "AUTO_LOGGED_IN";
                }

                if (body.has("id")) return body.get("id").getAsString();
                if (body.has("user") && !body.get("user").isJsonNull())
                    return body.getAsJsonObject("user").get("id").getAsString();
                return "OK";
            } else {
                JsonObject err = JsonParser.parseString(response.body()).getAsJsonObject();
                String msg = err.has("msg") ? err.get("msg").getAsString()
                           : err.has("message") ? err.get("message").getAsString()
                           : err.has("error_description") ? err.get("error_description").getAsString()
                           : "Supabase Error: " + response.statusCode();
                throw new Exception(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Verify email OTP. On success, fetches and stores typed profile.
     */
    public static boolean verifyEmailOtp(String email, String otp) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("email", email);
            json.addProperty("token", otp);
            json.addProperty("type", "signup");

            HttpRequest.Builder request = HttpRequest.newBuilder()
                    .uri(URI.create(SupabaseClient.getBaseUrl() + "/auth/v1/verify"))
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()));

            HttpResponse<String> response = SupabaseClient.sendRequest(request);

            if (response.statusCode() == 200) {
                JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
                if (body.has("access_token")) {
                    String token = body.get("access_token").getAsString();
                    UUID userId = UUID.fromString(body.getAsJsonObject("user").get("id").getAsString());
                    fetchAndStoreProfile(userId, token);
                }
                return true;
            }
            System.err.println("[AuthService] OTP verification failed: " + response.statusCode() + " " + response.body());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean resendOtp(String email) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("type", "signup");
            json.addProperty("email", email);

            HttpRequest.Builder request = HttpRequest.newBuilder()
                    .uri(URI.create(SupabaseClient.getBaseUrl() + "/auth/v1/resend"))
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()));

            return SupabaseClient.sendRequest(request).statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Fetches the role-specific profile from fyp schema and stores the typed
     * object (Student / Supervisor / Examiner / Admin / IndustryPartner) in SessionManager.
     * Falls back to a plain User if the role-specific row doesn't exist yet.
     */
    private static boolean fetchAndStoreProfile(UUID userId, String token) {
        try {
            // Get base user row to determine role
            HttpRequest.Builder userReq = HttpRequest.newBuilder()
                    .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/users?user_id=eq." + userId))
                    .GET();
            HttpResponse<String> userRes = SupabaseClient.sendAuthenticatedRequest(userReq, token);

            if (userRes.statusCode() != 200 || userRes.body().equals("[]")) {
                System.err.println("[AuthService] fyp.users row not found for " + userId);
                return false;
            }

            com.google.gson.JsonObject fypUser = JsonParser.parseString(userRes.body())
                    .getAsJsonArray().get(0).getAsJsonObject();
            String role = fypUser.get("role").getAsString();

            User profile = switch (role) {
                case "STUDENT"          -> new StudentDAO().findByUserId(userId, token).orElse(null);
                case "SUPERVISOR"       -> new SupervisorDAO().findByUserId(userId, token).orElse(null);
                case "EXAMINER"         -> new ExaminerDAO().findByUserId(userId, token).orElse(null);
                case "ADMIN"            -> new AdminDAO().findByUserId(userId, token).orElse(null);
                case "INDUSTRY_PARTNER" -> new IndustryPartnerDAO().findByUserId(userId, token).orElse(null);
                default                 -> null;
            };

            if (profile == null) {
                // Role-specific row missing (trigger may not have run yet) — fall back to plain User
                System.err.println("[AuthService] Role-specific profile missing for " + role + " " + userId + ". Using base User.");
                String name  = fypUser.has("name")  ? fypUser.get("name").getAsString()  : "";
                String email = fypUser.has("email") ? fypUser.get("email").getAsString() : "";
                profile = new User(userId, name, email, "", true, true, role) {};
            }

            SessionManager.setCurrentUser(profile, token);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
