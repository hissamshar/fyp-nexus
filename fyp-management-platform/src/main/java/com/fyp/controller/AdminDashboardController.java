package com.fyp.controller;

import com.fyp.Main;
import com.fyp.model.AuditLogEntry;
import com.fyp.service.AdminService;
import com.fyp.service.ProposalService;
import com.fyp.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private Label userNameLabel, totalUsersLabel, activeProjectsLabel, pendingLabel, completedLabel;
    @FXML private StackPane contentPane;
    @FXML private TableView<AuditLogEntry> auditTable;
    @FXML private TableColumn<AuditLogEntry,String> auditActionCol, auditTimeCol, auditIpCol, auditDetailCol;

    private final AdminService adminService     = new AdminService();
    private final ProposalService proposalSvc   = new ProposalService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (SessionManager.getCurrentUser() != null)
            userNameLabel.setText(SessionManager.getCurrentUser().getName());

        auditActionCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAction()));
        auditTimeCol.setCellValueFactory(c  -> new SimpleStringProperty(
            c.getValue().getTimestamp() != null ? c.getValue().getTimestamp().toString().replace("T"," ").substring(0,19) : ""));
        auditIpCol.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getIpAddress()));
        auditDetailCol.setCellValueFactory(c-> new SimpleStringProperty(c.getValue().getDetails()));

        loadData();
    }

    private void loadData() {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                int users         = adminService.getTotalUsers();
                Map<String,Integer> counts = adminService.getProjectStatusCounts();
                int active    = counts.getOrDefault("IN_PROGRESS",0) + counts.getOrDefault("INITIATED",0);
                int pending   = proposalSvc.getPendingProposals().size();
                int completed = counts.getOrDefault("COMPLETED",0);
                List<AuditLogEntry> logs = adminService.getAuditLogs(50);
                Platform.runLater(() -> {
                    totalUsersLabel.setText(String.valueOf(users));
                    activeProjectsLabel.setText(String.valueOf(active));
                    pendingLabel.setText(String.valueOf(pending));
                    completedLabel.setText(String.valueOf(completed));
                    auditTable.getItems().setAll(logs);
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    @FXML void showDashboard()  { loadData(); }
    @FXML void showUsers()      { loadSubView("UserManagementView"); }
    @FXML void showDeadlines()  { loadSubView("DeadlineManagementView"); }
    @FXML void showAnalytics()  { loadSubView("AnalyticsDashboardView"); }
    @FXML void showReports()    { loadSubView("ReportGenerationView"); }
    @FXML void showAuditLogs()  { loadData(); }
    @FXML void showIndustry()   { loadSubView("IndustryProblemsView"); }
    @FXML void showRepository() { loadSubView("RepositoryView"); }
    @FXML void showProfile()    { loadSubView("ProfileView"); }
    @FXML void handleLogout()   { SessionManager.clearSession(); Main.loadView("LoginView"); }

    private void loadSubView(String name) {
        try {
            Parent v = FXMLLoader.load(getClass().getResource("/fxml/" + name + ".fxml"));
            contentPane.getChildren().setAll(v);
        } catch (Exception e) {
            Label lbl = new Label(name + " — View not yet available");
            lbl.setStyle("-fx-text-fill:#9090C0;-fx-font-size:16;");
            contentPane.getChildren().setAll(lbl);
        }
    }
}
