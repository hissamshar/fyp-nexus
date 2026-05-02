package com.fyp.model;

import java.util.UUID;

public class Grade {
    private UUID gradeId;
    private String letterGrade;
    private boolean isPublished;
    private UUID projectId;
    private UUID rubricId;

    public Grade() {}

    public Grade(UUID gradeId, String letterGrade, boolean isPublished,
                 UUID projectId, UUID rubricId) {
        this.gradeId     = gradeId;
        this.letterGrade = letterGrade;
        this.isPublished = isPublished;
        this.projectId   = projectId;
        this.rubricId    = rubricId;
    }

    public double calculateGPA() {
        if (letterGrade == null) return 0.0;
        return switch (letterGrade) {
            case "A+"  -> 4.0;
            case "A"   -> 4.0;
            case "A-"  -> 3.7;
            case "B+"  -> 3.3;
            case "B"   -> 3.0;
            case "B-"  -> 2.7;
            case "C+"  -> 2.3;
            case "C"   -> 2.0;
            case "F"   -> 0.0;
            default    -> 0.0;
        };
    }

    public void publishGrade() {
        this.isPublished = true;
    }

    public UUID getGradeId()      { return gradeId; }
    public String getLetterGrade(){ return letterGrade; }
    public boolean isPublished()  { return isPublished; }
    public UUID getProjectId()    { return projectId; }
    public UUID getRubricId()     { return rubricId; }

    public void setGradeId(UUID gradeId)          { this.gradeId = gradeId; }
    public void setLetterGrade(String letterGrade) { this.letterGrade = letterGrade; }
    public void setPublished(boolean published)    { isPublished = published; }
    public void setProjectId(UUID projectId)       { this.projectId = projectId; }
    public void setRubricId(UUID rubricId)         { this.rubricId = rubricId; }
}
