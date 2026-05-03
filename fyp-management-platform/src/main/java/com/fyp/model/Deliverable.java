package com.fyp.model;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

public class Deliverable {
    private UUID fileId;
    private String title;        // UI-friendly title
    private String fileName;
    private String fileType;
    private String status;       // e.g. "UPLOADED", "REVIEWED"
    private LocalDateTime uploadTimestamp;
    private String filePath;
    private UUID milestoneId;
    private UUID studentId;
    private String description;

    public Deliverable() {}

    public File download()        { return new File(filePath != null ? filePath : ""); }
    public boolean verifyChecksum(){ return filePath != null && new File(filePath).exists(); }
    public boolean delete()       { return filePath != null && new File(filePath).delete(); }

    // ── Getters ────────────────────────────────────────────────────────────────
    public UUID getFileId()                 { return fileId; }
    public String getTitle()               { return title != null ? title : fileName; }
    public String getFileName()            { return fileName; }
    public String getFileType()            { return fileType; }
    public String getStatus()              { return status != null ? status : "UPLOADED"; }
    public LocalDateTime getUploadedAt()   { return uploadTimestamp; }
    public LocalDateTime getUploadTimestamp(){ return uploadTimestamp; }
    public String getFilePath()            { return filePath; }
    public UUID getMilestoneId()           { return milestoneId; }
    public UUID getStudentId()             { return studentId; }
    public String getDescription()         { return description; }

    // ── Setters ────────────────────────────────────────────────────────────────
    public void setFileId(UUID fileId)                          { this.fileId = fileId; }
    public void setTitle(String title)                          { this.title = title; this.fileName = title; }
    public void setFileName(String fileName)                    { this.fileName = fileName; }
    public void setFileType(String fileType)                    { this.fileType = fileType; }
    public void setStatus(String status)                        { this.status = status; }
    public void setUploadTimestamp(LocalDateTime uploadTimestamp){ this.uploadTimestamp = uploadTimestamp; }
    public void setFilePath(String filePath)                    { this.filePath = filePath; }
    public void setMilestoneId(UUID milestoneId)                { this.milestoneId = milestoneId; }
    public void setStudentId(UUID studentId)                    { this.studentId = studentId; }
    public void setDescription(String description)              { this.description = description; }
}

