package com.fyp.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Rubric {
    private UUID rubricId;
    private int totalPoints;
    private String version;
    private String name;
    private List<RubricCriterion> criteria = new ArrayList<>();

    public Rubric() {}

    public Rubric(UUID rubricId, int totalPoints, String version, String name) {
        this.rubricId    = rubricId;
        this.totalPoints = totalPoints;
        this.version     = version;
        this.name        = name;
    }

    public void addCriterion(String name, int maxScore) {
        RubricCriterion c = new RubricCriterion();
        c.setCriterionName(name);
        c.setMaxScore(maxScore);
        c.setRubricId(this.rubricId);
        criteria.add(c);
    }

    public int calculateTotal() {
        return criteria.stream().mapToInt(RubricCriterion::getMaxScore).sum();
    }

    public UUID getRubricId()           { return rubricId; }
    public int getTotalPoints()         { return totalPoints; }
    public String getVersion()          { return version; }
    public String getName()             { return name; }
    public List<RubricCriterion> getCriteria(){ return criteria; }

    public void setRubricId(UUID rubricId)          { this.rubricId = rubricId; }
    public void setTotalPoints(int totalPoints)     { this.totalPoints = totalPoints; }
    public void setVersion(String version)          { this.version = version; }
    public void setName(String name)                { this.name = name; }
    public void setCriteria(List<RubricCriterion> c){ this.criteria = c; }
}
