package com.fyp.service;

import com.fyp.dao.*;
import com.fyp.enums.NotificationType;
import com.fyp.model.*;
import com.fyp.util.AuditLogger;
import com.fyp.util.SessionManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminService {

    private final UserDAO userDAO = new UserDAO();
    private final ProjectDAO projectDAO = new ProjectDAO();
    private final SemesterDeadlineDAO deadlineDAO = new SemesterDeadlineDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final SupervisorDAO supervisorDAO = new SupervisorDAO();
    private final ExaminerDAO examinerDAO = new ExaminerDAO();
    private final NotificationService notifService = new NotificationService();

    private void requireAdmin() throws Exception {
        if (!"ADMIN".equals(SessionManager.getCurrentUser().getRole()))
            throw new Exception("Admin privileges required.");
    }

    public void activateUser(UUID userId) throws Exception {
        requireAdmin();
        userDAO.setActive(userId, true);
        AuditLogger.log(SessionManager.getCurrentUser().getUserId(),
            "ADMIN_USER_ACTIVATE", "userId=" + userId);
    }

    public void deactivateUser(UUID userId) throws Exception {
        requireAdmin();
        userDAO.setActive(userId, false);
        AuditLogger.log(SessionManager.getCurrentUser().getUserId(),
            "ADMIN_USER_DEACTIVATE", "userId=" + userId);
    }

    public List<User> getAllUsers() throws Exception {
        requireAdmin();
        return userDAO.findAll();
    }

    public List<User> getUsersByRole(String role) throws Exception {
        requireAdmin();
        return userDAO.findByRole(role);
    }

    public void assignSupervisor(UUID projectId, UUID supervisorId) throws Exception {
        requireAdmin();
        String sql_check = "SELECT slots_available FROM fyp.supervisors WHERE supervisor_id = ?::uuid";
        supervisorDAO.findBySupervisorId(supervisorId).ifPresent(sup -> {
            if (sup.getSlotsAvailable() <= 0) {
                throw new IllegalStateException(
                    "Supervisor has reached the maximum student limit (5).");
            }
            try {
                var conn = com.fyp.util.DBConnection.getConnection();
                var ps = conn.prepareStatement(
                    "INSERT INTO fyp.supervisor_assignments (project_id, supervisor_id) " +
                    "VALUES (?::uuid, ?::uuid) ON CONFLICT DO NOTHING");
                ps.setString(1, projectId.toString());
                ps.setString(2, supervisorId.toString());
                ps.executeUpdate();
                conn.close();
                supervisorDAO.decrementSlots(supervisorId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        AuditLogger.log(SessionManager.getCurrentUser().getUserId(),
            "SUPERVISOR_ASSIGNED", "project=" + projectId + " supervisor=" + supervisorId);
    }

    public void assignExaminer(UUID projectId, UUID examinerId,
                               UUID examinerUserId, String examinerEmail) throws Exception {
        requireAdmin();
        examinerDAO.assignToProject(projectId, examinerId);
        notifService.create(examinerUserId, NotificationType.SYSTEM,
            "You have been assigned to evaluate project " + projectId, examinerEmail);
        AuditLogger.log(SessionManager.getCurrentUser().getUserId(),
            "EXAMINER_ASSIGNED", "project=" + projectId + " examiner=" + examinerId);
    }

    public UUID createDeadline(SemesterDeadline deadline) throws Exception {
        requireAdmin();
        UUID id = deadlineDAO.insert(deadline);
        // Notify all active users
        userDAO.findAll().forEach(u -> {
            try {
                notifService.create(u.getUserId(), NotificationType.DEADLINE,
                    "Deadline set: " + deadline.getDeadlineType() + " — " + deadline.getDueDate(), null);
            } catch (Exception ignored) {}
        });
        return id;
    }

    public void updateDeadline(SemesterDeadline deadline) throws Exception {
        requireAdmin();
        deadlineDAO.update(deadline);
    }

    public List<SemesterDeadline> getDeadlines() throws Exception {
        return deadlineDAO.findAll();
    }

    public List<AuditLogEntry> getAuditLogs(int limit) throws Exception {
        requireAdmin();
        return auditLogDAO.findAll(limit);
    }

    public Map<String, Integer> getProjectStatusCounts() throws Exception {
        return projectDAO.countByStatus();
    }

    public int getTotalUsers() throws Exception {
        return userDAO.findAll().size();
    }
}
