package com.fyp.model;

import com.fyp.enums.ProposalStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public class Supervisor extends User {
    private UUID supervisorId;
    private String employeeId;
    private String researchArea;
    private int slotsAvailable;

    public Supervisor() { super(); this.role = "SUPERVISOR"; }

    public Supervisor(UUID userId, String name, String email, String passwordHash,
                      boolean isActive, boolean isEmailVerified,
                      UUID supervisorId, String employeeId,
                      String researchArea, int slotsAvailable) {
        super(userId, name, email, passwordHash, isActive, isEmailVerified, "SUPERVISOR");
        this.supervisorId  = supervisorId;
        this.employeeId    = employeeId;
        this.researchArea  = researchArea;
        this.slotsAvailable = slotsAvailable;
    }

    public void reviewProposal(UUID proposalId, ProposalStatus status) {
        // Delegated to ProposalService
    }

    public void scheduleMeeting(UUID studentId, LocalDateTime time) {
        // Delegated to MeetingService
    }

    public void provideFeedback(UUID targetId, String text) {
        // Delegated to FeedbackService
    }

    // ── Getters ────────────────────────────────────────────────────────────────
    public UUID getSupervisorId()   { return supervisorId; }
    public String getEmployeeId()   { return employeeId; }
    public String getResearchArea() { return researchArea; }
    public int getSlotsAvailable()  { return slotsAvailable; }

    // ── Setters ────────────────────────────────────────────────────────────────
    public void setSupervisorId(UUID supervisorId)   { this.supervisorId = supervisorId; }
    public void setEmployeeId(String employeeId)     { this.employeeId = employeeId; }
    public void setResearchArea(String researchArea) { this.researchArea = researchArea; }
    public void setSlotsAvailable(int slotsAvailable){ this.slotsAvailable = slotsAvailable; }
}
