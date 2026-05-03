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

public class StudentDashboardController implements Initializable {

    @FXML private Label userNameLabel, welcomeLabel;
    @FXML private Label proposalCountLabel, milestoneCountLabel, meetingCountLabel, notifCountLabel;
    @FXML private Label notifBadge;
    @FXML private StackPane contentPane;
    @FXML private VBox dashboardContent;
    @FXML private TableView<ProjectProposal> proposalsTable;
    @FXML private TableColumn<ProjectProposal, String> propTitleCol, propStatusCol, propDateCol;

    // Sidebar buttons for active state tracking
    @FXML private Button navDashboard, navProposals, navMilestones, navDeliverables,
                         navProgress, navDiscussion, navMeetings, navGrades, navIndustry, navProfile;
    private Button activeNavBtn;

    private final ProposalService proposalService   = new ProposalService();
    private final MilestoneService milestoneService = new MilestoneService();
    private final MeetingService meetingService     = new MeetingService();
    private final NotificationService notifService  = new NotificationService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            userNameLabel.setText(user.getName());
            welcomeLabel.setText("Welcome back, " + user.getName().split(" ")[0] + "!");
        }

        propTitleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        propStatusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        propDateCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getSubmissionDate() != null ? c.getValue().getSubmissionDate().toString() : "—"));

        // Set initial active state on dashboard button
        activeNavBtn = navDashboard;
        loadDashboardData();
    }

    private void loadDashboardData() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<ProjectProposal> proposals = proposalService.getProposalsForCurrentUser();
                List<MeetingRequest> meetings   = meetingService.getMeetingsForCurrentUser();
                int unread = notifService.getUnreadCount(SessionManager.getCurrentUser().getUserId());

                Platform.runLater(() -> {
                    proposalCountLabel.setText(String.valueOf(proposals.size()));
                    meetingCountLabel.setText(String.valueOf(meetings.size()));
                    notifCountLabel.setText(String.valueOf(unread));
                    notifBadge.setText(unread > 0 ? String.valueOf(unread) : "");
                    notifBadge.setVisible(unread > 0);
                    proposalsTable.getItems().setAll(proposals);
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    // ── Navigation ─────────────────────────────────────────────────────────────

    @FXML void showDashboard()      { 
        setActive(navDashboard);    
        contentPane.getChildren().setAll(dashboardContent);
        loadDashboardData(); 
    }
    @FXML void showProposals()      { setActive(navProposals);    loadSubView("ProposalFormView"); }
    @FXML void showMilestones()     { setActive(navMilestones);   loadSubView("MilestoneView"); }
    @FXML void showDeliverables()   { setActive(navDeliverables); loadSubView("DeliverableUploadView"); }
    @FXML void showProgressReport() { setActive(navProgress);     loadSubView("ProgressReportView"); }
    @FXML void showDiscussion()     { setActive(navDiscussion);   loadSubView("DiscussionBoardView"); }
    @FXML void showMeetings()       { setActive(navMeetings);     loadSubView("MeetingSchedulerView"); }
    @FXML void showGrades()         { setActive(navGrades);       loadSubView("GradingView"); }
    @FXML void showIndustry()       { setActive(navIndustry);     loadSubView("IndustryProblemsView"); }
    @FXML void showProfile()        { setActive(navProfile);      loadSubView("ProfileView"); }
    @FXML void openNotifications()  { loadSubView("NotificationPanelView"); }

    @FXML void handleLogout() {
        SessionManager.clearSession();
        Main.loadView("LoginView");
    }

    /** Toggle sidebar active highlight to the given button. */
    private void setActive(Button btn) {
        if (activeNavBtn != null) {
            activeNavBtn.getStyleClass().remove("sidebar-btn-active");
        }
        if (btn != null) {
            btn.getStyleClass().add("sidebar-btn-active");
        }
        activeNavBtn = btn;
    }

    private void loadSubView(String viewName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/" + viewName + ".fxml"));
            Parent view = loader.load();
            contentPane.getChildren().setAll(view);
        } catch (Exception e) {
            showPlaceholder(viewName + " — coming soon");
            e.printStackTrace();
        }
    }

    private void showPlaceholder(String msg) {
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill: #9090C0; -fx-font-size: 18;");
        contentPane.getChildren().setAll(lbl);
    }
}
