package com.fyp.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class GradeEntry {
    private UUID entryId;
    private int score;
    private String comments;
    private LocalDateTime gradedAt;
    private UUID gradeId;
    private UUID graderId;
    private UUID criterionId;

    public GradeEntry() {}

    public GradeEntry(UUID entryId, int score, String comments,
                      LocalDateTime gradedAt, UUID gradeId,
                      UUID graderId, UUID criterionId) {
        this.entryId     = entryId;
        this.score       = score;
        this.comments    = comments;
        this.gradedAt    = gradedAt;
        this.gradeId     = gradeId;
        this.graderId    = graderId;
        this.criterionId = criterionId;
    }

    public void updateEntry(int newScore, String newComment) {
        this.score    = newScore;
        this.comments = newComment;
        this.gradedAt = LocalDateTime.now();
    }

    public String getFeedback() { return comments; }

    public UUID getEntryId()        { return entryId; }
    public int getScore()           { return score; }
    public String getComments()     { return comments; }
    public LocalDateTime getGradedAt(){ return gradedAt; }
    public UUID getGradeId()        { return gradeId; }
    public UUID getGraderId()       { return graderId; }
    public UUID getCriterionId()    { return criterionId; }

    public void setEntryId(UUID entryId)           { this.entryId = entryId; }
    public void setScore(int score)                { this.score = score; }
    public void setComments(String comments)       { this.comments = comments; }
    public void setGradedAt(LocalDateTime gradedAt){ this.gradedAt = gradedAt; }
    public void setGradeId(UUID gradeId)           { this.gradeId = gradeId; }
    public void setGraderId(UUID graderId)          { this.graderId = graderId; }
    public void setCriterionId(UUID criterionId)   { this.criterionId = criterionId; }
}
