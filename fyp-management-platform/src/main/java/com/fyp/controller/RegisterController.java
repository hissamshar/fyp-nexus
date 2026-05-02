package com.fyp.controller;

import com.fyp.Main;
import com.fyp.service.AuthService;
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

    private final AuthService authService = new AuthService();
    private UUID pendingUserId;

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
        Task<UUID> task = new Task<>() {
            @Override
            protected UUID call() throws Exception {
                return authService.register(name, email, pass, role, dept, company);
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            pendingUserId = task.getValue();
            OTPVerificationController ctrl = Main.loadViewWithController("OTPVerificationView");
            if (ctrl != null) ctrl.setContext(pendingUserId, email, "EMAIL_VERIFY");
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
