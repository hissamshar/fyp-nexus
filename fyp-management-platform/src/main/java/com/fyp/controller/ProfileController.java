package com.fyp.controller;

import com.fyp.model.User;
import com.fyp.service.AuthService;
import com.fyp.util.SessionManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private Label avatarLabel, profileName, profileRole, profileEmail, profileMember;
    @FXML private TextField nameField, emailField;
    @FXML private PasswordField currentPassField, newPassField, confirmPassField;
    @FXML private Label errorLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadProfile();
    }

    private void loadProfile() {
        User u = SessionManager.getCurrentUser();
        if (u == null) return;

        avatarLabel.setText(getAvatar(u.getRole()));
        profileName.setText(u.getName());
        profileRole.setText(u.getRole());
        profileEmail.setText(u.getEmail());
        profileMember.setText("Member since account creation");

        nameField.setText(u.getName());
        emailField.setText(u.getEmail());
    }

    private String getAvatar(String role) {
        if (role == null) return "👤";
        return switch (role) {
            case "ADMIN"            -> "🛡️";
            case "SUPERVISOR"       -> "👨‍🏫";
            case "EXAMINER"         -> "👨‍💼";
            case "INDUSTRY_PARTNER" -> "🏭";
            default                 -> "👨‍🎓";
        };
    }

    @FXML
    private void handleUpdateProfile() {
        hideError();
        String name = nameField.getText().trim();
        if (name.isEmpty()) { showError("Name cannot be empty."); return; }

        // In a real app, call a UserService to update the name via API
        User u = SessionManager.getCurrentUser();
        if (u != null) {
            profileName.setText(name);
            showInfo("Profile updated successfully!");
        }
    }

    @FXML
    private void handleChangePassword() {
        hideError();
        String current = currentPassField.getText();
        String newPass  = newPassField.getText();
        String confirm  = confirmPassField.getText();

        if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            showError("All password fields are required."); return;
        }
        if (!newPass.equals(confirm)) {
            showError("New passwords do not match."); return;
        }
        if (newPass.length() < 8) {
            showError("Password must be at least 8 characters."); return;
        }

        // Placeholder — wire to AuthService.changePassword() when implemented
        showInfo("Password changed successfully! Please log in again.");
        currentPassField.clear(); newPassField.clear(); confirmPassField.clear();
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
    private void hideError() { errorLabel.setVisible(false); errorLabel.setManaged(false); }
}
