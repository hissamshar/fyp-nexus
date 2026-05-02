package com.fyp.dao;

import com.fyp.model.Student;
import com.fyp.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class StudentDAO {

    public Optional<Student> findByUserId(UUID userId) throws SQLException {
        String sql = """
            SELECT u.user_id, u.name, u.email, u.password_hash, u.is_active, u.is_email_verified,
                   s.student_id, s.department, s.cgpa
            FROM fyp.users u
            JOIN fyp.students s ON s.user_id = u.user_id
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

    public Optional<Student> findByStudentId(UUID studentId) throws SQLException {
        String sql = """
            SELECT u.user_id, u.name, u.email, u.password_hash, u.is_active, u.is_email_verified,
                   s.student_id, s.department, s.cgpa
            FROM fyp.users u
            JOIN fyp.students s ON s.user_id = u.user_id
            WHERE s.student_id = ?::uuid
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Student> findAll() throws SQLException {
        List<Student> list = new ArrayList<>();
        String sql = """
            SELECT u.user_id, u.name, u.email, u.password_hash, u.is_active, u.is_email_verified,
                   s.student_id, s.department, s.cgpa
            FROM fyp.users u
            JOIN fyp.students s ON s.user_id = u.user_id
            ORDER BY u.name
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public UUID insert(UUID userId, String department, double cgpa) throws SQLException {
        String sql = "INSERT INTO fyp.students (user_id, department, cgpa) " +
                     "VALUES (?::uuid, ?, ?) RETURNING student_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setString(2, department);
            ps.setDouble(3, cgpa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("student_id"));
            }
        }
        throw new SQLException("INSERT into fyp.students returned no ID.");
    }

    public void update(UUID studentId, String department, double cgpa) throws SQLException {
        String sql = "UPDATE fyp.students SET department = ?, cgpa = ? WHERE student_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, department);
            ps.setDouble(2, cgpa);
            ps.setString(3, studentId.toString());
            ps.executeUpdate();
        }
    }

    private Student mapRow(ResultSet rs) throws SQLException {
        return new Student(
            UUID.fromString(rs.getString("user_id")),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getBoolean("is_active"),
            rs.getBoolean("is_email_verified"),
            UUID.fromString(rs.getString("student_id")),
            rs.getString("department"),
            rs.getDouble("cgpa")
        );
    }
}
