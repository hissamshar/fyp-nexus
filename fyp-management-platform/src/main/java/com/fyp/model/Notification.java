package com.fyp.model;

import com.fyp.enums.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;

public class Notification {
    private UUID notifId;
    private String message;
    private NotificationType type;
    private boolean isRead;
    private UUID recipientId;
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(UUID notifId, String message, NotificationType type,
                        boolean isRead, UUID recipientId, LocalDateTime createdAt) {
        this.notifId     = notifId;
        this.message     = message;
        this.type        = type;
        this.isRead      = isRead;
        this.recipientId = recipientId;
        this.createdAt   = createdAt;
    }

    public void send(UUID recipientId) {
        this.recipientId = recipientId;
        // Persisted via NotificationDAO
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public UUID getNotifId()          { return notifId; }
    public String getMessage()        { return message; }
    public NotificationType getType() { return type; }
    public boolean isRead()           { return isRead; }
    public UUID getRecipientId()      { return recipientId; }
    public LocalDateTime getCreatedAt(){ return createdAt; }

    public void setNotifId(UUID notifId)             { this.notifId = notifId; }
    public void setMessage(String message)           { this.message = message; }
    public void setType(NotificationType type)       { this.type = type; }
    public void setRead(boolean read)                { isRead = read; }
    public void setRecipientId(UUID recipientId)     { this.recipientId = recipientId; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }
}
