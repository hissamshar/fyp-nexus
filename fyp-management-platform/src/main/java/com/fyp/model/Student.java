package com.fyp.model;

import com.fyp.model.Grade;
import com.fyp.model.ProjectProposal;
import java.io.File;
import java.util.List;
import java.util.UUID;

public class Student extends User {
    private UUID studentId;
    private String department;
    private double cgpa;

    public Student() { super(); this.role = "STUDENT"; }

    public Student(UUID userId, String name, String email, String passwordHash,
                   boolean isActive, boolean isEmailVerified,
                   UUID studentId, String department, double cgpa) {
        super(userId, name, email, passwordHash, isActive, isEmailVerified, "STUDENT");
        this.studentId  = studentId;
        this.department = department;
        this.cgpa       = cgpa;
    }

    public void submitProposal(ProjectProposal proposal) {
        // Delegated to ProposalService
    }

    public void uploadDeliverable(UUID milestoneId, File file) {
        // Delegated to DeliverableService
    }

    public List<Grade> viewGrades() {
        // Delegated to GradeService
        return List.of();
    }

    // ── Getters ────────────────────────────────────────────────────────────────
    public UUID getStudentId()   { return studentId; }
    public String getDepartment(){ return department; }
    public double getCgpa()      { return cgpa; }

    // ── Setters ────────────────────────────────────────────────────────────────
    public void setStudentId(UUID studentId)   { this.studentId = studentId; }
    public void setDepartment(String department){ this.department = department; }
    public void setCgpa(double cgpa)           { this.cgpa = cgpa; }
}
