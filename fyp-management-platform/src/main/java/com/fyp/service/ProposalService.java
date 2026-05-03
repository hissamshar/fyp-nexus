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
 */
public class ProposalService {

    private final ProjectProposalDAO proposalDAO = new ProjectProposalDAO();
    private final ProjectDAO projectDAO = new ProjectDAO();
    private final UserDAO userDAO = new UserDAO();
    private final NotificationService notifService = new NotificationService();
    private final DiscussionDAO discussionDAO = new DiscussionDAO();

    private String jwt() { return SessionManager.getJwtToken(); }

    public UUID submitProposal(String title, String abstract_, String objectives,
                               String methodology, String expectedOutcome,
                               String description, UUID supervisorId) throws Exception {

        User current = SessionManager.getCurrentUser();
        if (!"STUDENT".equals(current.getRole()))
            throw new Exception("Only students can submit proposals.");

        Student student = (current instanceof Student s) ? s
            : new StudentDAO().findByUserId(current.getUserId(), jwt())
                .orElseThrow(() -> new Exception("Student profile not found."));

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

        UUID proposalId = proposalDAO.insert(proposal, jwt());
        AuditLogger.logProposalSubmit(current.getUserId(), proposalId);

        SupervisorDAO supervisorDAO = new SupervisorDAO();
        supervisorDAO.findBySupervisorId(supervisorId, jwt()).ifPresent(sup -> {
            try {
                notifService.create(sup.getUserId(), NotificationType.PROPOSAL,
                    "New proposal submitted: \"" + title + "\"", sup.getEmail());
            } catch (Exception e) {
                System.err.println("[ProposalService] Notification error: " + e.getMessage());
            }
        });

        return proposalId;
    }

    public UUID saveDraft(String title, String abstract_, String objectives,
                          String methodology, String expectedOutcome,
                          String description, UUID supervisorId) throws Exception {

        User current = SessionManager.getCurrentUser();
        if (!"STUDENT".equals(current.getRole()))
            throw new Exception("Only students can save proposals.");

        Student student = (current instanceof Student s) ? s
            : new StudentDAO().findByUserId(current.getUserId(), jwt())
                .orElseThrow(() -> new Exception("Student profile not found."));

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

        return proposalDAO.insert(proposal, jwt());
    }

    public void reviewProposal(UUID proposalId, ProposalStatus decision, String comment) throws Exception {
        User current = SessionManager.getCurrentUser();
        if (!"SUPERVISOR".equals(current.getRole()) && !"ADMIN".equals(current.getRole()))
            throw new Exception("Only supervisors or admins can review proposals.");

        proposalDAO.updateStatus(proposalId, decision, comment, jwt());

        proposalDAO.findById(proposalId, jwt()).ifPresent(proposal -> {
            try {
                if (decision == ProposalStatus.APPROVED) {
                    UUID projectId = projectDAO.insert(proposalId, jwt());
                    discussionDAO.insertBoard(projectId, false, jwt());

                    StudentDAO sDAO = new StudentDAO();
                    sDAO.findByStudentId(proposal.getStudentId(), jwt()).ifPresent(student -> {
                        try {
                            notifService.create(student.getUserId(), NotificationType.PROPOSAL,
                                "Your proposal \"" + proposal.getTitle() + "\" was APPROVED. Project created!",
                                student.getEmail());
                        } catch (Exception ignored) {}
                    });
                } else if (decision == ProposalStatus.REJECTED) {
                    StudentDAO sDAO = new StudentDAO();
                    sDAO.findByStudentId(proposal.getStudentId(), jwt()).ifPresent(student -> {
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
            Student s = (current instanceof Student st) ? st
                : new StudentDAO().findByUserId(current.getUserId(), jwt()).orElseThrow();
            return proposalDAO.findByStudentId(s.getStudentId(), jwt());
        } else if ("SUPERVISOR".equals(current.getRole())) {
            Supervisor sv = (current instanceof Supervisor sup) ? sup
                : new SupervisorDAO().findByUserId(current.getUserId(), jwt()).orElseThrow();
            return proposalDAO.findBySupervisorId(sv.getSupervisorId(), jwt());
        }
        return proposalDAO.findAll(jwt());
    }

    public List<ProjectProposal> getPendingProposals() throws Exception {
        return proposalDAO.findByStatus(ProposalStatus.PENDING, jwt());
    }

    // ── Adapter methods for controllers ──────────────────────────────────────

    /** Fetch all supervisors visible to the current user. */
    public List<Supervisor> getAvailableSupervisors(String token) throws Exception {
        SupervisorDAO supDAO = new SupervisorDAO();
        return supDAO.findAll(token);
    }

    /** Supervisor-facing: get proposals assigned to the current supervisor. */
    public List<ProjectProposal> getProposalsForSupervisor(String token) throws Exception {
        User current = SessionManager.getCurrentUser();
        if ("SUPERVISOR".equals(current.getRole())) {
            Supervisor sv = (current instanceof Supervisor sup) ? sup
                : new SupervisorDAO().findByUserId(current.getUserId(), token).orElseThrow();
            return proposalDAO.findBySupervisorId(sv.getSupervisorId(), token);
        }
        return proposalDAO.findAll(token);
    }

    /**
     * Simple submit/draft helper used by ProposalFormController.
     * Maps problem/solution/outcomes to the richer domain model fields.
     */
    public ProjectProposal submitProposal(String title, String problem, String solution,
                                          String outcomes, UUID supervisorId,
                                          String token, boolean submit) throws Exception {
        User current = SessionManager.getCurrentUser();
        ProjectProposal p = new ProjectProposal();
        p.setTitle(title);
        p.setAbstract(problem);        // problem statement → abstract field
        p.setObjectives(solution);     // proposed solution → objectives field
        p.setExpectedOutcome(outcomes);
        p.setSupervisorId(supervisorId);
        p.setSubmissionDate(LocalDate.now());
        p.setStatus(submit ? ProposalStatus.PENDING : ProposalStatus.DRAFT);
        if ("STUDENT".equals(current.getRole())) {
            Student s = (current instanceof Student st) ? st
                : new StudentDAO().findByUserId(current.getUserId(), token).orElseThrow();
            p.setStudentId(s.getStudentId());
        }
        UUID id = proposalDAO.insert(p, token);
        p.setProposalId(id);
        return p;
    }

    /** String-based review action for ProposalReviewController. */
    public boolean reviewProposal(UUID proposalId, String action, String feedback, String token) {
        try {
            ProposalStatus status = switch (action) {
                case "APPROVED"            -> ProposalStatus.APPROVED;
                case "REJECTED"            -> ProposalStatus.REJECTED;
                case "REVISION_REQUESTED"  -> ProposalStatus.REVISION_REQUESTED;
                default                    -> ProposalStatus.PENDING;
            };
            proposalDAO.updateStatus(proposalId, status, feedback, token);
            if (status == ProposalStatus.APPROVED) {
                projectDAO.insert(proposalId, token);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}
