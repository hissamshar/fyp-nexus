package com.fyp.controller;

import com.fyp.Main;
import com.fyp.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

public class IndustryPartnerDashboardController implements Initializable {

    @FXML private Label userNameLabel;
    @FXML private StackPane contentPane;
    @FXML private javafx.scene.layout.VBox dashboardContent;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (SessionManager.getCurrentUser() != null) {
            userNameLabel.setText(SessionManager.getCurrentUser().getName());
        }
    }

    @FXML void showDashboard() {
        contentPane.getChildren().setAll(dashboardContent);
    }
    @FXML void showPostProblem() {}
    @FXML void handleLogout() {
        SessionManager.clearSession();
        Main.loadView("LoginView");
    }
}
