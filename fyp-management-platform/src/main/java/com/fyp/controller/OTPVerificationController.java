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


    private UUID userId;
    private String email;
    private String purpose; // "EMAIL_VERIFY" or "PASSWORD_RESET"

    public void setContext(UUID userId, String email, String purpose) {
        this.userId  = userId;
        this.email   = email;
        this.purpose = purpose;
        instructionLabel.setText("A verification code was sent to " + email + ".");
    }

    @FXML
    private void handleVerify() {
        String otp = otpField.getText().trim();
        if (otp.isEmpty()) {
            showError("Please enter the verification code.");
            return;
        }

        setLoading(true);
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                if ("EMAIL_VERIFY".equals(purpose)) {
                    return AuthService.verifyEmailOtp(email, otp);
                } else {
                    return OTPService.verify(userId, otp, purpose);
                }
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            if (task.getValue()) {
                if ("EMAIL_VERIFY".equals(purpose)) {
                    showInfo("Email verified! You are now logged in.");
                    Main.loadView("DashboardView"); // Or whatever the landing view is
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
        if (email == null) return;
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                if ("EMAIL_VERIFY".equals(purpose)) {
                    return AuthService.resendOtp(email);
                } else {
                    String otp = OTPService.generateAndStore(userId, purpose);
                    com.fyp.util.EmailService.sendOTP(email, otp);
                    return true;
                }
            }
        };
        task.setOnSucceeded(e -> {
            if (task.getValue()) showInfo("New code sent to " + email);
            else showError("Failed to resend code.");
        });
        task.setOnFailed(e -> showError("Failed to resend code."));
        new Thread(task).start();
    }

    @FXML
    private void handleBack() { Main.loadView("LoginView"); }

    private void showError(String msg) { errorLabel.setStyle("-fx-text-fill: #FF6B6B;"); errorLabel.setText(msg); }
    private void showInfo(String msg)  { errorLabel.setStyle("-fx-text-fill: #4ECDC4;"); errorLabel.setText(msg); }
    private void setLoading(boolean b) { verifyBtn.setDisable(b); loadingIndicator.setVisible(b); }
}
