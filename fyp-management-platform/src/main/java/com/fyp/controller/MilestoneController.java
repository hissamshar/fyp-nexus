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
import javafx.scene.paint.Color;

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
        card.setStyle("-fx-background-color:#1E293B; -fx-background-radius:10; -fx-border-color:" +
            (m.getStatus().name().equals("COMPLETED") ? "#10B981" :
             m.getStatus().name().equals("OVERDUE")   ? "#EF4444" : "#334155") + ";" +
            "-fx-border-radius:10; -fx-border-width: 0 0 0 4;");

        Label title  = new Label("🎯  " + m.getTitle());
        title.setStyle("-fx-text-fill:#F8FAFC; -fx-font-size:15; -fx-font-weight:bold;");

        Label status = new Label("Status: " + m.getStatus().name() +
            (m.getDueDate() != null ? "  •  Due: " + m.getDueDate().toString() : ""));
        status.setStyle("-fx-text-fill:#94A3B8; -fx-font-size:13;");

        Label desc = new Label(m.getDescription() != null ? m.getDescription() : "");
        desc.setStyle("-fx-text-fill:#CBD5E1; -fx-font-size:13;");
        desc.setWrapText(true);

        HBox actions = new HBox(8);
        Button markDone = new Button("✅ Mark Complete");
        markDone.setStyle("-fx-background-color:#059669; -fx-text-fill:white; -fx-background-radius:6; -fx-cursor:hand; -fx-padding:6 14;");
        markDone.setOnAction(e -> markComplete(m));

        if ("COMPLETED".equals(m.getStatus().name())) markDone.setDisable(true);
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
        t.setOnSucceeded(e -> { if (t.getValue()) loadMilestones(); });
        new Thread(t).start();
    }

    @FXML
    private void handleAddMilestone() {
        String title = mTitleField.getText().trim();
        if (title.isEmpty()) { showError("Title is required."); return; }
        LocalDate due = mDueDatePicker.getValue();
        String desc = mDescField.getText().trim();

        Task<Milestone> t = new Task<>() {
            @Override protected Milestone call() throws Exception {
                return milestoneService.createMilestone(title, desc, due, SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> Platform.runLater(() -> {
            mTitleField.clear(); mDescField.clear(); mDueDatePicker.setValue(null);
            loadMilestones();
        }));
        t.setOnFailed(e -> showError("Failed to add milestone."));
        new Thread(t).start();
    }

    private void showError(String msg) {
        errorLabel.setText(msg); errorLabel.setVisible(true); errorLabel.setManaged(true);
    }
}
