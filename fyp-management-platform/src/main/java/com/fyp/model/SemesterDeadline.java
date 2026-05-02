package com.fyp.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class SemesterDeadline {
    private UUID deadlineId;
    private String semester;
    private String deadlineType;
    private LocalDateTime dueDate;
    private String description;

    public SemesterDeadline() {}

    public SemesterDeadline(UUID deadlineId, String semester, String deadlineType,
                            LocalDateTime dueDate, String description) {
        this.deadlineId   = deadlineId;
        this.semester     = semester;
        this.deadlineType = deadlineType;
        this.dueDate      = dueDate;
        this.description  = description;
    }

    public UUID getDeadlineId()       { return deadlineId; }
    public String getSemester()       { return semester; }
    public String getDeadlineType()   { return deadlineType; }
    public LocalDateTime getDueDate() { return dueDate; }
    public String getDescription()    { return description; }

    public void setDeadlineId(UUID deadlineId)       { this.deadlineId = deadlineId; }
    public void setSemester(String semester)         { this.semester = semester; }
    public void setDeadlineType(String deadlineType) { this.deadlineType = deadlineType; }
    public void setDueDate(LocalDateTime dueDate)    { this.dueDate = dueDate; }
    public void setDescription(String description)   { this.description = description; }
}
