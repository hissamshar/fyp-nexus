package com.fyp.util;

import com.fyp.model.User;
import java.time.LocalDateTime;

/**
 * Singleton session manager. Tracks the currently logged-in user
 * and enforces 30-minute inactivity timeout.
 */
public class SessionManager {

    private static User currentUser;
    private static String jwtToken;
    private static LocalDateTime lastActivityTime;
    private static final long TIMEOUT_MINUTES = 30;

    private SessionManager() {}

    public static void setCurrentUser(User user, String token) {
        currentUser      = user;
        jwtToken         = token;
        lastActivityTime = LocalDateTime.now();
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static String getJwtToken() {
        return jwtToken;
    }

    /**
     * Refresh activity timestamp. Call this on every user action.
     */
    public static void refreshActivity() {
        lastActivityTime = LocalDateTime.now();
    }

    /**
     * Returns true if no user is logged in or session has timed out.
     */
    public static boolean isSessionExpired() {
        if (currentUser == null) return true;
        if (lastActivityTime == null) return true;
        return lastActivityTime.plusMinutes(TIMEOUT_MINUTES).isBefore(LocalDateTime.now());
    }

    /**
     * Clear the current session.
     */
    public static void clearSession() {
        currentUser      = null;
        jwtToken         = null;
        lastActivityTime = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null && !isSessionExpired();
    }

    public static String getCurrentRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }
}
