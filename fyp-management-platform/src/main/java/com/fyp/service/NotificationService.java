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

    /**
     * Create a notification and optionally send an email.
     */
    public UUID create(UUID recipientId, NotificationType type,
                       String message, String recipientEmail) throws Exception {
        UUID id = notifDAO.insert(message, type, recipientId);
        if (recipientEmail != null) {
            EmailService.sendNotification(recipientEmail,
                    "[FYP Platform] " + type.name(), message);
        }
        return id;
    }

    public List<Notification> getForCurrentUser() throws Exception {
        UUID uid = SessionManager.getCurrentUser().getUserId();
        return notifDAO.findByRecipient(uid);
    }

    public List<Notification> getUnreadForCurrentUser() throws Exception {
        UUID uid = SessionManager.getCurrentUser().getUserId();
        return notifDAO.findUnread(uid);
    }

    public int getUnreadCount(UUID userId) throws Exception {
        return notifDAO.countUnread(userId);
    }

    public void markRead(UUID notifId) throws Exception {
        notifDAO.markAsRead(notifId);
    }

    public void markAllRead(UUID userId) throws Exception {
        notifDAO.markAllAsRead(userId);
    }
}
