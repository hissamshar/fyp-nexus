package com.fyp.dao;

import com.fyp.model.SemesterDeadline;
import com.fyp.util.DBConnection;

import java.sql.*;
import java.util.*;

public class SemesterDeadlineDAO {

    public List<SemesterDeadline> findAll() throws SQLException {
        List<SemesterDeadline> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.semester_deadlines ORDER BY due_date ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<SemesterDeadline> findBySemester(String semester) throws SQLException {
        List<SemesterDeadline> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.semester_deadlines WHERE semester = ? ORDER BY due_date ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, semester);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public UUID insert(SemesterDeadline d) throws SQLException {
        String sql = "INSERT INTO fyp.semester_deadlines (semester, deadline_type, due_date, description) " +
                     "VALUES (?, ?, ?, ?) RETURNING deadline_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, d.getSemester());
            ps.setString(2, d.getDeadlineType());
            ps.setObject(3, d.getDueDate());
            ps.setString(4, d.getDescription());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("deadline_id"));
            }
        }
        throw new SQLException("INSERT into fyp.semester_deadlines returned no ID.");
    }

    public void update(SemesterDeadline d) throws SQLException {
        String sql = "UPDATE fyp.semester_deadlines SET semester=?, deadline_type=?, due_date=?, description=? " +
                     "WHERE deadline_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, d.getSemester());
            ps.setString(2, d.getDeadlineType());
            ps.setObject(3, d.getDueDate());
            ps.setString(4, d.getDescription());
            ps.setString(5, d.getDeadlineId().toString());
            ps.executeUpdate();
        }
    }

    public void delete(UUID deadlineId) throws SQLException {
        String sql = "DELETE FROM fyp.semester_deadlines WHERE deadline_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, deadlineId.toString());
            ps.executeUpdate();
        }
    }

    private SemesterDeadline mapRow(ResultSet rs) throws SQLException {
        SemesterDeadline d = new SemesterDeadline();
        d.setDeadlineId(UUID.fromString(rs.getString("deadline_id")));
        d.setSemester(rs.getString("semester"));
        d.setDeadlineType(rs.getString("deadline_type"));
        Timestamp ts = rs.getTimestamp("due_date");
        if (ts != null) d.setDueDate(ts.toLocalDateTime());
        d.setDescription(rs.getString("description"));
        return d;
    }
}
