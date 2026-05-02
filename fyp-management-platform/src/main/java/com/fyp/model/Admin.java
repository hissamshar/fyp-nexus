package com.fyp.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Admin extends User {
    private UUID adminId;
    private List<String> permissions;

    public Admin() { super(); this.role = "ADMIN"; }

    public Admin(UUID userId, String name, String email, String passwordHash,
                 boolean isActive, boolean isEmailVerified,
                 UUID adminId, List<String> permissions) {
        super(userId, name, email, passwordHash, isActive, isEmailVerified, "ADMIN");
        this.adminId     = adminId;
        this.permissions = permissions;
    }

    public void manageUsers(String action, User user) {
        // Delegated to AdminService
    }

    public void setupDeadlines(Map<String, Object> config) {
        // Delegated to AdminService
    }

    public List<AuditLogEntry> viewAuditLogs() {
        // Delegated to AuditLogger / AdminService
        return List.of();
    }

    // ── Getters ────────────────────────────────────────────────────────────────
    public UUID getAdminId()             { return adminId; }
    public List<String> getPermissions() { return permissions; }

    // ── Setters ────────────────────────────────────────────────────────────────
    public void setAdminId(UUID adminId)             { this.adminId = adminId; }
    public void setPermissions(List<String> permissions){ this.permissions = permissions; }
}
