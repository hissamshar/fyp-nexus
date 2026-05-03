package com.fyp.controller;

import com.fyp.model.*;
import com.fyp.service.ProposalService;
import com.fyp.service.MilestoneService;
import com.fyp.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ProposalFormController implements Initializable {

    @FXML private TextField titleField;
    @FXML private TextArea problemField, solutionField, outcomesField;
    @FXML private ComboBox<Supervisor> supervisorCombo;
    @FXML private TableView<ProjectProposal> proposalsTable;
    @FXML private TableColumn<ProjectProposal, String> titleCol, statusCol, supCol, dateCol;
    @FXML private Label errorLabel;

    private final ProposalService proposalService = new ProposalService();
    private final MilestoneService milestoneService = new MilestoneService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        titleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        supCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getSupervisorId() != null ? c.getValue().getSupervisorId().toString().substring(0, 8) + "…" : "—"));
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getSubmissionDate() != null ? c.getValue().getSubmissionDate().toString() : "—"));

        loadSupervisors();
        loadProposals();
    }

    private void loadSupervisors() {
        Task<List<Supervisor>> t = new Task<>() {
            @Override protected List<Supervisor> call() throws Exception {
                return proposalService.getAvailableSupervisors(SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> supervisorCombo.setItems(FXCollections.observableArrayList(t.getValue())));
        new Thread(t).start();
    }

    private void loadProposals() {
        Task<List<ProjectProposal>> t = new Task<>() {
            @Override protected List<ProjectProposal> call() throws Exception {
                return proposalService.getProposalsForCurrentUser();
            }
        };
        t.setOnSucceeded(e -> proposalsTable.getItems().setAll(t.getValue()));
        new Thread(t).start();
    }

    @FXML
    private void handleSaveDraft() {
        submitProposal(false);
    }

    @FXML
    private void handleSubmit() {
        if (titleField.getText().trim().isEmpty()) {
            showError("Title is required.");
            return;
        }
        if (supervisorCombo.getValue() == null) {
            showError("Please select a supervisor.");
            return;
        }
        submitProposal(true);
    }

    private void submitProposal(boolean submit) {
        hideError();
        String title    = titleField.getText().trim();
        String problem  = problemField.getText().trim();
        String solution = solutionField.getText().trim();
        String outcomes = outcomesField.getText().trim();
        Supervisor sup  = supervisorCombo.getValue();

        Task<ProjectProposal> t = new Task<>() {
            @Override protected ProjectProposal call() throws Exception {
                return proposalService.submitProposal(title, problem, solution, outcomes,
                    sup != null ? sup.getSupervisorId() : null,
                    SessionManager.getJwtToken(), submit);
            }
        };
        t.setOnSucceeded(e -> {
            if (t.getValue() != null) {
                showInfo(submit ? "Proposal submitted!" : "Draft saved!");
                clearForm();
                loadProposals();
            } else {
                showError("Failed to save proposal. Please try again.");
            }
        });
        t.setOnFailed(e -> showError("Error: " + t.getException().getMessage()));
        new Thread(t).start();
    }

    private void clearForm() {
        titleField.clear(); problemField.clear(); solutionField.clear();
        outcomesField.clear(); supervisorCombo.setValue(null);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showInfo(String msg) {
        errorLabel.setText(msg);
        errorLabel.setStyle("-fx-text-fill:#4ADE80; -fx-background-color:rgba(74,222,128,0.1); -fx-padding:8 12; -fx-background-radius:6;");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setStyle(null);
    }
}
