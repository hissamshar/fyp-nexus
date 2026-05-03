package com.fyp.controller;

import com.fyp.Main;
import com.fyp.model.*;
import com.fyp.service.*;
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
import java.util.ResourceBundle;

public class SupervisorDashboardController implements Initializable {

    @FXML private Label userNameLabel, welcomeLabel, pendingLabel, projectsLabel, slotsLabel, meetLabel, notifBadge;
    @FXML private StackPane contentPane;
    @FXML private TableView<ProjectProposal> proposalsTable;
    @FXML private TableColumn<ProjectProposal, String> propTitleCol, propStudentCol, propDateCol, propActionCol;

    private final ProposalService proposalService = new ProposalService();
    private final ProjectService projectService   = new ProjectService();
    private final MeetingService meetingService   = new MeetingService();
    private final NotificationService notifSvc    = new NotificationService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            userNameLabel.setText(user.getName());
            welcomeLabel.setText("Welcome, " + user.getName().split(" ")[0] + "!");
        }
        setupTable();
        loadData();
    }

    private void setupTable() {
        propTitleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        propStudentCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStudentId().toString().substring(0, 8) + "…"));
        propDateCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getSubmissionDate() != null ? c.getValue().getSubmissionDate().toString() : "—"));
        propActionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Review");
            { btn.setStyle("-fx-background-color:#6C63FF;-fx-text-fill:white;-fx-cursor:hand;");
              btn.setOnAction(e -> { loadSubView("ProposalReviewView"); }); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void loadData() {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                List<ProjectProposal> pending = proposalService.getPendingProposals();
                List<Project> projects        = projectService.getProjectsForCurrentUser();
                List<MeetingRequest> meetings = meetingService.getMeetingsForCurrentUser();
                int unread = notifSvc.getUnreadCount(SessionManager.getCurrentUser().getUserId());
                User u = SessionManager.getCurrentUser();
                int slots = 0;
                if (u instanceof Supervisor sup) {
                    slots = sup.getSlotsAvailable();
                } else {
                    var sv = new com.fyp.dao.SupervisorDAO().findByUserId(u.getUserId(), SessionManager.getJwtToken());
                    if (sv.isPresent()) slots = sv.get().getSlotsAvailable();
                }
                final int finalSlots = slots;
                Platform.runLater(() -> {
                    pendingLabel.setText(String.valueOf(pending.size()));
                    projectsLabel.setText(String.valueOf(projects.size()));
                    slotsLabel.setText(String.valueOf(finalSlots));
                    meetLabel.setText(String.valueOf(meetings.size()));
                    notifBadge.setText(unread > 0 ? String.valueOf(unread) : "");
                    notifBadge.setVisible(unread > 0);
                    proposalsTable.getItems().setAll(pending);
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    @FXML void showDashboard()   { loadData(); }
    @FXML void showProposals()   { loadSubView("ProposalReviewView"); }
    @FXML void showProjects()    { loadSubView("MilestoneView"); }
    @FXML void showMilestones()  { loadSubView("MilestoneView"); }
    @FXML void showFeedback()    { loadSubView("FeedbackView"); }
    @FXML void showMeetings()    { loadSubView("MeetingSchedulerView"); }
    @FXML void showDiscussion()  { loadSubView("DiscussionBoardView"); }
    @FXML void showGrading()     { loadSubView("GradingView"); }
    @FXML void showProfile()     { loadSubView("ProfileView"); }
    @FXML void openNotifications(){ loadSubView("NotificationPanelView"); }
    @FXML void handleLogout()    { SessionManager.clearSession(); Main.loadView("LoginView"); }

    private void loadSubView(String name) {
        try {
            Parent v = FXMLLoader.load(getClass().getResource("/fxml/" + name + ".fxml"));
            contentPane.getChildren().setAll(v);
        } catch (Exception e) { contentPane.getChildren().setAll(new Label(name + " — Loading…")); }
    }
}
