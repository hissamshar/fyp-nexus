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

    private final AuthService authService = new AuthService();

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

        Task<Optional<User>> task = new Task<>() {
            @Override
            protected Optional<User> call() throws Exception {
                return authService.login(email, password);
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            Optional<User> userOpt = task.getValue();
            if (userOpt.isEmpty()) {
                showError("Invalid email or password.");
            } else {
                User user = userOpt.get();
                SessionManager.setCurrentUser(user);
                navigateToDashboard(user.getRole());
            }
        });

        task.setOnFailed(e -> {
            setLoading(false);
            String msg = task.getException().getMessage();
            if (msg != null && msg.contains("locked")) {
                showError(msg);
            } else if (msg != null && msg.contains("Email not verified")) {
                showError(msg);
            } else {
                showError("Connection error. Please check your network and try again.");
            }
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
        Main.loadView("ForgotPasswordView");
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
