package com.fyp.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class SemesterDeadline {
    private UUID deadlineId;
    private String name;          // human-readable name
    private String semester;
    private String deadlineType;
    private LocalDate deadlineDate; // LocalDate alias for UI
    private LocalDateTime dueDate;
    private String description;

    public SemesterDeadline() {}

    // ── Getters ────────────────────────────────────────────────────────────────
    public UUID getDeadlineId()         { return deadlineId; }
    public String getName()             { return name != null ? name : deadlineType; }
    public String getSemester()         { return semester; }
    public String getDeadlineType()     { return deadlineType; }
    public LocalDate getDeadlineDate()  { return deadlineDate != null ? deadlineDate :
                                              (dueDate != null ? dueDate.toLocalDate() : null); }
    public LocalDateTime getDueDate()   { return dueDate; }
    public String getDescription()      { return description; }

    // ── Setters ────────────────────────────────────────────────────────────────
    public void setDeadlineId(UUID deadlineId)       { this.deadlineId = deadlineId; }
    public void setName(String name)                 { this.name = name; this.deadlineType = name; }
    public void setSemester(String semester)         { this.semester = semester; }
    public void setDeadlineType(String deadlineType) { this.deadlineType = deadlineType; }
    public void setDeadlineDate(LocalDate date)      { this.deadlineDate = date;
                                                       this.dueDate = date.atStartOfDay(); }
    public void setDueDate(LocalDateTime dueDate)    { this.dueDate = dueDate; }
    public void setDescription(String description)   { this.description = description; }
}

