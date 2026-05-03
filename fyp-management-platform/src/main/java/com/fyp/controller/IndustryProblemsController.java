package com.fyp.controller;

import com.fyp.model.IndustryProblem;
import com.fyp.service.ProjectService;
import com.fyp.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class IndustryProblemsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ListView<IndustryProblem> problemsList;
    @FXML private Label problemTitle, problemCompany, problemDomain, problemDeadline, statusMsg;
    @FXML private TextArea problemDesc;

    private final ProjectService projectService = new ProjectService();
    private List<IndustryProblem> allProblems;
    private IndustryProblem selectedProblem;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        problemsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(IndustryProblem p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setText(null); return; }
                setText(p.getTitle() + "\n" + (p.getCompanyName() != null ? p.getCompanyName() : ""));
                setStyle("-fx-text-fill:#F8FAFC; -fx-padding:10; -fx-font-size:13;");
            }
        });
        problemsList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) showDetail(n);
        });
        loadProblems();
    }

    private void loadProblems() {
        Task<List<IndustryProblem>> t = new Task<>() {
            @Override protected List<IndustryProblem> call() throws Exception {
                return projectService.getIndustryProblems(SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> {
            allProblems = t.getValue();
            problemsList.setItems(FXCollections.observableArrayList(allProblems));
        });
        new Thread(t).start();
    }

    @FXML
    private void handleSearch() {
        if (allProblems == null) return;
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            problemsList.setItems(FXCollections.observableArrayList(allProblems));
        } else {
            problemsList.setItems(FXCollections.observableArrayList(
                allProblems.stream()
                    .filter(p -> p.getTitle().toLowerCase().contains(query) ||
                                 (p.getCompanyName() != null && p.getCompanyName().toLowerCase().contains(query)) ||
                                 (p.getDomain() != null && p.getDomain().toLowerCase().contains(query)))
                    .collect(Collectors.toList())
            ));
        }
    }

    private void showDetail(IndustryProblem p) {
        selectedProblem = p;
        problemTitle.setText(p.getTitle());
        problemCompany.setText(p.getCompanyName() != null ? p.getCompanyName() : "—");
        problemDomain.setText(p.getDomain() != null ? p.getDomain() : "—");
        problemDesc.setText(p.getDescription() != null ? p.getDescription() : "");
        problemDeadline.setText(p.getDeadline() != null ? p.getDeadline().toString() : "No deadline");
        hideStatus();
    }

    @FXML
    private void handleAdopt() {
        if (selectedProblem == null) return;
        Task<Boolean> t = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return projectService.adoptIndustryProblem(selectedProblem.getProblemId(), SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> {
            if (t.getValue()) showStatus("✅ Problem adopted! It's linked to your project.");
            else showStatus("❌ Failed to adopt. You may need an active project first.");
        });
        new Thread(t).start();
    }

    private void showStatus(String msg) {
        statusMsg.setText(msg);
        boolean ok = msg.startsWith("✅");
        statusMsg.setStyle(ok
            ? "-fx-text-fill:#4ADE80;-fx-background-color:rgba(74,222,128,0.1);-fx-padding:8 12;-fx-background-radius:6;"
            : "-fx-text-fill:#F87171;-fx-background-color:rgba(248,113,113,0.1);-fx-padding:8 12;-fx-background-radius:6;");
        statusMsg.setVisible(true); statusMsg.setManaged(true);
    }
    private void hideStatus() { statusMsg.setVisible(false); statusMsg.setManaged(false); }
}
