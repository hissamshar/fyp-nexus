package com.fyp.model;

import java.util.List;
import java.util.UUID;

public class IndustryPartner extends User {
    private UUID partnerId;
    private String companyName;
    private String contactEmail;

    public IndustryPartner() { super(); this.role = "INDUSTRY_PARTNER"; }

    public IndustryPartner(UUID userId, String name, String email, String passwordHash,
                           boolean isActive, boolean isEmailVerified,
                           UUID partnerId, String companyName, String contactEmail) {
        super(userId, name, email, passwordHash, isActive, isEmailVerified, "INDUSTRY_PARTNER");
        this.partnerId    = partnerId;
        this.companyName  = companyName;
        this.contactEmail = contactEmail;
    }

    public void postProblem(IndustryProblem problem) {
        // Delegated to IndustryProblemService
    }

    public List<Project> viewAdoptedProjects() {
        // Delegated to IndustryProblemService
        return List.of();
    }

    public UUID getPartnerId()      { return partnerId; }
    public String getCompanyName()  { return companyName; }
    public String getContactEmail() { return contactEmail; }

    public void setPartnerId(UUID partnerId)       { this.partnerId = partnerId; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public void setContactEmail(String contactEmail){ this.contactEmail = contactEmail; }
}
