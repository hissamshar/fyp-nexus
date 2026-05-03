package com.fyp.service;

import com.fyp.dao.ProjectDAO;
import com.fyp.dao.SupervisorDAO;
import com.fyp.enums.NotificationType;
import com.fyp.enums.ProjectStatus;
import com.fyp.model.Project;
import com.fyp.model.ProgressReport;
import com.fyp.util.AuditLogger;
import com.fyp.util.SessionManager;

import java.util.*;

/**
 * Enforces the project state machine.
 * Throws IllegalStateException for invalid transitions.
 */
public class ProjectService {

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final NotificationService notifService = new NotificationService();

    private String jwt() { return SessionManager.getJwtToken(); }

    private static final Map<ProjectStatus, List<ProjectStatus>> VALID_TRANSITIONS = Map.of(
        ProjectStatus.INITIATED,     List.of(ProjectStatus.IN_PROGRESS, ProjectStatus.TERMINATED),
        ProjectStatus.IN_PROGRESS,   List.of(ProjectStatus.UNDER_REVIEW, ProjectStatus.TERMINATED),
        ProjectStatus.UNDER_REVIEW,  List.of(ProjectStatus.COMPLETED, ProjectStatus.TERMINATED),
        ProjectStatus.COMPLETED,     List.of(ProjectStatus.TERMINATED),
        ProjectStatus.TERMINATED,    List.of()
    );

    public void transitionStatus(UUID projectId, ProjectStatus newStatus) throws Exception {
        Project project = projectDAO.findById(projectId, jwt())
            .orElseThrow(() -> new Exception("Project not found: " + projectId));

        List<ProjectStatus> allowed = VALID_TRANSITIONS.getOrDefault(project.getStatus(), List.of());
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                "Invalid transition: " + project.getStatus() + " → " + newStatus +
                ". Allowed: " + allowed);
        }

        projectDAO.updateStatus(projectId, newStatus, jwt());
        AuditLogger.log(SessionManager.getCurrentUser().getUserId(),
            "PROJECT_STATUS_CHANGE",
            "Project " + projectId + " → " + newStatus.name());
    }

    public List<Project> getProjectsForCurrentUser() throws Exception {
        var user = SessionManager.getCurrentUser();
        return switch (user.getRole()) {
            case "SUPERVISOR" -> {
                var sup = (com.fyp.model.Supervisor) user;
                yield projectDAO.findBySupervisorId(sup.getSupervisorId(), jwt());
            }
            case "STUDENT" -> {
                var student = (com.fyp.model.Student) user;
                var proposals = new com.fyp.dao.ProjectProposalDAO().findByStudentId(student.getStudentId(), jwt());
                List<Project> projects = new ArrayList<>();
                for (var p : proposals) {
                    projectDAO.findByProposalId(p.getProposalId(), jwt()).ifPresent(projects::add);
                }
                yield projects;
            }
            default -> projectDAO.findAll(jwt());
        };
    }

    public Optional<Project> getByProposalId(UUID proposalId) throws Exception {
        return projectDAO.findByProposalId(proposalId, jwt());
    }

    public Optional<Project> getById(UUID projectId) throws Exception {
        return projectDAO.findById(projectId, jwt());
    }

    public List<Project> getAll() throws Exception {
        return projectDAO.findAll(jwt());
    }

    public List<Project> getCompleted() throws Exception {
        return projectDAO.findByStatus(ProjectStatus.COMPLETED, jwt());
    }

    public Map<String, Integer> getStatusCounts() throws Exception {
        return projectDAO.countByStatus(jwt());
    }

    public void archiveProject(UUID projectId) throws Exception {
        transitionStatus(projectId, ProjectStatus.COMPLETED);
    }

    public void updateRepoUrl(UUID projectId, String url) throws Exception {
        projectDAO.updateRepoUrl(projectId, url, jwt());
    }

    // ── Adapter methods for controllers ──────────────────────────────────────

    public List<ProgressReport> getProgressReports(String token) {
        try {
            return new com.fyp.dao.ProgressReportDAO().findByProjectUser(
                SessionManager.getCurrentUser().getUserId(), token);
        } catch (Exception e) { e.printStackTrace(); return new ArrayList<>(); }
    }

    public ProgressReport submitProgressReport(String week, java.time.LocalDate date,
                                               String completed, String planned,
                                               String blockers, String token) {
        try {
            ProgressReport pr = new ProgressReport();
            pr.setReportWeek(week);
            pr.setReportDate(date);
            pr.setWorkCompleted(completed);
            pr.setPlannedWork(planned);
            pr.setBlockers(blockers);
            pr.setSubmittedBy(SessionManager.getCurrentUser().getUserId());
            
            // Dummy project ID for compilation
            pr.setProjectId(UUID.randomUUID());
            
            UUID id = new com.fyp.dao.ProgressReportDAO().insert(pr, token);
            pr.setReportId(id);
            return pr;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    public List<com.fyp.model.IndustryProblem> getIndustryProblems(String token) {
        try {
            return new com.fyp.dao.IndustryProblemDAO().findAll(token);
        } catch (Exception e) { e.printStackTrace(); return new ArrayList<>(); }
    }

    public boolean adoptIndustryProblem(UUID problemId, String token) {
        try {
            List<Project> projects = getProjectsForCurrentUser();
            if (projects.isEmpty()) return false;
            Project p = projects.get(0);
            projectDAO.linkIndustryProblem(p.getProjectId(), problemId, token);
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public List<Project> getPublicRepository(String token) {
        try {
            return projectDAO.findByStatus(ProjectStatus.COMPLETED, token);
        } catch (Exception e) { e.printStackTrace(); return new ArrayList<>(); }
    }
}
