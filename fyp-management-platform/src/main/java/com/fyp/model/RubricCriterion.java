package com.fyp.model;

import java.util.UUID;

public class RubricCriterion {
    private UUID criterionId;
    private String criterionName;
    private int maxScore;
    private String description;
    private UUID rubricId;

    public RubricCriterion() {}

    public RubricCriterion(UUID criterionId, String criterionName,
                           int maxScore, String description, UUID rubricId) {
        this.criterionId   = criterionId;
        this.criterionName = criterionName;
        this.maxScore      = maxScore;
        this.description   = description;
        this.rubricId      = rubricId;
    }

    public boolean validateScore(int points) {
        return points >= 0 && points <= maxScore;
    }

    public UUID getCriterionId()    { return criterionId; }
    public String getCriterionName(){ return criterionName; }
    public int getMaxScore()        { return maxScore; }
    public String getDescription()  { return description; }
    public UUID getRubricId()       { return rubricId; }

    public void setCriterionId(UUID criterionId)       { this.criterionId = criterionId; }
    public void setCriterionName(String criterionName) { this.criterionName = criterionName; }
    public void setMaxScore(int maxScore)              { this.maxScore = maxScore; }
    public void setDescription(String description)    { this.description = description; }
    public void setRubricId(UUID rubricId)            { this.rubricId = rubricId; }

    // Alias for UI binding
    public String getName()           { return criterionName; }
    public void setName(String name)  { this.criterionName = name; }
}
