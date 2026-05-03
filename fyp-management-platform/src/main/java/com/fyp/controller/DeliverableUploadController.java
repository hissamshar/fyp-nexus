package com.fyp.controller;

import com.fyp.model.Deliverable;
import com.fyp.model.Milestone;
import com.fyp.service.DeliverableService;
import com.fyp.service.MilestoneService;
import com.fyp.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DeliverableUploadController implements Initializable {

    @FXML private ComboBox<Milestone> milestoneCombo;
    @FXML private TextField dTitleField;
    @FXML private TextArea dDescField;
    @FXML private Label selectedFileLabel, errorLabel;
    @FXML private TableView<Deliverable> deliverablesTable;
    @FXML private TableColumn<Deliverable, String> dTitleCol, dMilestoneCol, dStatusCol, dDateCol;

    private final DeliverableService deliverableService = new DeliverableService();
    private final MilestoneService milestoneService     = new MilestoneService();
    private File selectedFile;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dTitleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        dMilestoneCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getMilestoneId() != null ? c.getValue().getMilestoneId().toString().substring(0, 8) + "…" : "—"));
        dStatusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        dDateCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getUploadedAt() != null ? c.getValue().getUploadedAt().toString().substring(0, 10) : "—"));

        milestoneCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Milestone m, boolean empty) {
                super.updateItem(m, empty); setText(empty || m == null ? null : m.getTitle());
            }
        });
        milestoneCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Milestone m, boolean empty) {
                super.updateItem(m, empty); setText(empty || m == null ? "Select milestone" : m.getTitle());
            }
        });

        loadMilestones();
        loadDeliverables();
    }

    private void loadMilestones() {
        Task<List<Milestone>> t = new Task<>() {
            @Override protected List<Milestone> call() throws Exception {
                return milestoneService.getMilestonesForCurrentUser(SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> milestoneCombo.setItems(FXCollections.observableArrayList(t.getValue())));
        new Thread(t).start();
    }

    private void loadDeliverables() {
        Task<List<Deliverable>> t = new Task<>() {
            @Override protected List<Deliverable> call() throws Exception {
                return deliverableService.getDeliverablesForCurrentUser(SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> deliverablesTable.getItems().setAll(t.getValue()));
        new Thread(t).start();
    }

    @FXML
    private void handleChooseFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Deliverable File");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.zip"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        selectedFile = fc.showOpenDialog(dTitleField.getScene().getWindow());
        if (selectedFile != null) {
            selectedFileLabel.setText(selectedFile.getName());
        }
    }

    @FXML
    private void handleUpload() {
        hideError();
        if (dTitleField.getText().trim().isEmpty()) { showError("Title is required."); return; }
        if (milestoneCombo.getValue() == null)       { showError("Please select a milestone."); return; }
        if (selectedFile == null)                    { showError("Please choose a file to upload."); return; }

        String title    = dTitleField.getText().trim();
        String desc     = dDescField.getText().trim();
        Milestone ms    = milestoneCombo.getValue();

        Task<Deliverable> t = new Task<>() {
            @Override protected Deliverable call() throws Exception {
                return deliverableService.uploadDeliverable(title, desc, ms.getMilestoneId(),
                    selectedFile, SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> {
            if (t.getValue() != null) {
                showInfo("Uploaded successfully!");
                dTitleField.clear(); dDescField.clear(); selectedFile = null;
                selectedFileLabel.setText("No file selected");
                loadDeliverables();
            } else {
                showError("Upload failed. Check file type and size.");
            }
        });
        t.setOnFailed(e -> showError("Error: " + t.getException().getMessage()));
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
