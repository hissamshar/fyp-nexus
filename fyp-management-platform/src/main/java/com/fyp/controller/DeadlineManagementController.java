package com.fyp.controller;

import com.fyp.model.SemesterDeadline;
import com.fyp.service.AdminService;
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

public class DeadlineManagementController implements Initializable {

    @FXML private TextField deadlineNameField;
    @FXML private DatePicker deadlineDatePicker;
    @FXML private TextArea deadlineDescField;
    @FXML private Label errorLabel;
    @FXML private TableView<SemesterDeadline> deadlinesTable;
    @FXML private TableColumn<SemesterDeadline, String> dNameCol, dDateCol, dStatusCol, dDescCol;

    private final AdminService adminService = new AdminService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dNameCol.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().getName()));
        dDateCol.setCellValueFactory(c   -> new SimpleStringProperty(
            c.getValue().getDeadlineDate() != null ? c.getValue().getDeadlineDate().toString() : "—"));
        dStatusCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getDeadlineDate() != null && c.getValue().getDeadlineDate().isBefore(LocalDate.now())
            ? "PASSED" : "UPCOMING"));
        dDescCol.setCellValueFactory(c   -> new SimpleStringProperty(
            c.getValue().getDescription() != null ? c.getValue().getDescription() : ""));

        loadDeadlines();
    }

    private void loadDeadlines() {
        Task<List<SemesterDeadline>> t = new Task<>() {
            @Override protected List<SemesterDeadline> call() throws Exception {
                return adminService.getDeadlines(SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> deadlinesTable.getItems().setAll(t.getValue()));
        new Thread(t).start();
    }

    @FXML
    private void handleSave() {
        hideError();
        if (deadlineNameField.getText().trim().isEmpty()) { showError("Name is required."); return; }
        if (deadlineDatePicker.getValue() == null)        { showError("Date is required."); return; }

        String name  = deadlineNameField.getText().trim();
        LocalDate dt = deadlineDatePicker.getValue();
        String desc  = deadlineDescField.getText().trim();

        Task<SemesterDeadline> t = new Task<>() {
            @Override protected SemesterDeadline call() throws Exception {
                return adminService.upsertDeadline(name, dt, desc, SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> {
            if (t.getValue() != null) {
                showInfo("Deadline saved!");
                deadlineNameField.clear(); deadlineDatePicker.setValue(null); deadlineDescField.clear();
                loadDeadlines();
            } else {
                showError("Failed to save. Try again.");
            }
        });
        new Thread(t).start();
    }

    @FXML
    private void handleDelete() {
        SemesterDeadline selected = deadlinesTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Select a deadline to delete."); return; }
        Task<Boolean> t = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return adminService.deleteDeadline(selected.getDeadlineId(), SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> { if (t.getValue()) loadDeadlines(); });
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
