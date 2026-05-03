package com.fyp.controller;

import com.fyp.Main;
import com.fyp.model.User;
import com.fyp.service.AuthService;
import com.fyp.util.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Optional;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginBtn;
    @FXML private ProgressIndicator loadingIndicator;

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        clearError();
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all required fields.");
            highlightEmpty();
            return;
        }

        setLoading(true);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return AuthService.login(email, password);
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            if (!task.getValue()) {
                showError("Invalid email or password.");
            } else {
                navigateToDashboard(SessionManager.getCurrentRole());
            }
        });

        task.setOnFailed(e -> {
            setLoading(false);
            showError("Connection error. Please check your network and try again.");
        });

        new Thread(task).start();
    }

    private void navigateToDashboard(String role) {
        String view = switch (role) {
            case "STUDENT"          -> "StudentDashboard";
            case "SUPERVISOR"       -> "SupervisorDashboard";
            case "EXAMINER"         -> "ExaminerDashboard";
            case "ADMIN"            -> "AdminDashboard";
            case "INDUSTRY_PARTNER" -> "IndustryPartnerDashboard";
            default -> "LoginView";
        };
        Platform.runLater(() -> Main.loadView(view));
    }

    @FXML
    private void handleForgotPassword() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Password Reset");
        alert.setHeaderText("Password Reset");
        alert.setContentText("Please contact your administrator or use the OTP verification flow to reset your password.");
        alert.showAndWait();
    }

    @FXML
    private void handleRegister() {
        Main.loadView("RegisterView");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        emailField.getStyleClass().remove("field-error");
        passwordField.getStyleClass().remove("field-error");
    }

    private void highlightEmpty() {
        if (emailField.getText().isBlank())
            emailField.getStyleClass().add("field-error");
        if (passwordField.getText().isBlank())
            passwordField.getStyleClass().add("field-error");
    }

    private void setLoading(boolean loading) {
        loginBtn.setDisable(loading);
        loadingIndicator.setVisible(loading);
    }
}
