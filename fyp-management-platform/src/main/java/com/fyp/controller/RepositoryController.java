package com.fyp.controller;

import com.fyp.model.Project;
import com.fyp.service.ProjectService;
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
import java.util.stream.Collectors;

public class RepositoryController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<Project> projectsTable;
    @FXML private TableColumn<Project, String> projTitleCol, projSupCol;
    @FXML private Label projTitle, projStatus, projSupervisor, projStudents, projGrade;
    @FXML private TextArea projProblem;

    private final ProjectService projectService = new ProjectService();
    private List<Project> allProjects;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        projTitleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        projSupCol.setCellValueFactory(c   -> new SimpleStringProperty(
            c.getValue().getSupervisorId() != null ? c.getValue().getSupervisorId().toString().substring(0, 8) + "…" : "—"));

        statusFilter.setItems(FXCollections.observableArrayList(
            "All", "APPROVED", "IN_PROGRESS", "SUBMITTED", "COMPLETED"));
        statusFilter.setValue("All");

        projectsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) showDetail(n);
        });

        loadProjects();
    }

    private void loadProjects() {
        Task<List<Project>> t = new Task<>() {
            @Override protected List<Project> call() throws Exception {
                return projectService.getPublicRepository(SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> {
            allProjects = t.getValue();
            projectsTable.setItems(FXCollections.observableArrayList(allProjects));
        });
        new Thread(t).start();
    }

    @FXML
    private void handleSearch() {
        if (allProjects == null) return;
        String query  = searchField.getText().trim().toLowerCase();
        String status = statusFilter.getValue();
        projectsTable.setItems(FXCollections.observableArrayList(
            allProjects.stream()
                .filter(p -> query.isEmpty() || (p.getTitle() != null && p.getTitle().toLowerCase().contains(query)))
                .filter(p -> "All".equals(status) || p.getStatus().name().equals(status))
                .collect(Collectors.toList())
        ));
    }

    private void showDetail(Project p) {
        projTitle.setText(p.getTitle() != null ? p.getTitle() : p.getProjectId().toString());
        projStatus.setText(p.getStatus().name());
        projSupervisor.setText(p.getSupervisorId() != null ? p.getSupervisorId().toString() : "—");
        projStudents.setText("—");
        projProblem.setText(p.getProblemStatement() != null ? p.getProblemStatement() : "—");
        projGrade.setText(p.getFinalGrade() != null ? p.getFinalGrade() : "Not graded yet");
    }
}
