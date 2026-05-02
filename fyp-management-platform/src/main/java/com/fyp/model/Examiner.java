package com.fyp.model;

import java.util.List;
import java.util.UUID;

public class Examiner extends User {
    private UUID examinerId;
    private List<String> expertise;
    private boolean isExternal;

    public Examiner() { super(); this.role = "EXAMINER"; }

    public Examiner(UUID userId, String name, String email, String passwordHash,
                    boolean isActive, boolean isEmailVerified,
                    UUID examinerId, List<String> expertise, boolean isExternal) {
        super(userId, name, email, passwordHash, isActive, isEmailVerified, "EXAMINER");
        this.examinerId = examinerId;
        this.expertise  = expertise;
        this.isExternal = isExternal;
    }

    public void evaluateProject(UUID projectId, Rubric rubric) {
        // Delegated to GradeService
    }

    public void submitFinalGrade(UUID projectId, double score) {
        // Delegated to GradeService
    }

    // ── Getters ────────────────────────────────────────────────────────────────
    public UUID getExaminerId()      { return examinerId; }
    public List<String> getExpertise(){ return expertise; }
    public boolean isExternal()      { return isExternal; }

    // ── Setters ────────────────────────────────────────────────────────────────
    public void setExaminerId(UUID examinerId)       { this.examinerId = examinerId; }
    public void setExpertise(List<String> expertise) { this.expertise = expertise; }
    public void setExternal(boolean external)        { isExternal = external; }
}
