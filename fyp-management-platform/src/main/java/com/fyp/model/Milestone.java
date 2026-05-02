package com.fyp.model;

import com.fyp.enums.MilestoneStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public class Milestone {
    private UUID milestoneId;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private int weightage;
    private MilestoneStatus status;
    private UUID projectId;

    public Milestone() {}

    public Milestone(UUID milestoneId, String title, String description,
                     LocalDateTime deadline, int weightage,
                     MilestoneStatus status, UUID projectId) {
        this.milestoneId = milestoneId;
        this.title       = title;
        this.description = description;
        this.deadline    = deadline;
        this.weightage   = weightage;
        this.status      = status;
        this.projectId   = projectId;
    }

    public boolean checkOverdue() {
        if (status == MilestoneStatus.COMPLETED) return false;
        return deadline != null && LocalDateTime.now().isAfter(deadline);
    }

    public void updateStatus(MilestoneStatus newStatus) {
        this.status = newStatus;
    }

    public UUID getMilestoneId()      { return milestoneId; }
    public String getTitle()          { return title; }
    public String getDescription()    { return description; }
    public LocalDateTime getDeadline(){ return deadline; }
    public int getWeightage()         { return weightage; }
    public MilestoneStatus getStatus(){ return status; }
    public UUID getProjectId()        { return projectId; }

    public void setMilestoneId(UUID milestoneId)       { this.milestoneId = milestoneId; }
    public void setTitle(String title)                 { this.title = title; }
    public void setDescription(String description)     { this.description = description; }
    public void setDeadline(LocalDateTime deadline)    { this.deadline = deadline; }
    public void setWeightage(int weightage)            { this.weightage = weightage; }
    public void setStatus(MilestoneStatus status)      { this.status = status; }
    public void setProjectId(UUID projectId)           { this.projectId = projectId; }
}
