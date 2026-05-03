package com.fyp.controller;

import com.fyp.Main;
import com.fyp.service.AuthService;
import com.fyp.util.SessionManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.UUID;

public class RegisterController implements Initializable {

    @FXML private TextField nameField, emailField, departmentField, companyField;
    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private CheckBox privacyCheck;
    @FXML private Label errorLabel;
    @FXML private Button registerBtn;
    @FXML private ProgressIndicator loadingIndicator;



    @Override
    public void initialize(URL url, ResourceBundle rb) {
        roleCombo.getItems().addAll(
            "Student", "Supervisor", "Examiner", "Industry Partner");
        roleCombo.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleRegister() {
        clearError();

        String name    = nameField.getText().trim();
        String email   = emailField.getText().trim();
        String pass    = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        String roleRaw = roleCombo.getValue();

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || roleRaw == null) {
            showError("Please fill in all required fields.");
            return;
        }
        if (!pass.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }
        if (!privacyCheck.isSelected()) {
            showError("You must accept the Privacy Policy.");
            return;
        }

        String role = switch (roleRaw) {
            case "Student"           -> "STUDENT";
            case "Supervisor"        -> "SUPERVISOR";
            case "Examiner"          -> "EXAMINER";
            case "Industry Partner"  -> "INDUSTRY_PARTNER";
            default                  -> "STUDENT";
        };

        String dept    = departmentField.getText().trim();
        String company = companyField.getText().trim();

        setLoading(true);
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                // We currently don't use dept and company in the basic signup for demo purposes, 
                // but they can be passed to meta-data if needed.
                return AuthService.signup(email, pass, name, role);
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            String userIdStr = task.getValue();
            if ("AUTO_LOGGED_IN".equals(userIdStr)) {
                // Email confirmation is disabled on Supabase; user is immediately logged in
                System.out.println("Auto logged in!");
                String dashboardView = switch (SessionManager.getCurrentRole()) {
                    case "STUDENT"          -> "StudentDashboard";
                    case "SUPERVISOR"       -> "SupervisorDashboard";
                    case "EXAMINER"         -> "ExaminerDashboard";
                    case "ADMIN"            -> "AdminDashboard";
                    case "INDUSTRY_PARTNER" -> "IndustryPartnerDashboard";
                    default -> "LoginView";
                };
                javafx.application.Platform.runLater(() -> Main.loadView(dashboardView));
            } else if (userIdStr != null) {
                try {
                    UUID userId = "OK".equals(userIdStr) ? null : UUID.fromString(userIdStr);
                    Main.loadViewWithContext("OTPVerificationView", controller -> {
                        if (controller instanceof OTPVerificationController otpCtrl) {
                            otpCtrl.setContext(userId, email, "EMAIL_VERIFY");
                        }
                    });
                } catch (Exception ex) {
                    showError("Registration successful! Please check your email for the code.");
                    Main.loadView("LoginView");
                }
            } else {
                showError("Registration failed. Please try again.");
            }
        });

        task.setOnFailed(e -> {
            setLoading(false);
            String msg = task.getException().getMessage();
            if ("DUPLICATE_EMAIL".equals(msg)) {
                showError("This email is already registered. Please log in.");
            } else {
                showError(msg != null ? msg : "Registration failed. Please try again.");
            }
        });

        new Thread(task).start();
    }

    @FXML
    private void handleLogin() {
        Main.loadView("LoginView");
    }

    private void showError(String msg) { errorLabel.setText(msg); errorLabel.setVisible(true); }
    private void clearError()          { errorLabel.setText(""); errorLabel.setVisible(false); }
    private void setLoading(boolean b) { registerBtn.setDisable(b); loadingIndicator.setVisible(b); }
}
