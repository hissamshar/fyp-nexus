package com.fyp.service;

import com.fyp.dao.*;
import com.fyp.enums.ProposalStatus;
import com.fyp.enums.NotificationType;
import com.fyp.model.*;
import com.fyp.util.AuditLogger;
import com.fyp.util.SessionManager;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for proposal submission and supervisor review.
 * Implements the exact sequence diagram from the spec.
 */
public class ProposalService {

    private final ProjectProposalDAO proposalDAO = new ProjectProposalDAO();
    private final ProjectDAO projectDAO = new ProjectDAO();
    private final UserDAO userDAO = new UserDAO();
    private final NotificationService notifService = new NotificationService();
    private final DiscussionDAO discussionDAO = new DiscussionDAO();

    /**
     * FR-05: Student submits a proposal (status = PENDING).
     * Validates all fields, inserts, notifies supervisor.
     */
    public UUID submitProposal(String title, String abstract_, String objectives,
                               String methodology, String expectedOutcome,
                               String description, UUID supervisorId) throws Exception {

        User current = SessionManager.getCurrentUser();
        if (!"STUDENT".equals(current.getRole()))
            throw new Exception("Only students can submit proposals.");

        Student student = (Student) current;

        // Validate required fields
        if (isBlank(title))           throw new Exception("FIELD_EMPTY:title");
        if (isBlank(abstract_))       throw new Exception("FIELD_EMPTY:abstract");
        if (isBlank(objectives))      throw new Exception("FIELD_EMPTY:objectives");
        if (isBlank(methodology))     throw new Exception("FIELD_EMPTY:methodology");
        if (isBlank(expectedOutcome)) throw new Exception("FIELD_EMPTY:expectedOutcome");
        if (supervisorId == null)     throw new Exception("FIELD_EMPTY:supervisor");

        ProjectProposal proposal = new ProjectProposal();
        proposal.setTitle(title.trim());
        proposal.setAbstract(abstract_.trim());
        proposal.setObjectives(objectives.trim());
        proposal.setMethodology(methodology.trim());
        proposal.setExpectedOutcome(expectedOutcome.trim());
        proposal.setDescription(description != null ? description.trim() : "");
        proposal.setSubmissionDate(LocalDate.now());
        proposal.setStatus(ProposalStatus.PENDING);
        proposal.setStudentId(student.getStudentId());
        proposal.setSupervisorId(supervisorId);

        UUID proposalId = proposalDAO.insert(proposal);
        AuditLogger.logProposalSubmit(current.getUserId(), proposalId);

        // Notify supervisor
        SupervisorDAO supervisorDAO = new SupervisorDAO();
        supervisorDAO.findBySupervisorId(supervisorId).ifPresent(sup -> {
            try {
                notifService.create(sup.getUserId(), NotificationType.PROPOSAL,
                    "New proposal submitted: \"" + title + "\"", sup.getEmail());
            } catch (Exception e) {
                System.err.println("[ProposalService] Notification error: " + e.getMessage());
            }
        });

        return proposalId;
    }

    /**
     * FR-05: Save proposal as draft (status = DRAFT).
     */
    public UUID saveDraft(String title, String abstract_, String objectives,
                          String methodology, String expectedOutcome,
                          String description, UUID supervisorId) throws Exception {

        User current = SessionManager.getCurrentUser();
        if (!"STUDENT".equals(current.getRole()))
            throw new Exception("Only students can save proposals.");

        Student student = (Student) current;

        ProjectProposal proposal = new ProjectProposal();
        proposal.setTitle(title != null ? title.trim() : "");
        proposal.setAbstract(abstract_ != null ? abstract_.trim() : "");
        proposal.setObjectives(objectives != null ? objectives.trim() : "");
        proposal.setMethodology(methodology != null ? methodology.trim() : "");
        proposal.setExpectedOutcome(expectedOutcome != null ? expectedOutcome.trim() : "");
        proposal.setDescription(description != null ? description.trim() : "");
        proposal.setStatus(ProposalStatus.DRAFT);
        proposal.setStudentId(student.getStudentId());
        proposal.setSupervisorId(supervisorId);

        return proposalDAO.insert(proposal);
    }

    /**
     * FR-06: Supervisor approves or rejects a proposal.
     * On approval, automatically creates a Project (status=INITIATED)
     * and creates a DiscussionBoard for the project.
     */
    public void reviewProposal(UUID proposalId, ProposalStatus decision, String comment) throws Exception {
        User current = SessionManager.getCurrentUser();
        if (!"SUPERVISOR".equals(current.getRole()) && !"ADMIN".equals(current.getRole()))
            throw new Exception("Only supervisors or admins can review proposals.");

        proposalDAO.updateStatus(proposalId, decision, comment);

        proposalDAO.findById(proposalId).ifPresent(proposal -> {
            try {
                if (decision == ProposalStatus.APPROVED) {
                    // Auto-create Project
                    UUID projectId = projectDAO.insert(proposalId);

                    // Auto-create discussion board for the project
                    discussionDAO.insertBoard(projectId, false);

                    // Notify student
                    StudentDAO sDAO = new StudentDAO();
                    sDAO.findByStudentId(proposal.getStudentId()).ifPresent(student -> {
                        try {
                            notifService.create(student.getUserId(), NotificationType.PROPOSAL,
                                "Your proposal \"" + proposal.getTitle() + "\" was APPROVED. Project created!",
                                student.getEmail());
                        } catch (Exception ignored) {}
                    });
                } else if (decision == ProposalStatus.REJECTED) {
                    StudentDAO sDAO = new StudentDAO();
                    sDAO.findByStudentId(proposal.getStudentId()).ifPresent(student -> {
                        try {
                            notifService.create(student.getUserId(), NotificationType.PROPOSAL,
                                "Your proposal \"" + proposal.getTitle() + "\" was REJECTED. Reason: " + comment,
                                student.getEmail());
                        } catch (Exception ignored) {}
                    });
                }
                AuditLogger.log(current.getUserId(), "PROPOSAL_REVIEW",
                    "Proposal " + proposalId + " => " + decision.name());
            } catch (Exception e) {
                System.err.println("[ProposalService] Error in reviewProposal: " + e.getMessage());
            }
        });
    }

    public List<ProjectProposal> getProposalsForCurrentUser() throws Exception {
        User current = SessionManager.getCurrentUser();
        if ("STUDENT".equals(current.getRole())) {
            return proposalDAO.findByStudentId(((Student) current).getStudentId());
        } else if ("SUPERVISOR".equals(current.getRole())) {
            return proposalDAO.findBySupervisorId(((Supervisor) current).getSupervisorId());
        }
        return proposalDAO.findAll();
    }

    public List<ProjectProposal> getPendingProposals() throws Exception {
        return proposalDAO.findByStatus(ProposalStatus.PENDING);
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}
