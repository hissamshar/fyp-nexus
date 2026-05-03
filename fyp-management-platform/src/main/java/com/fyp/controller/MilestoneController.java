package com.fyp.controller;

import com.fyp.model.Milestone;
import com.fyp.service.MilestoneService;
import com.fyp.util.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class MilestoneController implements Initializable {

    @FXML private TextField mTitleField;
    @FXML private TextArea mDescField;
    @FXML private DatePicker mDueDatePicker;
    @FXML private VBox milestonesContainer;
    @FXML private Label errorLabel;

    private final MilestoneService milestoneService = new MilestoneService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadMilestones();
    }

    private void loadMilestones() {
        Task<List<Milestone>> t = new Task<>() {
            @Override protected List<Milestone> call() throws Exception {
                return milestoneService.getMilestonesForCurrentUser(SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> Platform.runLater(() -> renderMilestones(t.getValue())));
        t.setOnFailed(e -> Platform.runLater(() ->
            showError("Failed to load milestones: " + t.getException().getMessage())));
        new Thread(t).start();
    }

    private void renderMilestones(List<Milestone> milestones) {
        milestonesContainer.getChildren().clear();
        if (milestones.isEmpty()) {
            Label empty = new Label("No milestones yet. Add one below!");
            empty.setStyle("-fx-text-fill:#64748B; -fx-font-size:14;");
            milestonesContainer.getChildren().add(empty);
            return;
        }
        for (Milestone m : milestones) {
            milestonesContainer.getChildren().add(buildMilestoneCard(m));
        }
    }

    private VBox buildMilestoneCard(Milestone m) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        String borderColor = switch (m.getStatus()) {
            case COMPLETED -> "#10B981";
            case OVERDUE   -> "#EF4444";
            default        -> "#334155";
        };
        card.setStyle("-fx-background-color:#1E293B; -fx-background-radius:10;" +
            "-fx-border-color:" + borderColor + "; -fx-border-radius:10; -fx-border-width: 0 0 0 4;");

        Label title  = new Label("🎯  " + m.getTitle());
        title.setStyle("-fx-text-fill:#F8FAFC; -fx-font-size:15; -fx-font-weight:bold;");

        String duePart = m.getDueDate() != null ? "  •  Due: " + m.getDueDate() : "";
        Label status = new Label("Status: " + m.getStatus().name() + duePart);
        status.setStyle("-fx-text-fill:#94A3B8; -fx-font-size:13;");

        Label desc = new Label(m.getDescription() != null ? m.getDescription() : "");
        desc.setStyle("-fx-text-fill:#CBD5E1; -fx-font-size:13;");
        desc.setWrapText(true);

        HBox actions = new HBox(8);
        Button markDone = new Button("✅ Mark Complete");
        markDone.getStyleClass().add("btn-success");
        markDone.setOnAction(e -> markComplete(m));
        if (m.getStatus().name().equals("COMPLETED")) markDone.setDisable(true);
        actions.getChildren().add(markDone);

        card.getChildren().addAll(title, status, desc, actions);
        return card;
    }

    private void markComplete(Milestone m) {
        Task<Boolean> t = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return milestoneService.markComplete(m.getMilestoneId(), SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> Platform.runLater(() -> { if (t.getValue()) loadMilestones(); }));
        t.setOnFailed(e -> Platform.runLater(() -> showError("Failed to update milestone.")));
        new Thread(t).start();
    }

    @FXML
    private void handleAddMilestone() {
        String title = mTitleField.getText().trim();
        if (title.isEmpty()) { showError("Title is required."); return; }
        LocalDate due = mDueDatePicker.getValue();
        String desc = mDescField.getText().trim();
        hideError();

        Task<Milestone> t = new Task<>() {
            @Override protected Milestone call() throws Exception {
                return milestoneService.createMilestone(title, desc, due, SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> Platform.runLater(() -> {
            if (t.getValue() != null) {
                mTitleField.clear();
                mDescField.clear();
                mDueDatePicker.setValue(null);
                loadMilestones();
            } else {
                showError("Failed to add milestone. Make sure you have an approved project.");
            }
        }));
        t.setOnFailed(e -> Platform.runLater(() ->
            showError(t.getException() != null
                ? t.getException().getMessage()
                : "Failed to add milestone.")));
        new Thread(t).start();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setStyle("-fx-text-fill:#F87171;-fx-background-color:rgba(248,113,113,0.1);-fx-padding:8 12;-fx-background-radius:6;");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
