package com.fyp.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Simple data class for audit log entries returned from the DB.
 */
public class AuditLogEntry {
    private UUID logId;
    private UUID userId;
    private String action;
    private String ipAddress;
    private LocalDateTime timestamp;
    private String details;

    public AuditLogEntry() {}

    public AuditLogEntry(UUID logId, UUID userId, String action,
                         String ipAddress, LocalDateTime timestamp, String details) {
        this.logId     = logId;
        this.userId    = userId;
        this.action    = action;
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
        this.details   = details;
    }

    public UUID getLogId()         { return logId; }
    public UUID getUserId()        { return userId; }
    public String getAction()      { return action; }
    public String getIpAddress()   { return ipAddress; }
    public LocalDateTime getTimestamp(){ return timestamp; }
    public String getDetails()     { return details; }

    public void setLogId(UUID logId)           { this.logId = logId; }
    public void setUserId(UUID userId)         { this.userId = userId; }
    public void setAction(String action)       { this.action = action; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setTimestamp(LocalDateTime ts) { this.timestamp = ts; }
    public void setDetails(String details)     { this.details = details; }
}
