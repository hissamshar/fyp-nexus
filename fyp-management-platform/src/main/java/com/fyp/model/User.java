package com.fyp.model;

import java.util.Map;
import java.util.UUID;

/**
 * Abstract base class for all user types in the FYP Management Platform.
 */
public abstract class User {
    protected UUID userId;
    protected String name;
    protected String email;
    protected String passwordHash;
    protected boolean isActive;
    protected boolean isEmailVerified;
    protected String role;

    public User() {}

    public User(UUID userId, String name, String email, String passwordHash,
                boolean isActive, boolean isEmailVerified, String role) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.isActive = isActive;
        this.isEmailVerified = isEmailVerified;
        this.role = role;
    }

    public boolean login(String credentials) {
        // Implemented in AuthService
        return false;
    }

    public void logout() {
        // Implemented in AuthService / SessionManager
    }

    public void updateProfile(Map<String, Object> data) {
        if (data.containsKey("name"))  this.name  = (String) data.get("name");
        if (data.containsKey("email")) this.email = (String) data.get("email");
    }

    public boolean resetPassword(String token) {
        // Implemented in AuthService / OTPService
        return false;
    }

    // ── Getters ────────────────────────────────────────────────────────────────
    public UUID getUserId()        { return userId; }
    public String getName()        { return name; }
    public String getEmail()       { return email; }
    public String getPasswordHash(){ return passwordHash; }
    public boolean isActive()      { return isActive; }
    public boolean isEmailVerified(){ return isEmailVerified; }
    public String getRole()        { return role; }

    // ── Setters ────────────────────────────────────────────────────────────────
    public void setUserId(UUID userId)             { this.userId = userId; }
    public void setName(String name)               { this.name = name; }
    public void setEmail(String email)             { this.email = email; }
    public void setPasswordHash(String passwordHash){ this.passwordHash = passwordHash; }
    public void setActive(boolean active)          { isActive = active; }
    public void setEmailVerified(boolean verified) { isEmailVerified = verified; }
    public void setRole(String role)               { this.role = role; }

    @Override
    public String toString() {
        return name + " <" + email + "> [" + role + "]";
    }
}
