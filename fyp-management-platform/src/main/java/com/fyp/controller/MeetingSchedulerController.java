package com.fyp.controller;

import com.fyp.model.*;
import com.fyp.service.MeetingService;
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

public class MeetingSchedulerController implements Initializable {

    @FXML private ComboBox<Supervisor> supervisorCombo;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;
    @FXML private TextArea agendaField;
    @FXML private Label errorLabel;
    @FXML private TableView<MeetingRequest> meetingsTable;
    @FXML private TableColumn<MeetingRequest, String> mDateCol, mPersonCol, mAgendaCol, mStatusCol;

    private final MeetingService meetingService = new MeetingService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        mDateCol.setCellValueFactory(c   -> new SimpleStringProperty(
            c.getValue().getProposedTime() != null ? c.getValue().getProposedTime().toString().substring(0,16) : "—"));
        mPersonCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getSupervisorId() != null ? c.getValue().getSupervisorId().toString().substring(0, 8) + "…" : "—"));
        mAgendaCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getAgenda() != null ? c.getValue().getAgenda() : ""));
        mStatusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));

        supervisorCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Supervisor s, boolean empty) {
                super.updateItem(s, empty); setText(empty || s == null ? null : s.getName());
            }
        });
        supervisorCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Supervisor s, boolean empty) {
                super.updateItem(s, empty); setText(empty || s == null ? "Select supervisor" : s.getName());
            }
        });

        loadSupervisors();
        loadMeetings();
    }

    private void loadSupervisors() {
        Task<List<Supervisor>> t = new Task<>() {
            @Override protected List<Supervisor> call() throws Exception {
                return meetingService.getAvailableSupervisors(SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> supervisorCombo.setItems(FXCollections.observableArrayList(t.getValue())));
        new Thread(t).start();
    }

    private void loadMeetings() {
        Task<List<MeetingRequest>> t = new Task<>() {
            @Override protected List<MeetingRequest> call() throws Exception {
                return meetingService.getMeetingsForCurrentUser();
            }
        };
        t.setOnSucceeded(e -> meetingsTable.setItems(FXCollections.observableArrayList(t.getValue())));
        new Thread(t).start();
    }

    @FXML
    private void handleRequest() {
        hideError();
        if (supervisorCombo.getValue() == null) { showError("Select a supervisor."); return; }
        if (datePicker.getValue() == null)       { showError("Select a date."); return; }

        Supervisor sup = supervisorCombo.getValue();
        String dateTime = datePicker.getValue().toString() + "T" +
            (timeField.getText().trim().isEmpty() ? "09:00" : timeField.getText().trim()) + ":00";
        String agenda   = agendaField.getText().trim();

        Task<MeetingRequest> t = new Task<>() {
            @Override protected MeetingRequest call() throws Exception {
                return meetingService.requestMeeting(sup.getUserId(), dateTime, agenda, SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> {
            if (t.getValue() != null) {
                showInfo("Meeting request sent!");
                agendaField.clear(); datePicker.setValue(null); timeField.clear();
                loadMeetings();
            } else {
                showError("Failed to send request.");
            }
        });
        new Thread(t).start();
    }

    @FXML private void handleAccept()      { updateStatus("CONFIRMED"); }
    @FXML private void handleDecline()     { updateStatus("CANCELLED"); }
    @FXML private void handleReschedule()  { updateStatus("RESCHEDULED"); }

    private void updateStatus(String status) {
        MeetingRequest selected = meetingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Select a meeting first."); return; }
        Task<Boolean> t = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return meetingService.updateStatus(selected.getMeetingId(), status, SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> { if (t.getValue()) loadMeetings(); });
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
