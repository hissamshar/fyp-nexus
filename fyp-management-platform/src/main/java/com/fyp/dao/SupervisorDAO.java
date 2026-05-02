package com.fyp.dao;

import com.fyp.model.Supervisor;
import com.fyp.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SupervisorDAO {

    public Optional<Supervisor> findByUserId(UUID userId) throws SQLException {
        String sql = """
            SELECT u.user_id, u.name, u.email, u.password_hash, u.is_active, u.is_email_verified,
                   s.supervisor_id, s.employee_id, s.research_area, s.slots_available
            FROM fyp.users u
            JOIN fyp.supervisors s ON s.user_id = u.user_id
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

    public Optional<Supervisor> findBySupervisorId(UUID supervisorId) throws SQLException {
        String sql = """
            SELECT u.user_id, u.name, u.email, u.password_hash, u.is_active, u.is_email_verified,
                   s.supervisor_id, s.employee_id, s.research_area, s.slots_available
            FROM fyp.users u
            JOIN fyp.supervisors s ON s.user_id = u.user_id
            WHERE s.supervisor_id = ?::uuid
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, supervisorId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Supervisor> findAll() throws SQLException {
        List<Supervisor> list = new ArrayList<>();
        String sql = """
            SELECT u.user_id, u.name, u.email, u.password_hash, u.is_active, u.is_email_verified,
                   s.supervisor_id, s.employee_id, s.research_area, s.slots_available
            FROM fyp.users u
            JOIN fyp.supervisors s ON s.user_id = u.user_id
            WHERE u.is_active = TRUE
            ORDER BY u.name
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Supervisor> findWithAvailableSlots() throws SQLException {
        List<Supervisor> list = new ArrayList<>();
        String sql = """
            SELECT u.user_id, u.name, u.email, u.password_hash, u.is_active, u.is_email_verified,
                   s.supervisor_id, s.employee_id, s.research_area, s.slots_available
            FROM fyp.users u
            JOIN fyp.supervisors s ON s.user_id = u.user_id
            WHERE u.is_active = TRUE AND s.slots_available > 0
            ORDER BY s.slots_available DESC, u.name
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public UUID insert(UUID userId, String employeeId, String researchArea, int slotsAvailable) throws SQLException {
        String sql = "INSERT INTO fyp.supervisors (user_id, employee_id, research_area, slots_available) " +
                     "VALUES (?::uuid, ?, ?, ?) RETURNING supervisor_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setString(2, employeeId);
            ps.setString(3, researchArea);
            ps.setInt(4, slotsAvailable);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("supervisor_id"));
            }
        }
        throw new SQLException("INSERT into fyp.supervisors returned no ID.");
    }

    public void update(UUID supervisorId, String researchArea, int slotsAvailable) throws SQLException {
        String sql = "UPDATE fyp.supervisors SET research_area = ?, slots_available = ? " +
                     "WHERE supervisor_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, researchArea);
            ps.setInt(2, slotsAvailable);
            ps.setString(3, supervisorId.toString());
            ps.executeUpdate();
        }
    }

    public void decrementSlots(UUID supervisorId) throws SQLException {
        String sql = "UPDATE fyp.supervisors SET slots_available = GREATEST(0, slots_available - 1) " +
                     "WHERE supervisor_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, supervisorId.toString());
            ps.executeUpdate();
        }
    }

    public void incrementSlots(UUID supervisorId) throws SQLException {
        String sql = "UPDATE fyp.supervisors SET slots_available = slots_available + 1 " +
                     "WHERE supervisor_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, supervisorId.toString());
            ps.executeUpdate();
        }
    }

    private Supervisor mapRow(ResultSet rs) throws SQLException {
        return new Supervisor(
            UUID.fromString(rs.getString("user_id")),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getBoolean("is_active"),
            rs.getBoolean("is_email_verified"),
            UUID.fromString(rs.getString("supervisor_id")),
            rs.getString("employee_id"),
            rs.getString("research_area"),
            rs.getInt("slots_available")
        );
    }
}
