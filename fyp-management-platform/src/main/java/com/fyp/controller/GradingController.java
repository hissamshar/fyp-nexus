package com.fyp.controller;

import com.fyp.model.*;
import com.fyp.service.GradeService;
import com.fyp.service.ProjectService;
import com.fyp.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class GradingController implements Initializable {

    @FXML private ComboBox<Project> projectCombo;
    @FXML private TableView<RubricCriterion> rubricTable;
    @FXML private TableColumn<RubricCriterion, String> criterionCol, maxScoreCol, scoreCol, commentsCol;
    @FXML private Label totalScoreLabel, gradeLabel, errorLabel;
    @FXML private TextArea overallFeedback;

    private final GradeService   gradeService   = new GradeService();
    private final ProjectService projectService = new ProjectService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        criterionCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        maxScoreCol.setCellValueFactory(c  -> new SimpleStringProperty(String.valueOf(c.getValue().getMaxScore())));
        scoreCol.setCellValueFactory(c     -> new SimpleStringProperty(String.valueOf(c.getValue().getMaxScore())));
        commentsCol.setCellValueFactory(c  -> new SimpleStringProperty(""));

        rubricTable.setEditable(true);
        scoreCol.setCellFactory(TextFieldTableCell.forTableColumn());
        scoreCol.setOnEditCommit(e -> {
            e.getRowValue().setMaxScore(parseScore(e.getNewValue(), e.getRowValue().getMaxScore()));
            recalcTotal();
        });

        projectCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Project p, boolean empty) {
                super.updateItem(p, empty); setText(empty || p == null ? null : p.getTitle());
            }
        });
        projectCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Project p, boolean empty) {
                super.updateItem(p, empty); setText(empty || p == null ? "Choose project to grade" : p.getTitle());
            }
        });

        loadProjects();
    }

    private void loadProjects() {
        Task<List<Project>> t = new Task<>() {
            @Override protected List<Project> call() throws Exception {
                return gradeService.getProjectsForGrader(SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> projectCombo.setItems(FXCollections.observableArrayList(t.getValue())));
        new Thread(t).start();
    }

    @FXML
    private void handleLoadRubric() {
        Project p = projectCombo.getValue();
        if (p == null) { showError("Select a project first."); return; }
        Task<Rubric> t = new Task<>() {
            @Override protected Rubric call() throws Exception {
                return gradeService.getRubricForProject(p.getProjectId(), SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> {
            Rubric r = t.getValue();
            if (r != null && r.getCriteria() != null) {
                rubricTable.setItems(FXCollections.observableArrayList(r.getCriteria()));
                recalcTotal();
            } else {
                showError("No rubric found for this project.");
            }
        });
        new Thread(t).start();
    }

    private void recalcTotal() {
        int max   = rubricTable.getItems().stream().mapToInt(RubricCriterion::getMaxScore).sum();
        int given = max; // simplified: same as max until edited
        totalScoreLabel.setText(given + " / " + max);
        double pct = max > 0 ? (given * 100.0 / max) : 0;
        gradeLabel.setText(pct >= 90 ? "A+" : pct >= 85 ? "A" : pct >= 80 ? "B+" :
                           pct >= 75 ? "B"  : pct >= 70 ? "C+" : pct >= 65 ? "C" :
                           pct >= 60 ? "D"  : "F");
    }

    private int parseScore(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return fallback; }
    }

    @FXML private void handleSaveDraft() { saveGrades(false); }
    @FXML private void handlePublish()   { saveGrades(true); }

    private void saveGrades(boolean publish) {
        Project p = projectCombo.getValue();
        if (p == null) { showError("Select a project."); return; }
        Task<Boolean> t = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return gradeService.saveGrades(p.getProjectId(), rubricTable.getItems(),
                    overallFeedback.getText().trim(), publish, SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> {
            if (t.getValue()) showInfo(publish ? "Grades published!" : "Draft saved!");
            else showError("Failed to save grades.");
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
}
