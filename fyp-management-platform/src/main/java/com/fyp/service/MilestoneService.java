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
        return milestoneDAO.insert(m);
    }

    public void markComplete(UUID milestoneId) throws Exception {
        milestoneDAO.updateStatus(milestoneId, MilestoneStatus.COMPLETED);
        AuditLogger.log(SessionManager.getCurrentUser().getUserId(),
            "MILESTONE_COMPLETE", "Milestone " + milestoneId);
    }

    public void markInProgress(UUID milestoneId) throws Exception {
        milestoneDAO.updateStatus(milestoneId, MilestoneStatus.IN_PROGRESS);
    }

    /** Check and update overdue milestones for a project. */
    public void checkOverdue(UUID projectId) throws Exception {
        List<Milestone> milestones = milestoneDAO.findByProjectId(projectId);
        for (Milestone m : milestones) {
            if (m.getStatus() != MilestoneStatus.COMPLETED && m.checkOverdue()) {
                milestoneDAO.updateStatus(m.getMilestoneId(), MilestoneStatus.OVERDUE);
            }
        }
    }

    /** Returns (completed, total) for progress bar calculation. */
    public float calculateProgress(UUID projectId) throws Exception {
        int[] counts = milestoneDAO.getProgressCounts(projectId);
        if (counts[1] == 0) return 0.0f;
        return (float) counts[0] / counts[1] * 100f;
    }

    public List<Milestone> getMilestonesForProject(UUID projectId) throws Exception {
        return milestoneDAO.findByProjectId(projectId);
    }

    public void deleteMilestone(UUID milestoneId) throws Exception {
        if (!"SUPERVISOR".equals(SessionManager.getCurrentUser().getRole()) &&
            !"ADMIN".equals(SessionManager.getCurrentUser().getRole()))
            throw new Exception("Unauthorized.");
        milestoneDAO.delete(milestoneId);
    }

    public void updateMilestone(Milestone m) throws Exception {
        milestoneDAO.update(m);
    }
}
