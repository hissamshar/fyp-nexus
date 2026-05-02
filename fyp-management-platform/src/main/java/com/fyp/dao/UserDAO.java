package com.fyp.dao;

import com.fyp.model.User;
import com.fyp.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DAO for fyp.users table.
 * All queries use PreparedStatement — no string concatenation.
 */
public class UserDAO {

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT user_id, name, email, password_hash, role, is_active, is_email_verified " +
                     "FROM fyp.users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<User> findById(UUID userId) throws SQLException {
        String sql = "SELECT user_id, name, email, password_hash, role, is_active, is_email_verified " +
                     "FROM fyp.users WHERE user_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, name, email, password_hash, role, is_active, is_email_verified " +
                     "FROM fyp.users ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) users.add(mapRow(rs));
        }
        return users;
    }

    public List<User> findByRole(String role) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, name, email, password_hash, role, is_active, is_email_verified " +
                     "FROM fyp.users WHERE role = ? ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) users.add(mapRow(rs));
            }
        }
        return users;
    }

    public UUID insert(String name, String email, String passwordHash, String role) throws SQLException {
        String sql = "INSERT INTO fyp.users (name, email, password_hash, role) " +
                     "VALUES (?, ?, ?, ?) RETURNING user_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, role);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("user_id"));
            }
        }
        throw new SQLException("INSERT into fyp.users returned no ID.");
    }

    public void updateProfile(UUID userId, String name, String email) throws SQLException {
        String sql = "UPDATE fyp.users SET name = ?, email = ? WHERE user_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, userId.toString());
            ps.executeUpdate();
        }
    }

    public void updatePasswordHash(UUID userId, String newHash) throws SQLException {
        String sql = "UPDATE fyp.users SET password_hash = ? WHERE user_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, userId.toString());
            ps.executeUpdate();
        }
    }

    public void setEmailVerified(UUID userId, boolean verified) throws SQLException {
        String sql = "UPDATE fyp.users SET is_email_verified = ? WHERE user_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, verified);
            ps.setString(2, userId.toString());
            ps.executeUpdate();
        }
    }

    public void setActive(UUID userId, boolean active) throws SQLException {
        String sql = "UPDATE fyp.users SET is_active = ? WHERE user_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setString(2, userId.toString());
            ps.executeUpdate();
        }
    }

    public void incrementFailedAttempts(UUID userId) throws SQLException {
        String sql = "UPDATE fyp.users SET failed_login_attempts = failed_login_attempts + 1 " +
                     "WHERE user_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.executeUpdate();
        }
    }

    public void resetFailedAttempts(UUID userId) throws SQLException {
        String sql = "UPDATE fyp.users SET failed_login_attempts = 0, locked_until = NULL " +
                     "WHERE user_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.executeUpdate();
        }
    }

    public int getFailedAttempts(UUID userId) throws SQLException {
        String sql = "SELECT failed_login_attempts FROM fyp.users WHERE user_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("failed_login_attempts");
            }
        }
        return 0;
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM fyp.users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        // Returns a lightweight User stub — full role objects built in role-specific DAOs
        com.fyp.model.User u = new com.fyp.model.User(
            UUID.fromString(rs.getString("user_id")),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getBoolean("is_active"),
            rs.getBoolean("is_email_verified"),
            rs.getString("role")
        ) {};  // anonymous subclass since User is abstract
        return u;
    }
}
