package com.fyp.util;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inserts audit log entries into fyp.audit_logs.
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

        String sql = "INSERT INTO fyp.audit_logs (user_id, action, ip_address, timestamp, details) " +
                     "VALUES (?::uuid, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId != null ? userId.toString() : null);
            ps.setString(2, action);
            ps.setString(3, ip);
            ps.setObject(4, LocalDateTime.now());
            ps.setString(5, details);
            ps.executeUpdate();
        }
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
