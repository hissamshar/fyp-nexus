package com.fyp.dao;

import com.fyp.model.AuditLogEntry;
import com.fyp.util.DBConnection;

import java.sql.*;
import java.util.*;

public class AuditLogDAO {

    public List<AuditLogEntry> findAll(int limit) throws SQLException {
        List<AuditLogEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.audit_logs ORDER BY timestamp DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<AuditLogEntry> findByUserId(UUID userId, int limit) throws SQLException {
        List<AuditLogEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.audit_logs WHERE user_id = ?::uuid ORDER BY timestamp DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<AuditLogEntry> findByAction(String action) throws SQLException {
        List<AuditLogEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.audit_logs WHERE action = ? ORDER BY timestamp DESC LIMIT 500";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, action);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private AuditLogEntry mapRow(ResultSet rs) throws SQLException {
        AuditLogEntry e = new AuditLogEntry();
        e.setLogId(UUID.fromString(rs.getString("log_id")));
        String uid = rs.getString("user_id");
        if (uid != null) e.setUserId(UUID.fromString(uid));
        e.setAction(rs.getString("action"));
        e.setIpAddress(rs.getString("ip_address"));
        Timestamp ts = rs.getTimestamp("timestamp");
        if (ts != null) e.setTimestamp(ts.toLocalDateTime());
        e.setDetails(rs.getString("details"));
        return e;
    }
}
