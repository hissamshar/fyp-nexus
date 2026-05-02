package com.fyp.service;

import com.fyp.dao.ProjectDAO;
import com.fyp.dao.SupervisorDAO;
import com.fyp.enums.NotificationType;
import com.fyp.enums.ProjectStatus;
import com.fyp.model.Project;
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

    // Valid state transitions per spec
    private static final Map<ProjectStatus, List<ProjectStatus>> VALID_TRANSITIONS = Map.of(
        ProjectStatus.INITIATED,     List.of(ProjectStatus.IN_PROGRESS, ProjectStatus.TERMINATED),
        ProjectStatus.IN_PROGRESS,   List.of(ProjectStatus.UNDER_REVIEW, ProjectStatus.TERMINATED),
        ProjectStatus.UNDER_REVIEW,  List.of(ProjectStatus.COMPLETED, ProjectStatus.TERMINATED),
        ProjectStatus.COMPLETED,     List.of(ProjectStatus.TERMINATED),
        ProjectStatus.TERMINATED,    List.of()
    );

    /**
     * Transition project to a new status, enforcing the state machine.
     */
    public void transitionStatus(UUID projectId, ProjectStatus newStatus) throws Exception {
        Project project = projectDAO.findById(projectId)
            .orElseThrow(() -> new Exception("Project not found: " + projectId));

        List<ProjectStatus> allowed = VALID_TRANSITIONS.getOrDefault(project.getStatus(), List.of());
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                "Invalid transition: " + project.getStatus() + " → " + newStatus +
                ". Allowed: " + allowed);
        }

        projectDAO.updateStatus(projectId, newStatus);
        AuditLogger.log(SessionManager.getCurrentUser().getUserId(),
            "PROJECT_STATUS_CHANGE",
            "Project " + projectId + " → " + newStatus.name());
    }

    public List<Project> getProjectsForCurrentUser() throws Exception {
        var user = SessionManager.getCurrentUser();
        return switch (user.getRole()) {
            case "SUPERVISOR" -> {
                var sup = (com.fyp.model.Supervisor) user;
                yield projectDAO.findBySupervisorId(sup.getSupervisorId());
            }
            case "STUDENT" -> {
                // Find projects linked to the student's proposals
                var student = (com.fyp.model.Student) user;
                var proposals = new com.fyp.dao.ProjectProposalDAO().findByStudentId(student.getStudentId());
                List<Project> projects = new ArrayList<>();
                for (var p : proposals) {
                    projectDAO.findByProposalId(p.getProposalId()).ifPresent(projects::add);
                }
                yield projects;
            }
            default -> projectDAO.findAll();
        };
    }

    public Optional<Project> getByProposalId(UUID proposalId) throws Exception {
        return projectDAO.findByProposalId(proposalId);
    }

    public Optional<Project> getById(UUID projectId) throws Exception {
        return projectDAO.findById(projectId);
    }

    public List<Project> getAll() throws Exception {
        return projectDAO.findAll();
    }

    public List<Project> getCompleted() throws Exception {
        return projectDAO.findByStatus(ProjectStatus.COMPLETED);
    }

    public Map<String, Integer> getStatusCounts() throws Exception {
        return projectDAO.countByStatus();
    }

    public void archiveProject(UUID projectId) throws Exception {
        transitionStatus(projectId, ProjectStatus.COMPLETED);
    }

    public void updateRepoUrl(UUID projectId, String url) throws Exception {
        projectDAO.updateRepoUrl(projectId, url);
    }
}
