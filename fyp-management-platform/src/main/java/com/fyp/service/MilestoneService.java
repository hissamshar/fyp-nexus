package com.fyp.service;

import com.fyp.dao.MilestoneDAO;
import com.fyp.enums.MilestoneStatus;
import com.fyp.enums.NotificationType;
import com.fyp.model.Milestone;
import com.fyp.util.AuditLogger;
import com.fyp.util.SessionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class MilestoneService {

    private final MilestoneDAO milestoneDAO = new MilestoneDAO();
    private final NotificationService notifService = new NotificationService();

    private String jwt() { return SessionManager.getJwtToken(); }

    public UUID createMilestone(UUID projectId, String title, String description,
                                LocalDateTime deadline, int weightage) throws Exception {
        var user = SessionManager.getCurrentUser();
        if (!"SUPERVISOR".equals(user.getRole()) && !"ADMIN".equals(user.getRole()))
            throw new Exception("Only supervisors or admins can create milestones.");

        Milestone m = new Milestone();
        m.setProjectId(projectId);
        m.setTitle(title.trim());
        m.setDescription(description);
        m.setDeadline(deadline);
        m.setWeightage(weightage);
        m.setStatus(MilestoneStatus.PENDING);
        return milestoneDAO.insert(m, jwt());
    }

    public void markComplete(UUID milestoneId) throws Exception {
        milestoneDAO.updateStatus(milestoneId, MilestoneStatus.COMPLETED, jwt());
        AuditLogger.log(SessionManager.getCurrentUser().getUserId(),
            "MILESTONE_COMPLETE", "Milestone " + milestoneId);
    }

    public void markInProgress(UUID milestoneId) throws Exception {
        milestoneDAO.updateStatus(milestoneId, MilestoneStatus.IN_PROGRESS, jwt());
    }

    public void checkOverdue(UUID projectId) throws Exception {
        List<Milestone> milestones = milestoneDAO.findByProjectId(projectId, jwt());
        for (Milestone m : milestones) {
            if (m.getStatus() != MilestoneStatus.COMPLETED && m.checkOverdue()) {
                milestoneDAO.updateStatus(m.getMilestoneId(), MilestoneStatus.OVERDUE, jwt());
            }
        }
    }

    public float calculateProgress(UUID projectId) throws Exception {
        int[] counts = milestoneDAO.getProgressCounts(projectId, jwt());
        if (counts[1] == 0) return 0.0f;
        return (float) counts[0] / counts[1] * 100f;
    }

    public List<Milestone> getMilestonesForProject(UUID projectId) throws Exception {
        return milestoneDAO.findByProjectId(projectId, jwt());
    }

    public void deleteMilestone(UUID milestoneId) throws Exception {
        if (!"SUPERVISOR".equals(SessionManager.getCurrentUser().getRole()) &&
            !"ADMIN".equals(SessionManager.getCurrentUser().getRole()))
            throw new Exception("Unauthorized.");
        milestoneDAO.delete(milestoneId, jwt());
    }

    public void updateMilestone(Milestone m) throws Exception {
        milestoneDAO.update(m, jwt());
    }

    // ── Adapter methods for controllers ──────────────────────────────────────

    /** For students: get all milestones across their projects. */
    public List<Milestone> getMilestonesForCurrentUser(String token) {
        try {
            var user = SessionManager.getCurrentUser();
            if ("STUDENT".equals(user.getRole())) {
                var proposals = new com.fyp.dao.ProjectProposalDAO()
                    .findByStudentId(((com.fyp.model.Student) user).getStudentId(), token);
                List<Milestone> all = new java.util.ArrayList<>();
                for (var prop : proposals) {
                    new com.fyp.dao.ProjectDAO().findByProposalId(prop.getProposalId(), token)
                        .ifPresent(p -> {
                            try {
                                all.addAll(milestoneDAO.findByProjectId(p.getProjectId(), token));
                            } catch (Exception ignored) {}
                        });
                }
                return all;
            }
            // Supervisors/admins see all
            return milestoneDAO.findAll(token);
        } catch (Exception e) { e.printStackTrace(); return new java.util.ArrayList<>(); }
    }

    /** Simplified create for student/controller use (no weightage/project required). */
    public Milestone createMilestone(String title, String description,
                                     java.time.LocalDate dueDate, String token) {
        try {
            Milestone m = new Milestone();
            m.setTitle(title);
            m.setDescription(description);
            m.setDueDate(dueDate);
            m.setStatus(MilestoneStatus.PENDING);
            UUID id = milestoneDAO.insert(m, token);
            m.setMilestoneId(id);
            return m;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    /** Boolean-return mark-complete for controller. */
    public boolean markComplete(UUID milestoneId, String token) {
        try {
            milestoneDAO.updateStatus(milestoneId, MilestoneStatus.COMPLETED, token);
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    /** Fetch all milestones — for admin/supervisor. */
    public List<Milestone> findAll(String token) {
        try { return milestoneDAO.findAll(token); }
        catch (Exception e) { e.printStackTrace(); return new java.util.ArrayList<>(); }
    }
}
