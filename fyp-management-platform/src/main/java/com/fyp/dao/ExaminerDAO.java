package com.fyp.dao;

import com.fyp.model.Examiner;
import com.fyp.util.DBConnection;

import java.sql.*;
import java.util.*;

public class ExaminerDAO {

    public Optional<Examiner> findByUserId(UUID userId) throws SQLException {
        String sql = """
            SELECT u.user_id, u.name, u.email, u.password_hash, u.is_active, u.is_email_verified,
                   e.examiner_id, e.expertise, e.is_external
            FROM fyp.users u
            JOIN fyp.examiners e ON e.user_id = u.user_id
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

    public List<Examiner> findAll() throws SQLException {
        List<Examiner> list = new ArrayList<>();
        String sql = """
            SELECT u.user_id, u.name, u.email, u.password_hash, u.is_active, u.is_email_verified,
                   e.examiner_id, e.expertise, e.is_external
            FROM fyp.users u
            JOIN fyp.examiners e ON e.user_id = u.user_id
            WHERE u.is_active = TRUE ORDER BY u.name
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public UUID insert(UUID userId, boolean isExternal) throws SQLException {
        String sql = "INSERT INTO fyp.examiners (user_id, is_external) VALUES (?::uuid, ?) RETURNING examiner_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setBoolean(2, isExternal);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("examiner_id"));
            }
        }
        throw new SQLException("INSERT into fyp.examiners returned no ID.");
    }

    public void assignToProject(UUID projectId, UUID examinerId) throws SQLException {
        String sql = "INSERT INTO fyp.examiner_assignments (project_id, examiner_id) VALUES (?::uuid, ?::uuid) " +
                     "ON CONFLICT (project_id, examiner_id) DO NOTHING";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectId.toString());
            ps.setString(2, examinerId.toString());
            ps.executeUpdate();
        }
    }

    public List<Examiner> findByProjectId(UUID projectId) throws SQLException {
        List<Examiner> list = new ArrayList<>();
        String sql = """
            SELECT u.user_id, u.name, u.email, u.password_hash, u.is_active, u.is_email_verified,
                   e.examiner_id, e.expertise, e.is_external
            FROM fyp.users u
            JOIN fyp.examiners e ON e.user_id = u.user_id
            JOIN fyp.examiner_assignments ea ON ea.examiner_id = e.examiner_id
            WHERE ea.project_id = ?::uuid
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Examiner mapRow(ResultSet rs) throws SQLException {
        Array expertiseArr = rs.getArray("expertise");
        List<String> expertise = new ArrayList<>();
        if (expertiseArr != null) {
            for (Object o : (Object[]) expertiseArr.getArray()) expertise.add((String) o);
        }
        return new Examiner(
            UUID.fromString(rs.getString("user_id")),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getBoolean("is_active"),
            rs.getBoolean("is_email_verified"),
            UUID.fromString(rs.getString("examiner_id")),
            expertise,
            rs.getBoolean("is_external")
        );
    }
}
