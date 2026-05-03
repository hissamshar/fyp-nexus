package com.fyp.service;

import com.fyp.dao.*;
import com.fyp.enums.NotificationType;
import com.fyp.model.*;
import com.fyp.util.AuditLogger;
import com.fyp.util.SessionManager;
import com.fyp.util.SupabaseClient;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpRequest;
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

    private String jwt() { return SessionManager.getJwtToken(); }

    private void requireAdmin() throws Exception {
        if (!"ADMIN".equals(SessionManager.getCurrentUser().getRole()))
            throw new Exception("Admin privileges required.");
    }

    public void activateUser(UUID userId) throws Exception {
        requireAdmin();
        userDAO.setActive(userId, true, jwt());
        AuditLogger.log(SessionManager.getCurrentUser().getUserId(),
            "ADMIN_USER_ACTIVATE", "userId=" + userId);
    }

    public void deactivateUser(UUID userId) throws Exception {
        requireAdmin();
        userDAO.setActive(userId, false, jwt());
        AuditLogger.log(SessionManager.getCurrentUser().getUserId(),
            "ADMIN_USER_DEACTIVATE", "userId=" + userId);
    }

    public List<User> getAllUsers() throws Exception {
        requireAdmin();
        return userDAO.findAll(jwt());
    }

    public List<User> getUsersByRole(String role) throws Exception {
        requireAdmin();
        return userDAO.findByRole(role, jwt());
    }

    public void assignSupervisor(UUID projectId, UUID supervisorId) throws Exception {
        requireAdmin();
        supervisorDAO.findBySupervisorId(supervisorId, jwt()).ifPresent(sup -> {
            if (sup.getSlotsAvailable() <= 0) {
                throw new IllegalStateException(
                    "Supervisor has reached the maximum student limit (5).");
            }
            try {
                // Insert into supervisor_assignments via REST
                JsonObject json = new JsonObject();
                json.addProperty("project_id", projectId.toString());
                json.addProperty("supervisor_id", supervisorId.toString());
                HttpRequest.Builder req = HttpRequest.newBuilder()
                        .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/supervisor_assignments"))
                        .header("Prefer", "resolution=ignore-duplicates")
                        .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
                SupabaseClient.sendAuthenticatedRequest(req, jwt());
                supervisorDAO.decrementSlots(supervisorId, jwt());
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
        examinerDAO.assignToProject(projectId, examinerId, jwt());
        notifService.create(examinerUserId, NotificationType.SYSTEM,
            "You have been assigned to evaluate project " + projectId, examinerEmail);
        AuditLogger.log(SessionManager.getCurrentUser().getUserId(),
            "EXAMINER_ASSIGNED", "project=" + projectId + " examiner=" + examinerId);
    }

    public UUID createDeadline(SemesterDeadline deadline) throws Exception {
        requireAdmin();
        UUID id = deadlineDAO.insert(deadline, jwt());
        // Notify all active users
        userDAO.findAll(jwt()).forEach(u -> {
            try {
                notifService.create(u.getUserId(), NotificationType.DEADLINE,
                    "Deadline set: " + deadline.getDeadlineType() + " — " + deadline.getDueDate(), null);
            } catch (Exception ignored) {}
        });
        return id;
    }

    public void updateDeadline(SemesterDeadline deadline) throws Exception {
        requireAdmin();
        deadlineDAO.update(deadline, jwt());
    }

    public List<SemesterDeadline> getDeadlines() throws Exception {
        return deadlineDAO.findAll(jwt());
    }

    public List<AuditLogEntry> getAuditLogs(int limit) throws Exception {
        requireAdmin();
        return auditLogDAO.findAll(limit, jwt());
    }

    public Map<String, Integer> getProjectStatusCounts() throws Exception {
        return projectDAO.countByStatus(jwt());
    }

    public int getTotalUsers() throws Exception {
        return userDAO.findAll(jwt()).size();
    }

    // ── Adapter methods for controllers ──────────────────────────────────────

    public Map<String, Object> getAnalyticsSummary(String token) {
        Map<String, Object> data = new java.util.HashMap<>();
        try {
            data.put("totalStudents",    userDAO.findByRole("STUDENT", token).size());
            data.put("activeProjects",   projectDAO.findAll(token).size());
            data.put("pendingProposals", new com.fyp.dao.ProjectProposalDAO()
                .findByStatus(com.fyp.enums.ProposalStatus.PENDING, token).size());
            data.put("avgGrade",         "—");

            Map<String, Long> statusMap = new java.util.LinkedHashMap<>();
            for (var entry : projectDAO.countByStatus(token).entrySet()) {
                statusMap.put(entry.getKey(), (long) entry.getValue());
            }
            data.put("projectsByStatus", statusMap);

            List<String[]> supLoad = new java.util.ArrayList<>();
            for (var s : supervisorDAO.findAll(token)) {
                supLoad.add(new String[]{
                    s.getName(),
                    String.valueOf(s.getSlotsAvailable()),
                    String.valueOf(5 - s.getSlotsAvailable())
                });
            }
            data.put("supervisorLoad", supLoad);
        } catch (Exception e) { e.printStackTrace(); }
        return data;
    }

    public String exportToCsv(String token) {
        try {
            String path = System.getProperty("user.home") + "/fyp_export_" +
                System.currentTimeMillis() + ".csv";
            StringBuilder sb = new StringBuilder("Title,Status,Supervisor\n");
            for (var p : projectDAO.findAll(token)) {
                sb.append(p.getTitle()).append(",").append(p.getStatus().name()).append(",")
                  .append(p.getSupervisorId()).append("\n");
            }
            java.nio.file.Files.writeString(java.nio.file.Path.of(path), sb.toString());
            return path;
        } catch (Exception e) { e.printStackTrace(); return "Export failed."; }
    }

    public String generatePdfReport(String token) {
        // Stub — wire to iText PDF library if available
        return System.getProperty("user.home") + "/fyp_report.pdf (PDF generation stub)";
    }

    public List<SemesterDeadline> getDeadlines(String token) {
        try { return deadlineDAO.findAll(token); }
        catch (Exception e) { e.printStackTrace(); return new java.util.ArrayList<>(); }
    }

    public SemesterDeadline upsertDeadline(String name, java.time.LocalDate date,
                                           String desc, String token) {
        try {
            SemesterDeadline d = new SemesterDeadline();
            d.setName(name);
            d.setDeadlineDate(date);
            d.setDescription(desc);
            deadlineDAO.insert(d, token);
            return d;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    public boolean deleteDeadline(UUID deadlineId, String token) {
        try { deadlineDAO.delete(deadlineId, token); return true; }
        catch (Exception e) { e.printStackTrace(); return false; }
    }
}
