package com.fyp.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ProgressReport {
    private UUID reportId;
    private String workDone;
    private String issuesFaced;
    private String nextSteps;
    private LocalDateTime submittedAt;
    private UUID projectId;
    private UUID studentId;

    public ProgressReport() {}

    public ProgressReport(UUID reportId, String workDone, String issuesFaced,
                          String nextSteps, LocalDateTime submittedAt,
                          UUID projectId, UUID studentId) {
        this.reportId    = reportId;
        this.workDone    = workDone;
        this.issuesFaced = issuesFaced;
        this.nextSteps   = nextSteps;
        this.submittedAt = submittedAt;
        this.projectId   = projectId;
        this.studentId   = studentId;
    }

    public UUID getReportId()          { return reportId; }
    public String getWorkDone()        { return workDone; }
    public String getIssuesFaced()     { return issuesFaced; }
    public String getNextSteps()       { return nextSteps; }
    public LocalDateTime getSubmittedAt(){ return submittedAt; }
    public UUID getProjectId()         { return projectId; }
    public UUID getStudentId()         { return studentId; }

    public void setReportId(UUID reportId)               { this.reportId = reportId; }
    public void setWorkDone(String workDone)             { this.workDone = workDone; }
    public void setIssuesFaced(String issuesFaced)       { this.issuesFaced = issuesFaced; }
    public void setNextSteps(String nextSteps)           { this.nextSteps = nextSteps; }
    public void setSubmittedAt(LocalDateTime submittedAt){ this.submittedAt = submittedAt; }
    public void setProjectId(UUID projectId)             { this.projectId = projectId; }
    public void setStudentId(UUID studentId)             { this.studentId = studentId; }
}
