package com.fyp.service;

import com.fyp.dao.GradeDAO;
import com.fyp.dao.RubricDAO;
import com.fyp.enums.NotificationType;
import com.fyp.model.*;
import com.fyp.util.AuditLogger;
import com.fyp.util.SessionManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GradeService {

    private final GradeDAO gradeDAO = new GradeDAO();
    private final RubricDAO rubricDAO = new RubricDAO();
    private final NotificationService notifService = new NotificationService();

    /** Create or get a grade record for a project. */
    public UUID getOrCreateGrade(UUID projectId, UUID rubricId) throws Exception {
        Optional<Grade> existing = gradeDAO.findByProjectId(projectId);
        if (existing.isPresent()) return existing.get().getGradeId();
        return gradeDAO.insertGrade(projectId, rubricId);
    }

    /** Submit a score for one rubric criterion. */
    public UUID submitEntry(UUID gradeId, UUID criterionId, int score,
                            String comments) throws Exception {
        var user = SessionManager.getCurrentUser();
        String role = user.getRole();
        if (!"SUPERVISOR".equals(role) && !"EXAMINER".equals(role))
            throw new Exception("Only supervisors or examiners can grade.");

        // Validate score against criterion max
        rubricDAO.findCriteriaByRubricId(gradeDAO.findEntriesByGradeId(gradeId)
            .stream().findFirst().map(e -> e.getGradeId()).orElse(gradeId))
            .stream().filter(crit -> crit.getCriterionId().equals(criterionId))
            .findFirst().ifPresent(crit -> {
                if (!crit.validateScore(score))
                    throw new IllegalArgumentException(
                        "Score " + score + " exceeds max " + crit.getMaxScore() + " for criterion.");
            });

        GradeEntry entry = new GradeEntry();
        entry.setGradeId(gradeId);
        entry.setCriterionId(criterionId);
        entry.setScore(score);
        entry.setComments(comments);
        entry.setGraderId(user.getUserId());

        UUID id = gradeDAO.insertGradeEntry(entry);
        AuditLogger.logGradeChange(user.getUserId(), gradeId);
        return id;
    }

    /** Calculate weighted total score for a grade. */
    public double calculateTotal(UUID gradeId) throws Exception {
        return gradeDAO.calculateWeightedTotal(gradeId);
    }

    /** Convert numeric score to letter grade. */
    public String scoreToLetterGrade(double score, double maxScore) {
        double pct = (score / maxScore) * 100;
        if (pct >= 90) return "A+";
        if (pct >= 85) return "A";
        if (pct >= 80) return "A-";
        if (pct >= 75) return "B+";
        if (pct >= 70) return "B";
        if (pct >= 65) return "B-";
        if (pct >= 60) return "C+";
        if (pct >= 55) return "C";
        return "F";
    }

    /** Publish a grade — makes it visible to the student. */
    public void publishGrade(UUID gradeId, UUID projectId, UUID studentUserId,
                             String studentEmail) throws Exception {
        Grade g = gradeDAO.findByProjectId(projectId)
            .orElseThrow(() -> new Exception("Grade not found for project."));
        double total = gradeDAO.calculateWeightedTotal(gradeId);
        String letter = scoreToLetterGrade(total, 100);
        gradeDAO.publishGrade(gradeId, letter);
        AuditLogger.logGradeChange(SessionManager.getCurrentUser().getUserId(), gradeId);
        notifService.create(studentUserId, NotificationType.GRADE,
            "Your grade has been published: " + letter, studentEmail);
    }

    public Optional<Grade> getGradeForProject(UUID projectId) throws Exception {
        return gradeDAO.findByProjectId(projectId);
    }

    public List<GradeEntry> getEntries(UUID gradeId) throws Exception {
        return gradeDAO.findEntriesByGradeId(gradeId);
    }

    public List<Rubric> getAllRubrics() throws Exception {
        return rubricDAO.findAll();
    }

    public Optional<Rubric> getRubric(UUID rubricId) throws Exception {
        return rubricDAO.findById(rubricId);
    }

    public UUID createRubric(String name, int totalPoints, String version) throws Exception {
        return rubricDAO.insertRubric(name, totalPoints, version);
    }

    public UUID addCriterion(UUID rubricId, String name, int maxScore, String desc) throws Exception {
        RubricCriterion c = new RubricCriterion();
        c.setRubricId(rubricId);
        c.setCriterionName(name);
        c.setMaxScore(maxScore);
        c.setDescription(desc);
        return rubricDAO.insertCriterion(c);
    }
}
