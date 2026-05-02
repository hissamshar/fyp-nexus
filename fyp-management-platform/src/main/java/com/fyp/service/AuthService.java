package com.fyp.service;

import com.fyp.dao.*;
import com.fyp.model.*;
import com.fyp.util.*;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles login, registration, OTP verification, and password reset.
 * All role-specific profile creation is delegated here.
 */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final SupervisorDAO supervisorDAO = new SupervisorDAO();
    private final ExaminerDAO examinerDAO = new ExaminerDAO();
    private final AdminDAO adminDAO = new AdminDAO();
    private final IndustryPartnerDAO partnerDAO = new IndustryPartnerDAO();

    private static final int MAX_LOGIN_ATTEMPTS = 5;

    /**
     * Authenticate a user. Returns the User object on success, empty on failure.
     * Throws an exception if the account is locked.
     */
    public Optional<User> login(String email, String password) throws Exception {
        Optional<User> opt = userDAO.findByEmail(email);
        if (opt.isEmpty()) return Optional.empty();

        User user = opt.get();

        if (!user.isActive()) throw new Exception("Account is deactivated. Contact the administrator.");
        if (!user.isEmailVerified()) throw new Exception("Email not verified. Please verify your email first.");

        int attempts = userDAO.getFailedAttempts(user.getUserId());
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            throw new Exception("Account locked after " + MAX_LOGIN_ATTEMPTS +
                    " failed attempts. Contact the administrator to unlock.");
        }

        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            userDAO.incrementFailedAttempts(user.getUserId());
            AuditLogger.log(user.getUserId(), "LOGIN_FAILED", "Invalid password attempt");
            return Optional.empty();
        }

        userDAO.resetFailedAttempts(user.getUserId());
        AuditLogger.logLogin(user.getUserId());
        return Optional.of(buildTypedUser(user));
    }

    /**
     * Register a new user. Sends OTP for email verification.
     * Returns the new userId on success.
     */
    public UUID register(String name, String email, String password,
                         String role, String department, String companyName) throws Exception {

        // Validate password strength
        String weakness = PasswordUtil.getWeaknessReason(password);
        if (weakness != null) throw new Exception(weakness);

        // Check duplicate email
        if (userDAO.emailExists(email)) {
            throw new Exception("DUPLICATE_EMAIL");
        }

        String hash = PasswordUtil.hash(password);
        UUID userId = userDAO.insert(name, email, hash, role);

        // Create role-specific profile record
        switch (role) {
            case "STUDENT"          -> studentDAO.insert(userId, department != null ? department : "CS", 0.0);
            case "SUPERVISOR"       -> supervisorDAO.insert(userId, "", "", 5);
            case "EXAMINER"         -> examinerDAO.insert(userId, false);
            case "ADMIN"            -> adminDAO.insert(userId, java.util.List.of("ALL"));
            case "INDUSTRY_PARTNER" -> partnerDAO.insert(userId, companyName != null ? companyName : "", email);
        }

        // Send OTP
        String otp = OTPService.generateAndStore(userId, "EMAIL_VERIFY");
        EmailService.sendOTP(email, otp);
        AuditLogger.logAccountChange(userId, "REGISTERED role=" + role);

        return userId;
    }

    /**
     * Verify email OTP. Returns true on success and activates the account.
     */
    public boolean verifyEmail(UUID userId, String otp) throws Exception {
        boolean valid = OTPService.verify(userId, otp, "EMAIL_VERIFY");
        if (valid) {
            userDAO.setEmailVerified(userId, true);
            AuditLogger.logAccountChange(userId, "EMAIL_VERIFIED");
        }
        return valid;
    }

    /**
     * Initiate password reset — sends OTP to the user's email.
     */
    public void initiatePasswordReset(String email) throws Exception {
        Optional<User> opt = userDAO.findByEmail(email);
        if (opt.isEmpty()) return; // Silent fail for security
        User user = opt.get();
        String otp = OTPService.generateAndStore(user.getUserId(), "PASSWORD_RESET");
        EmailService.sendOTP(email, otp);
    }

    /**
     * Complete password reset using OTP.
     */
    public boolean resetPassword(String email, String otp, String newPassword) throws Exception {
        Optional<User> opt = userDAO.findByEmail(email);
        if (opt.isEmpty()) return false;

        User user = opt.get();
        if (!OTPService.verify(user.getUserId(), otp, "PASSWORD_RESET")) return false;

        String weakness = PasswordUtil.getWeaknessReason(newPassword);
        if (weakness != null) throw new Exception(weakness);

        userDAO.updatePasswordHash(user.getUserId(), PasswordUtil.hash(newPassword));
        AuditLogger.logPasswordReset(user.getUserId());
        return true;
    }

    /**
     * Build the correctly-typed user object based on role (for SessionManager).
     */
    public User buildTypedUser(User base) throws SQLException {
        return switch (base.getRole()) {
            case "STUDENT"          -> studentDAO.findByUserId(base.getUserId()).map(u -> (User) u).orElse(base);
            case "SUPERVISOR"       -> supervisorDAO.findByUserId(base.getUserId()).map(u -> (User) u).orElse(base);
            case "EXAMINER"         -> examinerDAO.findByUserId(base.getUserId()).map(u -> (User) u).orElse(base);
            case "ADMIN"            -> adminDAO.findByUserId(base.getUserId()).map(u -> (User) u).orElse(base);
            case "INDUSTRY_PARTNER" -> partnerDAO.findByUserId(base.getUserId()).map(u -> (User) u).orElse(base);
            default                 -> base;
        };
    }
}
