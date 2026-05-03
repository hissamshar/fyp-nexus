package com.fyp.service;

import com.fyp.dao.NotificationDAO;
import com.fyp.enums.NotificationType;
import com.fyp.model.Notification;
import com.fyp.util.EmailService;
import com.fyp.util.SessionManager;

import java.util.List;
import java.util.UUID;

/**
 * Creates, sends, and manages in-app notifications.
 * Also optionally sends an email to the recipient.
 */
public class NotificationService {

    private final NotificationDAO notifDAO = new NotificationDAO();

    private String jwt() { return SessionManager.getJwtToken(); }

    public UUID create(UUID recipientId, NotificationType type,
                       String message, String recipientEmail) throws Exception {
        UUID id = notifDAO.insert(message, type, recipientId, jwt());
        if (recipientEmail != null) {
            EmailService.sendNotification(recipientEmail,
                    "[FYP Platform] " + type.name(), message);
        }
        return id;
    }

    public List<Notification> getForCurrentUser() throws Exception {
        UUID uid = SessionManager.getCurrentUser().getUserId();
        return notifDAO.findByRecipient(uid, jwt());
    }

    public List<Notification> getUnreadForCurrentUser() throws Exception {
        UUID uid = SessionManager.getCurrentUser().getUserId();
        return notifDAO.findUnread(uid, jwt());
    }

    public int getUnreadCount(UUID userId) throws Exception {
        return notifDAO.countUnread(userId, jwt());
    }

    public void markRead(UUID notifId) throws Exception {
        notifDAO.markAsRead(notifId, jwt());
    }

    public void markAllRead(UUID userId) throws Exception {
        notifDAO.markAllAsRead(userId, jwt());
    }

    // ── Adapter methods for NotificationPanelController ────────────────────────

    public List<Notification> getNotificationsForUser(UUID userId, String token) {
        try { return notifDAO.findByRecipient(userId, token); }
        catch (Exception e) { e.printStackTrace(); return java.util.List.of(); }
    }

    public boolean markRead(UUID notifId, String token) {
        try { notifDAO.markAsRead(notifId, token); return true; }
        catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean markAllRead(UUID userId, String token) {
        try { notifDAO.markAllAsRead(userId, token); return true; }
        catch (Exception e) { e.printStackTrace(); return false; }
    }
}
