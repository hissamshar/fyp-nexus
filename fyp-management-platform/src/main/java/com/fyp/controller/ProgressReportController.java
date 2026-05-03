package com.fyp.controller;

import com.fyp.model.ProgressReport;
import com.fyp.service.ProjectService;
import com.fyp.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class ProgressReportController implements Initializable {

    @FXML private TextField weekField;
    @FXML private DatePicker reportDate;
    @FXML private TextArea completedField, plannedField, blockersField;
    @FXML private Label errorLabel;
    @FXML private TableView<ProgressReport> reportsTable;
    @FXML private TableColumn<ProgressReport, String> rWeekCol, rDateCol, rSummaryCol;

    private final ProjectService projectService = new ProjectService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        rWeekCol.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getReportWeek()));
        rDateCol.setCellValueFactory(c    -> new SimpleStringProperty(
            c.getValue().getReportDate() != null ? c.getValue().getReportDate().toString() : "—"));
        rSummaryCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getWorkCompleted() != null ?
            c.getValue().getWorkCompleted().substring(0, Math.min(60, c.getValue().getWorkCompleted().length())) + "…"
            : ""));

        reportDate.setValue(LocalDate.now());
        loadReports();
    }

    private void loadReports() {
        Task<List<ProgressReport>> t = new Task<>() {
            @Override protected List<ProgressReport> call() throws Exception {
                return projectService.getProgressReports(SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> reportsTable.getItems().setAll(t.getValue()));
        new Thread(t).start();
    }

    @FXML
    private void handleSubmit() {
        hideError();
        if (weekField.getText().trim().isEmpty()) { showError("Week is required."); return; }
        if (completedField.getText().trim().isEmpty()) { showError("Work completed is required."); return; }

        String week      = weekField.getText().trim();
        LocalDate date   = reportDate.getValue() != null ? reportDate.getValue() : LocalDate.now();
        String completed = completedField.getText().trim();
        String planned   = plannedField.getText().trim();
        String blockers  = blockersField.getText().trim();

        Task<ProgressReport> t = new Task<>() {
            @Override protected ProgressReport call() throws Exception {
                return projectService.submitProgressReport(week, date, completed, planned, blockers,
                    SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> {
            if (t.getValue() != null) {
                showInfo("Report submitted!");
                weekField.clear(); completedField.clear(); plannedField.clear(); blockersField.clear();
                reportDate.setValue(LocalDate.now());
                loadReports();
            } else {
                showError("Failed to submit. Do you have an active project?");
            }
        });
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
