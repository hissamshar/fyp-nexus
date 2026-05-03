package com.fyp.controller;

import com.fyp.model.ProjectProposal;
import com.fyp.enums.ProposalStatus;
import com.fyp.service.ProposalService;
import com.fyp.util.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ProposalReviewController implements Initializable {

    @FXML private ComboBox<String> statusFilter;
    @FXML private ListView<ProjectProposal> proposalsList;
    @FXML private Label detailTitle, detailStudent, statusLabel, errorLabel;
    @FXML private TextArea detailProblem, detailSolution, feedbackField;

    private final ProposalService proposalService = new ProposalService();
    private ProjectProposal selected;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statusFilter.setItems(FXCollections.observableArrayList("All", "PENDING", "UNDER_REVIEW", "APPROVED", "REJECTED"));
        statusFilter.setValue("PENDING");

        proposalsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ProjectProposal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    // Fix Issue 11 — clear everything on empty cells
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                } else {
                    setText(item.getTitle());
                    Label statusBadge = new Label("[" + item.getStatus().name() + "]");
                    statusBadge.setStyle("-fx-text-fill:#818CF8; -fx-font-size:11;");
                    setGraphic(statusBadge);
                }
            }
        });

        proposalsList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) showDetail(n);
        });

        loadProposals();
    }

    private void loadProposals() {
        Task<List<ProjectProposal>> t = new Task<>() {
            @Override protected List<ProjectProposal> call() throws Exception {
                return proposalService.getProposalsForSupervisor(SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> Platform.runLater(() -> {
            List<ProjectProposal> proposals = t.getValue();
            proposalsList.setItems(FXCollections.observableArrayList(proposals));
            // Auto-select if a proposal was passed via SessionManager context
            Object ctx = SessionManager.getContext("selectedProposalId");
            if (ctx != null) {
                String idStr = ctx.toString();
                proposals.stream()
                    .filter(p -> p.getProposalId() != null && p.getProposalId().toString().equals(idStr))
                    .findFirst()
                    .ifPresent(p -> {
                        proposalsList.getSelectionModel().select(p);
                        proposalsList.scrollTo(p);
                    });
                SessionManager.clearContext("selectedProposalId");
            } else if (!proposals.isEmpty()) {
                // Auto-select the first item if nothing specific was requested
                proposalsList.getSelectionModel().selectFirst();
            }
        }));
        t.setOnFailed(e -> Platform.runLater(() -> showError("Failed to load proposals.")));
        new Thread(t).start();
    }

    private void showDetail(ProjectProposal p) {
        selected = p;
        detailTitle.setText(p.getTitle());
        detailStudent.setText(p.getStudentName());
        detailProblem.setText(p.getAbstract() != null ? p.getAbstract() : p.getDescription());
        detailSolution.setText(p.getObjectives() != null ? p.getObjectives() : "");
        statusLabel.setText("Current status: " + p.getStatus().name());
        feedbackField.clear();
        hideError();
    }

    @FXML private void handleRefresh() { loadProposals(); }

    @FXML private void handleApprove() { reviewSelected("APPROVED"); }
    @FXML private void handleReject()  { reviewSelected("REJECTED"); }
    @FXML private void handleRevise()  { reviewSelected("REVISION_REQUESTED"); }

    private void reviewSelected(String action) {
        if (selected == null) { showError("Select a proposal first."); return; }
        String feedback = feedbackField.getText().trim();
        Task<Boolean> t = new Task<>() {
            @Override protected Boolean call() throws Exception {
                ProposalStatus status = switch (action) {
                    case "APPROVED"            -> ProposalStatus.APPROVED;
                    case "REJECTED"            -> ProposalStatus.REJECTED;
                    case "REVISION_REQUESTED"  -> ProposalStatus.REVISION_REQUESTED;
                    default                    -> ProposalStatus.PENDING;
                };
                proposalService.reviewProposal(selected.getProposalId(), status, feedback);
                return true;
            }
        };
        t.setOnSucceeded(e -> Platform.runLater(() -> {
            if (t.getValue()) {
                showInfo("Proposal " + action.toLowerCase().replace("_", " ") + " successfully.");
                loadProposals();
            } else {
                showError("Action failed. Please try again.");
            }
        }));
        t.setOnFailed(e -> Platform.runLater(() -> showError("Error: " + t.getException().getMessage())));
        new Thread(t).start();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setStyle("-fx-text-fill:#F87171;-fx-background-color:rgba(248,113,113,0.1);-fx-padding:8 12;-fx-background-radius:6;");
        errorLabel.setVisible(true); errorLabel.setManaged(true);
    }
    private void showInfo(String msg) {
        errorLabel.setText(msg);
        errorLabel.setStyle("-fx-text-fill:#4ADE80;-fx-background-color:rgba(74,222,128,0.1);-fx-padding:8 12;-fx-background-radius:6;");
        errorLabel.setVisible(true); errorLabel.setManaged(true);
    }
    private void hideError() { errorLabel.setVisible(false); errorLabel.setManaged(false); }
}
