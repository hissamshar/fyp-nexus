package com.fyp.dao;

import com.fyp.model.Grade;
import com.fyp.model.GradeEntry;
import com.fyp.util.DBConnection;

import java.sql.*;
import java.util.*;

public class GradeDAO {

    public Optional<Grade> findByProjectId(UUID projectId) throws SQLException {
        String sql = "SELECT * FROM fyp.grades WHERE project_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapGrade(rs));
            }
        }
        return Optional.empty();
    }

    public UUID insertGrade(UUID projectId, UUID rubricId) throws SQLException {
        String sql = "INSERT INTO fyp.grades (project_id, rubric_id, is_published) " +
                     "VALUES (?::uuid, ?::uuid, FALSE) RETURNING grade_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectId.toString());
            ps.setString(2, rubricId != null ? rubricId.toString() : null);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("grade_id"));
            }
        }
        throw new SQLException("INSERT into fyp.grades returned no ID.");
    }

    public void publishGrade(UUID gradeId, String letterGrade) throws SQLException {
        String sql = "UPDATE fyp.grades SET is_published = TRUE, letter_grade = ? WHERE grade_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, letterGrade);
            ps.setString(2, gradeId.toString());
            ps.executeUpdate();
        }
    }

    public UUID insertGradeEntry(GradeEntry entry) throws SQLException {
        String sql = "INSERT INTO fyp.grade_entries (score, comments, grade_id, grader_id, criterion_id) " +
                     "VALUES (?, ?, ?::uuid, ?::uuid, ?::uuid) RETURNING entry_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entry.getScore());
            ps.setString(2, entry.getComments());
            ps.setString(3, entry.getGradeId().toString());
            ps.setString(4, entry.getGraderId().toString());
            ps.setString(5, entry.getCriterionId() != null ? entry.getCriterionId().toString() : null);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("entry_id"));
            }
        }
        throw new SQLException("INSERT into fyp.grade_entries returned no ID.");
    }

    public List<GradeEntry> findEntriesByGradeId(UUID gradeId) throws SQLException {
        List<GradeEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.grade_entries WHERE grade_id = ?::uuid ORDER BY graded_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, gradeId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    GradeEntry e = new GradeEntry();
                    e.setEntryId(UUID.fromString(rs.getString("entry_id")));
                    e.setScore(rs.getInt("score"));
                    e.setComments(rs.getString("comments"));
                    e.setGradedAt(rs.getTimestamp("graded_at").toLocalDateTime());
                    e.setGradeId(UUID.fromString(rs.getString("grade_id")));
                    e.setGraderId(UUID.fromString(rs.getString("grader_id")));
                    String cid = rs.getString("criterion_id");
                    if (cid != null) e.setCriterionId(UUID.fromString(cid));
                    list.add(e);
                }
            }
        }
        return list;
    }

    public double calculateWeightedTotal(UUID gradeId) throws SQLException {
        String sql = "SELECT SUM(ge.score) as total FROM fyp.grade_entries ge WHERE ge.grade_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, gradeId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        }
        return 0.0;
    }

    private Grade mapGrade(ResultSet rs) throws SQLException {
        Grade g = new Grade();
        g.setGradeId(UUID.fromString(rs.getString("grade_id")));
        g.setLetterGrade(rs.getString("letter_grade"));
        g.setPublished(rs.getBoolean("is_published"));
        g.setProjectId(UUID.fromString(rs.getString("project_id")));
        String rid = rs.getString("rubric_id");
        if (rid != null) g.setRubricId(UUID.fromString(rid));
        return g;
    }
}
