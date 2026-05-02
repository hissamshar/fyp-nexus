package com.fyp.dao;

import com.fyp.model.Admin;
import com.fyp.util.DBConnection;

import java.sql.*;
import java.util.*;

public class AdminDAO {

    public Optional<Admin> findByUserId(UUID userId) throws SQLException {
        String sql = """
            SELECT u.user_id, u.name, u.email, u.password_hash, u.is_active, u.is_email_verified,
                   a.admin_id, a.permissions
            FROM fyp.users u
            JOIN fyp.admins a ON a.user_id = u.user_id
            WHERE u.user_id = ?::uuid
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public UUID insert(UUID userId, List<String> permissions) throws SQLException {
        String sql = "INSERT INTO fyp.admins (user_id, permissions) VALUES (?::uuid, ?) RETURNING admin_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            Array arr = conn.createArrayOf("text", permissions.toArray());
            ps.setArray(2, arr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("admin_id"));
            }
        }
        throw new SQLException("INSERT into fyp.admins returned no ID.");
    }

    private Admin mapRow(ResultSet rs) throws SQLException {
        List<String> perms = new ArrayList<>();
        Array arr = rs.getArray("permissions");
        if (arr != null) { for (Object o : (Object[]) arr.getArray()) perms.add((String) o); }
        return new Admin(
            UUID.fromString(rs.getString("user_id")),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getBoolean("is_active"),
            rs.getBoolean("is_email_verified"),
            UUID.fromString(rs.getString("admin_id")),
            perms
        );
    }
}
