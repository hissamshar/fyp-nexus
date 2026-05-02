package com.fyp.controller;

import com.fyp.Main;
import com.fyp.service.AuthService;
import com.fyp.util.OTPService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.UUID;

public class OTPVerificationController {

    @FXML private TextField otpField;
    @FXML private Label errorLabel, instructionLabel;
    @FXML private Button verifyBtn;
    @FXML private ProgressIndicator loadingIndicator;

    private final AuthService authService = new AuthService();
    private UUID userId;
    private String email;
    private String purpose; // "EMAIL_VERIFY" or "PASSWORD_RESET"

    public void setContext(UUID userId, String email, String purpose) {
        this.userId  = userId;
        this.email   = email;
        this.purpose = purpose;
        instructionLabel.setText("A 6-digit code was sent to " + email + ".");
    }

    @FXML
    private void handleVerify() {
        String otp = otpField.getText().trim();
        if (otp.length() != 6) {
            showError("Please enter the 6-digit code.");
            return;
        }

        setLoading(true);
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return "EMAIL_VERIFY".equals(purpose)
                    ? authService.verifyEmail(userId, otp)
                    : OTPService.verify(userId, otp, purpose);
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            if (task.getValue()) {
                if ("EMAIL_VERIFY".equals(purpose)) {
                    showInfo("Email verified! You can now log in.");
                    Main.loadView("LoginView");
                } else {
                    Main.loadView("LoginView");
                }
            } else {
                showError("Incorrect or expired code. Please try again.");
            }
        });

        task.setOnFailed(e -> {
            setLoading(false);
            showError("Verification failed. Please try again.");
        });

        new Thread(task).start();
    }

    @FXML
    private void handleResend() {
        if (userId == null) return;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String otp = OTPService.generateAndStore(userId, purpose);
                com.fyp.util.EmailService.sendOTP(email, otp);
                return null;
            }
        };
        task.setOnSucceeded(e -> showInfo("New code sent to " + email));
        task.setOnFailed(e -> showError("Failed to resend code."));
        new Thread(task).start();
    }

    @FXML
    private void handleBack() { Main.loadView("LoginView"); }

    private void showError(String msg) { errorLabel.setStyle("-fx-text-fill: #FF6B6B;"); errorLabel.setText(msg); }
    private void showInfo(String msg)  { errorLabel.setStyle("-fx-text-fill: #4ECDC4;"); errorLabel.setText(msg); }
    private void setLoading(boolean b) { verifyBtn.setDisable(b); loadingIndicator.setVisible(b); }
}
