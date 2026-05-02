package com.fyp.model;

import com.fyp.enums.ProjectStatus;
import java.time.LocalDate;
import java.util.UUID;

public class Project {
    private UUID projectId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String repoUrl;
    private ProjectStatus status;
    private UUID proposalId;

    public Project() {}

    public Project(UUID projectId, LocalDate startDate, LocalDate endDate,
                   String repoUrl, ProjectStatus status, UUID proposalId) {
        this.projectId  = projectId;
        this.startDate  = startDate;
        this.endDate    = endDate;
        this.repoUrl    = repoUrl;
        this.status     = status;
        this.proposalId = proposalId;
    }

    public float calculateProgress() {
        // Calculated in MilestoneService based on completed vs total milestones
        return 0.0f;
    }

    public void updateStatus(ProjectStatus newStatus) {
        this.status = newStatus;
    }

    public void archiveProject() {
        this.status = ProjectStatus.COMPLETED;
    }

    // ── Getters ────────────────────────────────────────────────────────────────
    public UUID getProjectId()      { return projectId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate()   { return endDate; }
    public String getRepoUrl()      { return repoUrl; }
    public ProjectStatus getStatus(){ return status; }
    public UUID getProposalId()     { return proposalId; }

    // ── Setters ────────────────────────────────────────────────────────────────
    public void setProjectId(UUID projectId)     { this.projectId = projectId; }
    public void setStartDate(LocalDate startDate){ this.startDate = startDate; }
    public void setEndDate(LocalDate endDate)    { this.endDate = endDate; }
    public void setRepoUrl(String repoUrl)       { this.repoUrl = repoUrl; }
    public void setStatus(ProjectStatus status)  { this.status = status; }
    public void setProposalId(UUID proposalId)   { this.proposalId = proposalId; }
}
