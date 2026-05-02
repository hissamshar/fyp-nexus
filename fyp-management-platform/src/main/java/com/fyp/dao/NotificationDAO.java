package com.fyp.dao;

import com.fyp.model.Notification;
import com.fyp.enums.NotificationType;
import com.fyp.util.DBConnection;

import java.sql.*;
import java.util.*;

public class NotificationDAO {

    public List<Notification> findByRecipient(UUID recipientId) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.notifications WHERE recipient_id = ?::uuid ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, recipientId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Notification> findUnread(UUID recipientId) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.notifications WHERE recipient_id = ?::uuid AND is_read = FALSE " +
                     "ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, recipientId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public int countUnread(UUID recipientId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM fyp.notifications WHERE recipient_id = ?::uuid AND is_read = FALSE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, recipientId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public UUID insert(String message, NotificationType type, UUID recipientId) throws SQLException {
        String sql = "INSERT INTO fyp.notifications (message, type, recipient_id) " +
                     "VALUES (?, ?, ?::uuid) RETURNING notif_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, message);
            ps.setString(2, type.name());
            ps.setString(3, recipientId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("notif_id"));
            }
        }
        throw new SQLException("INSERT into fyp.notifications returned no ID.");
    }

    public void markAsRead(UUID notifId) throws SQLException {
        String sql = "UPDATE fyp.notifications SET is_read = TRUE WHERE notif_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, notifId.toString());
            ps.executeUpdate();
        }
    }

    public void markAllAsRead(UUID recipientId) throws SQLException {
        String sql = "UPDATE fyp.notifications SET is_read = TRUE WHERE recipient_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, recipientId.toString());
            ps.executeUpdate();
        }
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotifId(UUID.fromString(rs.getString("notif_id")));
        n.setMessage(rs.getString("message"));
        n.setType(NotificationType.valueOf(rs.getString("type")));
        n.setRead(rs.getBoolean("is_read"));
        n.setRecipientId(UUID.fromString(rs.getString("recipient_id")));
        n.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return n;
    }
}
