package com.fyp.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class IndustryProblem {
    private UUID problemId;
    private String title;
    private String description;
    private String domain;
    private String companyName;
    private LocalDate deadline;
    private List<String> requiredSkills;
    private String contactEmail;
    private UUID partnerId;
    private UUID adoptedBy;
    private boolean isActive;

    public IndustryProblem() {}

    // ── Getters ────────────────────────────────────────────────────────────────
    public UUID getProblemId()              { return problemId; }
    public String getTitle()               { return title; }
    public String getDescription()         { return description; }
    public String getDomain()              { return domain; }
    public String getCompanyName()         { return companyName; }
    public LocalDate getDeadline()         { return deadline; }
    public List<String> getRequiredSkills(){ return requiredSkills; }
    public String getContactEmail()        { return contactEmail; }
    public UUID getPartnerId()             { return partnerId; }
    public UUID getAdoptedBy()             { return adoptedBy; }
    public boolean isActive()              { return isActive; }

    // ── Setters ────────────────────────────────────────────────────────────────
    public void setProblemId(UUID problemId)              { this.problemId = problemId; }
    public void setTitle(String title)                    { this.title = title; }
    public void setDescription(String description)        { this.description = description; }
    public void setDomain(String domain)                  { this.domain = domain; }
    public void setCompanyName(String companyName)        { this.companyName = companyName; }
    public void setDeadline(LocalDate deadline)           { this.deadline = deadline; }
    public void setRequiredSkills(List<String> skills)   { this.requiredSkills = skills; }
    public void setContactEmail(String contactEmail)      { this.contactEmail = contactEmail; }
    public void setPartnerId(UUID partnerId)              { this.partnerId = partnerId; }
    public void setAdoptedBy(UUID adoptedBy)              { this.adoptedBy = adoptedBy; }
    public void setActive(boolean active)                 { isActive = active; }
}

