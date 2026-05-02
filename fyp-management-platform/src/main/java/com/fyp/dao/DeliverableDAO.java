package com.fyp.dao;

import com.fyp.model.Deliverable;
import com.fyp.util.DBConnection;

import java.sql.*;
import java.util.*;

public class DeliverableDAO {

    public List<Deliverable> findByMilestoneId(UUID milestoneId) throws SQLException {
        List<Deliverable> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.deliverables WHERE milestone_id = ?::uuid ORDER BY upload_timestamp DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, milestoneId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Deliverable> findByStudentId(UUID studentId) throws SQLException {
        List<Deliverable> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.deliverables WHERE student_id = ?::uuid ORDER BY upload_timestamp DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public UUID insert(Deliverable d) throws SQLException {
        String sql = "INSERT INTO fyp.deliverables (file_name, file_type, file_path, milestone_id, student_id) " +
                     "VALUES (?, ?, ?, ?::uuid, ?::uuid) RETURNING file_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, d.getFileName());
            ps.setString(2, d.getFileType());
            ps.setString(3, d.getFilePath());
            ps.setString(4, d.getMilestoneId().toString());
            ps.setString(5, d.getStudentId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("file_id"));
            }
        }
        throw new SQLException("INSERT into fyp.deliverables returned no ID.");
    }

    public void delete(UUID fileId) throws SQLException {
        String sql = "DELETE FROM fyp.deliverables WHERE file_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fileId.toString());
            ps.executeUpdate();
        }
    }

    private Deliverable mapRow(ResultSet rs) throws SQLException {
        Deliverable d = new Deliverable();
        d.setFileId(UUID.fromString(rs.getString("file_id")));
        d.setFileName(rs.getString("file_name"));
        d.setFileType(rs.getString("file_type"));
        d.setFilePath(rs.getString("file_path"));
        d.setMilestoneId(UUID.fromString(rs.getString("milestone_id")));
        d.setStudentId(UUID.fromString(rs.getString("student_id")));
        Timestamp ts = rs.getTimestamp("upload_timestamp");
        if (ts != null) d.setUploadTimestamp(ts.toLocalDateTime());
        return d;
    }
}
