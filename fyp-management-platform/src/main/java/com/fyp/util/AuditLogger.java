package com.fyp.util;

import com.google.gson.JsonObject;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.UUID;

/**
 * Inserts audit log entries into fyp.audit_logs via Supabase REST API.
 * Call on every key user action: login, logout, proposal submit,
 * file upload, grade change, account change.
 */
public class AuditLogger {

    private AuditLogger() {}

    /**
     * Log an action asynchronously (on background thread to avoid blocking UI).
     */
    public static void log(UUID userId, String action, String details) {
        Thread.ofVirtual().start(() -> {
            try {
                doLog(userId, action, details);
            } catch (Exception e) {
                System.err.println("[AuditLogger] Failed to write audit log: " + e.getMessage());
            }
        });
    }

    private static void doLog(UUID userId, String action, String details) throws Exception {
        String ip;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            ip = "unknown";
        }

        String jwt = SessionManager.getJwtToken();
        if (jwt == null) return; // Not logged in, skip audit

        JsonObject json = new JsonObject();
        if (userId != null) json.addProperty("user_id", userId.toString());
        json.addProperty("action", action);
        json.addProperty("ip_address", ip);
        json.addProperty("details", details);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/audit_logs"))
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        SupabaseClient.sendAuthenticatedRequest(request, jwt);
    }

    // ── Convenience helpers ────────────────────────────────────────────────────

    public static void logLogin(UUID userId) {
        log(userId, "USER_LOGIN", "Successful login");
    }

    public static void logLogout(UUID userId) {
        log(userId, "USER_LOGOUT", "User logged out");
    }

    public static void logProposalSubmit(UUID userId, UUID proposalId) {
        log(userId, "PROPOSAL_SUBMIT", "Proposal ID: " + proposalId);
    }

    public static void logFileUpload(UUID userId, String fileName) {
        log(userId, "FILE_UPLOAD", "File: " + fileName);
    }

    public static void logGradeChange(UUID userId, UUID gradeId) {
        log(userId, "GRADE_CHANGE", "Grade ID: " + gradeId);
    }

    public static void logAccountChange(UUID userId, String changeType) {
        log(userId, "ACCOUNT_CHANGE", changeType);
    }

    public static void logPasswordReset(UUID userId) {
        log(userId, "PASSWORD_RESET", "Password reset via OTP");
    }
}
