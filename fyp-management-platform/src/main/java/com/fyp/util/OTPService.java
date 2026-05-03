package com.fyp.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Generate, store, and verify 6-digit OTPs with 15-minute expiry.
 * Stored in fyp.otp_tokens via Supabase REST API.
 * OTPs are marked as used immediately after verification.
 */
public class OTPService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_EXPIRY_MINUTES = 15;

    private OTPService() {}

    /**
     * Generate a 6-digit OTP, store it in the DB, and return the code.
     */
    public static String generateAndStore(UUID userId, String purpose) throws Exception {
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        String jwt = SessionManager.getJwtToken();

        // Invalidate existing unused OTPs
        JsonObject invalidateBody = new JsonObject();
        invalidateBody.addProperty("is_used", true);
        HttpRequest.Builder invalidateReq = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/otp_tokens?user_id=eq." + userId + "&purpose=eq." + purpose + "&is_used=eq.false"))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(invalidateBody.toString()));
        SupabaseClient.sendAuthenticatedRequest(invalidateReq, jwt);

        // Insert new OTP
        JsonObject json = new JsonObject();
        json.addProperty("user_id", userId.toString());
        json.addProperty("otp_code", otp);
        json.addProperty("purpose", purpose);
        json.addProperty("expires_at", expiresAt.toString());
        HttpRequest.Builder insertReq = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/otp_tokens"))
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(insertReq, jwt);

        return otp;
    }

    /**
     * Verify an OTP. Returns true if valid, not expired, and not already used.
     */
    public static boolean verify(UUID userId, String inputOtp, String purpose) throws Exception {
        String jwt = SessionManager.getJwtToken();
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/otp_tokens?user_id=eq." + userId
                        + "&otp_code=eq." + inputOtp + "&purpose=eq." + purpose
                        + "&is_used=eq.false&order=created_at.desc&limit=1"))
                .GET();
        HttpResponse<String> res = SupabaseClient.sendAuthenticatedRequest(req, jwt);

        if (res.statusCode() != 200 || res.body().equals("[]")) return false;

        JsonArray arr = JsonParser.parseString(res.body()).getAsJsonArray();
        JsonObject token = arr.get(0).getAsJsonObject();
        String tokenId = token.get("token_id").getAsString();
        String expiresStr = token.get("expires_at").getAsString();
        LocalDateTime expiresAt = LocalDateTime.parse(expiresStr);

        // Mark as used regardless
        markUsed(UUID.fromString(tokenId), jwt);

        // Check expiry
        return !LocalDateTime.now().isAfter(expiresAt);
    }

    private static void markUsed(UUID tokenId, String jwt) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("is_used", true);
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/otp_tokens?token_id=eq." + tokenId))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(req, jwt);
    }
}
