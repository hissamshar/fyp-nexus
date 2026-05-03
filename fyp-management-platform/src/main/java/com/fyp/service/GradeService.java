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

    private String jwt() { return SessionManager.getJwtToken(); }

    public UUID getOrCreateGrade(UUID projectId, UUID rubricId) throws Exception {
        Optional<Grade> existing = gradeDAO.findByProjectId(projectId, jwt());
        if (existing.isPresent()) return existing.get().getGradeId();
        return gradeDAO.insertGrade(projectId, rubricId, jwt());
    }

    public UUID submitEntry(UUID gradeId, UUID criterionId, int score,
                            String comments) throws Exception {
        var user = SessionManager.getCurrentUser();
        String role = user.getRole();
        if (!"SUPERVISOR".equals(role) && !"EXAMINER".equals(role))
            throw new Exception("Only supervisors or examiners can grade.");

        // Validate score against criterion max
        rubricDAO.findCriteriaByRubricId(gradeDAO.findEntriesByGradeId(gradeId, jwt())
            .stream().findFirst().map(e -> e.getGradeId()).orElse(gradeId), jwt())
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

        UUID id = gradeDAO.insertGradeEntry(entry, jwt());
        AuditLogger.logGradeChange(user.getUserId(), gradeId);
        return id;
    }

    public double calculateTotal(UUID gradeId) throws Exception {
        return gradeDAO.calculateWeightedTotal(gradeId, jwt());
    }

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

    public void publishGrade(UUID gradeId, UUID projectId, UUID studentUserId,
                             String studentEmail) throws Exception {
        Grade g = gradeDAO.findByProjectId(projectId, jwt())
            .orElseThrow(() -> new Exception("Grade not found for project."));
        double total = gradeDAO.calculateWeightedTotal(gradeId, jwt());
        String letter = scoreToLetterGrade(total, 100);
        gradeDAO.publishGrade(gradeId, letter, jwt());
        AuditLogger.logGradeChange(SessionManager.getCurrentUser().getUserId(), gradeId);
        notifService.create(studentUserId, NotificationType.GRADE,
            "Your grade has been published: " + letter, studentEmail);
    }

    public Optional<Grade> getGradeForProject(UUID projectId) throws Exception {
        return gradeDAO.findByProjectId(projectId, jwt());
    }

    public List<GradeEntry> getEntries(UUID gradeId) throws Exception {
        return gradeDAO.findEntriesByGradeId(gradeId, jwt());
    }

    public List<Rubric> getAllRubrics() throws Exception {
        return rubricDAO.findAll(jwt());
    }

    public Optional<Rubric> getRubric(UUID rubricId) throws Exception {
        return rubricDAO.findById(rubricId, jwt());
    }

    public UUID createRubric(String name, int totalPoints, String version) throws Exception {
        return rubricDAO.insertRubric(name, totalPoints, version, jwt());
    }

    public UUID addCriterion(UUID rubricId, String name, int maxScore, String desc) throws Exception {
        RubricCriterion c = new RubricCriterion();
        c.setRubricId(rubricId);
        c.setCriterionName(name);
        c.setMaxScore(maxScore);
        c.setDescription(desc);
        return rubricDAO.insertCriterion(c, jwt());
    }

    // ── Adapter methods for GradingController ─────────────────────────────────

    public List<Project> getProjectsForGrader(String token) {
        try {
            return new com.fyp.dao.ProjectDAO().findAll(token);
        } catch (Exception e) { e.printStackTrace(); return java.util.List.of(); }
    }

    public Rubric getRubricForProject(UUID projectId, String token) {
        try {
            return rubricDAO.findByProjectId(projectId, token).orElse(null);
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    public boolean saveGrades(UUID projectId, List<RubricCriterion> criteria,
                              String feedback, boolean publish, String token) {
        try {
            // Upsert or create the grade record
            Optional<Grade> existing = gradeDAO.findByProjectId(projectId, token);
            UUID gradeId;
            if (existing.isPresent()) {
                gradeId = existing.get().getGradeId();
            } else {
                // Find or create a default rubric id
                gradeId = gradeDAO.insertGrade(projectId, null, token);
            }
            if (publish) {
                int total = criteria.stream().mapToInt(RubricCriterion::getMaxScore).sum();
                String letter = scoreToLetterGrade(total, total);
                gradeDAO.publishGrade(gradeId, letter, token);
            }
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }
}
