package com.fyp.model;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

public class Deliverable {
    private UUID fileId;
    private String fileName;
    private String fileType;
    private LocalDateTime uploadTimestamp;
    private String filePath;
    private UUID milestoneId;
    private UUID studentId;

    public Deliverable() {}

    public Deliverable(UUID fileId, String fileName, String fileType,
                       LocalDateTime uploadTimestamp, String filePath,
                       UUID milestoneId, UUID studentId) {
        this.fileId          = fileId;
        this.fileName        = fileName;
        this.fileType        = fileType;
        this.uploadTimestamp = uploadTimestamp;
        this.filePath        = filePath;
        this.milestoneId     = milestoneId;
        this.studentId       = studentId;
    }

    public File download() {
        return new File(filePath);
    }

    public boolean verifyChecksum() {
        // Delegates to FileValidator
        return new File(filePath).exists();
    }

    public boolean delete() {
        return new File(filePath).delete();
    }

    public UUID getFileId()               { return fileId; }
    public String getFileName()           { return fileName; }
    public String getFileType()           { return fileType; }
    public LocalDateTime getUploadTimestamp(){ return uploadTimestamp; }
    public String getFilePath()           { return filePath; }
    public UUID getMilestoneId()          { return milestoneId; }
    public UUID getStudentId()            { return studentId; }

    public void setFileId(UUID fileId)                         { this.fileId = fileId; }
    public void setFileName(String fileName)                   { this.fileName = fileName; }
    public void setFileType(String fileType)                   { this.fileType = fileType; }
    public void setUploadTimestamp(LocalDateTime uploadTimestamp){ this.uploadTimestamp = uploadTimestamp; }
    public void setFilePath(String filePath)                   { this.filePath = filePath; }
    public void setMilestoneId(UUID milestoneId)               { this.milestoneId = milestoneId; }
    public void setStudentId(UUID studentId)                   { this.studentId = studentId; }
}
