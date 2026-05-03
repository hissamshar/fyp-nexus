package com.fyp.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class ProgressReport {
    private UUID reportId;
    private String reportWeek;    // UI alias
    private String workDone;
    private String plannedWork;   // UI alias
    private String issuesFaced;
    private String blockers;      // UI alias for issuesFaced
    private String nextSteps;
    private LocalDate reportDate; // UI alias
    private LocalDateTime submittedAt;
    private UUID projectId;
    private UUID studentId;
    private UUID submittedBy;     // UI alias

    public ProgressReport() {}

    // ── Getters ────────────────────────────────────────────────────────────────
    public UUID getReportId()          { return reportId; }
    public String getReportWeek()      { return reportWeek != null ? reportWeek : "Week -"; }
    public String getWorkDone()        { return workDone; }
    public String getWorkCompleted()   { return workDone; }
    public String getPlannedWork()     { return plannedWork != null ? plannedWork : nextSteps; }
    public String getIssuesFaced()     { return issuesFaced; }
    public String getBlockers()        { return blockers != null ? blockers : issuesFaced; }
    public String getNextSteps()       { return nextSteps; }
    public LocalDate getReportDate()   { return reportDate != null ? reportDate : 
                                             (submittedAt != null ? submittedAt.toLocalDate() : null); }
    public LocalDateTime getSubmittedAt(){ return submittedAt; }
    public UUID getProjectId()         { return projectId; }
    public UUID getStudentId()         { return studentId; }
    public UUID getSubmittedBy()       { return submittedBy != null ? submittedBy : studentId; }

    // ── Setters ────────────────────────────────────────────────────────────────
    public void setReportId(UUID reportId)               { this.reportId = reportId; }
    public void setReportWeek(String reportWeek)         { this.reportWeek = reportWeek; }
    public void setWorkDone(String workDone)             { this.workDone = workDone; }
    public void setWorkCompleted(String workDone)        { this.workDone = workDone; }
    public void setPlannedWork(String plannedWork)       { this.plannedWork = plannedWork; this.nextSteps = plannedWork; }
    public void setIssuesFaced(String issuesFaced)       { this.issuesFaced = issuesFaced; this.blockers = issuesFaced; }
    public void setBlockers(String blockers)             { this.blockers = blockers; this.issuesFaced = blockers; }
    public void setNextSteps(String nextSteps)           { this.nextSteps = nextSteps; this.plannedWork = nextSteps; }
    public void setReportDate(LocalDate reportDate)      { this.reportDate = reportDate; 
                                                           this.submittedAt = reportDate != null ? reportDate.atStartOfDay() : null; }
    public void setSubmittedAt(LocalDateTime submittedAt){ this.submittedAt = submittedAt; }
    public void setProjectId(UUID projectId)             { this.projectId = projectId; }
    public void setStudentId(UUID studentId)             { this.studentId = studentId; }
    public void setSubmittedBy(UUID submittedBy)         { this.submittedBy = submittedBy; this.studentId = submittedBy; }
}

