package com.fyp.controller;

import com.fyp.model.Project;
import com.fyp.service.AdminService;
import com.fyp.service.ProposalService;
import com.fyp.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.*;

public class AnalyticsDashboardController implements Initializable {

    @FXML private Label totalStudents, activeProjects, pendingProposals, avgGrade;
    @FXML private TableView<Map.Entry<String, Long>> projectStatusTable;
    @FXML private TableColumn<Map.Entry<String, Long>, String> psStatusCol, psCountCol;
    @FXML private TableView<String[]> supervisorLoadTable;
    @FXML private TableColumn<String[], String> slNameCol, slSlotsCol, slStudCol;

    private final AdminService    adminService    = new AdminService();
    private final ProposalService proposalService = new ProposalService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        psStatusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getKey()));
        psCountCol.setCellValueFactory(c  -> new SimpleStringProperty(String.valueOf(c.getValue().getValue())));
        slNameCol.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue()[0]));
        slSlotsCol.setCellValueFactory(c  -> new SimpleStringProperty(c.getValue()[1]));
        slStudCol.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue()[2]));

        loadAnalytics();
    }

    private void loadAnalytics() {
        Task<Map<String, Object>> t = new Task<>() {
            @Override protected Map<String, Object> call() throws Exception {
                return adminService.getAnalyticsSummary(SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> {
            Map<String, Object> data = t.getValue();
            totalStudents.setText(String.valueOf(data.getOrDefault("totalStudents", 0)));
            activeProjects.setText(String.valueOf(data.getOrDefault("activeProjects", 0)));
            pendingProposals.setText(String.valueOf(data.getOrDefault("pendingProposals", 0)));
            avgGrade.setText(String.valueOf(data.getOrDefault("avgGrade", "—")));

            @SuppressWarnings("unchecked")
            Map<String, Long> statusMap = (Map<String, Long>) data.getOrDefault("projectsByStatus", new HashMap<>());
            projectStatusTable.setItems(FXCollections.observableArrayList(statusMap.entrySet()));

            @SuppressWarnings("unchecked")
            List<String[]> supLoad = (List<String[]>) data.getOrDefault("supervisorLoad", new ArrayList<>());
            supervisorLoadTable.setItems(FXCollections.observableArrayList(supLoad));
        });
        new Thread(t).start();
    }

    @FXML
    private void handleExportCsv() {
        Task<String> t = new Task<>() {
            @Override protected String call() throws Exception {
                return adminService.exportToCsv(SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export Complete");
            alert.setHeaderText("CSV exported successfully");
            alert.setContentText("File saved to: " + t.getValue());
            alert.showAndWait();
        });
        new Thread(t).start();
    }

    @FXML
    private void handleGeneratePdf() {
        Task<String> t = new Task<>() {
            @Override protected String call() throws Exception {
                return adminService.generatePdfReport(SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("PDF Generated");
            alert.setHeaderText("PDF report generated");
            alert.setContentText("Saved to: " + t.getValue());
            alert.showAndWait();
        });
        new Thread(t).start();
    }
}
