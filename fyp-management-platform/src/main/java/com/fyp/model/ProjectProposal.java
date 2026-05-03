package com.fyp.model;

import com.fyp.enums.ProposalStatus;
import java.time.LocalDate;
import java.util.UUID;

public class ProjectProposal {
    private UUID proposalId;
    private String title;
    private String description;
    private String abstract_;
    private String objectives;
    private String methodology;
    private String expectedOutcome;
    private LocalDate submissionDate;
    private ProposalStatus status;
    private String rejectionComment;
    private UUID studentId;
    private String studentName; // Added for UI display
    private UUID supervisorId;

    public ProjectProposal() {}

    public ProjectProposal(UUID proposalId, String title, String description,
                           String abstract_, String objectives, String methodology,
                           String expectedOutcome, LocalDate submissionDate,
                           ProposalStatus status, UUID studentId, UUID supervisorId) {
        this.proposalId      = proposalId;
        this.title           = title;
        this.description     = description;
        this.abstract_       = abstract_;
        this.objectives      = objectives;
        this.methodology     = methodology;
        this.expectedOutcome = expectedOutcome;
        this.submissionDate  = submissionDate;
        this.status          = status;
        this.studentId       = studentId;
        this.supervisorId    = supervisorId;
    }

    public void updateContent(String newTitle, String newDesc) {
        this.title       = newTitle;
        this.description = newDesc;
    }

    public void submitForReview() {
        this.status = ProposalStatus.PENDING;
        this.submissionDate = LocalDate.now();
    }

    public void cancel() {
        this.status = ProposalStatus.DRAFT;
    }

    // ── Getters ────────────────────────────────────────────────────────────────
    public UUID getProposalId()        { return proposalId; }
    public String getTitle()           { return title; }
    public String getDescription()     { return description; }
    public String getAbstract()        { return abstract_; }
    public String getObjectives()      { return objectives; }
    public String getMethodology()     { return methodology; }
    public String getExpectedOutcome() { return expectedOutcome; }
    public LocalDate getSubmissionDate(){ return submissionDate; }
    public ProposalStatus getStatus()  { return status; }
    public String getRejectionComment(){ return rejectionComment; }
    public UUID getStudentId()         { return studentId; }
    public String getStudentName()     { return studentName != null ? studentName : "Unknown Student"; }
    public UUID getSupervisorId()      { return supervisorId; }

    // ── Setters ────────────────────────────────────────────────────────────────
    public void setProposalId(UUID proposalId)             { this.proposalId = proposalId; }
    public void setTitle(String title)                     { this.title = title; }
    public void setDescription(String description)         { this.description = description; }
    public void setAbstract(String abstract_)              { this.abstract_ = abstract_; }
    public void setObjectives(String objectives)           { this.objectives = objectives; }
    public void setMethodology(String methodology)         { this.methodology = methodology; }
    public void setExpectedOutcome(String expectedOutcome) { this.expectedOutcome = expectedOutcome; }
    public void setSubmissionDate(LocalDate submissionDate){ this.submissionDate = submissionDate; }
    public void setStatus(ProposalStatus status)           { this.status = status; }
    public void setRejectionComment(String comment)        { this.rejectionComment = comment; }
    public void setStudentId(UUID studentId)               { this.studentId = studentId; }
    public void setStudentName(String studentName)         { this.studentName = studentName; }
    public void setSupervisorId(UUID supervisorId)         { this.supervisorId = supervisorId; }
}
