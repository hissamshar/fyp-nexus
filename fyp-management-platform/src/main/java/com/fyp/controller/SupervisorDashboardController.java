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
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class SupervisorDashboardController implements Initializable {

    @FXML private Label userNameLabel, welcomeLabel, pendingLabel, projectsLabel, slotsLabel, meetLabel, notifBadge;
    @FXML private StackPane contentPane;
    @FXML private VBox dashboardContent;
    @FXML private TableView<ProjectProposal> proposalsTable;
    @FXML private TableColumn<ProjectProposal, String> propTitleCol, propStudentCol, propDateCol, propActionCol;

    // Sidebar buttons for active state tracking
    @FXML private Button supNavDash, supNavProposals, supNavProjects, supNavMilestones,
                         supNavFeedback, supNavMeetings, supNavDiscussion, supNavGrading, supNavProfile;
    private Button activeNavBtn;

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
        activeNavBtn = supNavDash;
        setupTable();
        loadData();
    }

    private void setupTable() {
        propTitleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        propStudentCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStudentName()));
        propDateCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getSubmissionDate() != null ? c.getValue().getSubmissionDate().toString() : "—"));
        propActionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Review");
            {
                btn.getStyleClass().add("btn-primary");
                btn.setStyle("-fx-font-size:12; -fx-padding:5 12;");
                btn.setOnAction(e -> {
                    ProjectProposal selected = getTableView().getItems().get(getIndex());
                    if (selected != null) {
                        // Bug 3 fix: pass the proposal ID via SessionManager context
                        SessionManager.setContext("selectedProposalId",
                            selected.getProposalId().toString());
                    }
                    setActive(supNavProposals);
                    loadSubView("ProposalReviewView");
                });
            }
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

    // ── Navigation ─────────────────────────────────────────────────────────────

    @FXML void showDashboard()    { 
        setActive(supNavDash);       
        contentPane.getChildren().setAll(dashboardContent);
        loadData(); 
    }
    @FXML void showProposals()    { setActive(supNavProposals);  loadSubView("ProposalReviewView"); }
    @FXML void showProjects()     { setActive(supNavProjects);   loadSubView("MilestoneView"); }
    @FXML void showMilestones()   { setActive(supNavMilestones); loadSubView("MilestoneView"); }
    @FXML void showFeedback()     { setActive(supNavFeedback);   loadSubView("ProgressReportView"); }
    @FXML void showMeetings()     { setActive(supNavMeetings);   loadSubView("MeetingSchedulerView"); }
    @FXML void showDiscussion()   { setActive(supNavDiscussion); loadSubView("DiscussionBoardView"); }
    @FXML void showGrading()      { setActive(supNavGrading);   loadSubView("GradingView"); }
    @FXML void showProfile()      { setActive(supNavProfile);   loadSubView("ProfileView"); }
    @FXML void openNotifications() { loadSubView("NotificationPanelView"); }
    @FXML void handleLogout()     { SessionManager.clearSession(); Main.loadView("LoginView"); }

    private void setActive(Button btn) {
        if (activeNavBtn != null) activeNavBtn.getStyleClass().remove("sidebar-btn-active");
        if (btn != null) btn.getStyleClass().add("sidebar-btn-active");
        activeNavBtn = btn;
    }

    private void loadSubView(String name) {
        try {
            Parent v = FXMLLoader.load(getClass().getResource("/fxml/" + name + ".fxml"));
            contentPane.getChildren().setAll(v);
        } catch (Exception e) {
            Label lbl = new Label(name + " — Coming Soon");
            lbl.setStyle("-fx-text-fill:#9090C0; -fx-font-size:18;");
            contentPane.getChildren().setAll(lbl);
        }
    }
}
