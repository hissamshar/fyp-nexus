package com.fyp.dao;

import com.fyp.model.ProgressReport;
import com.fyp.util.DBConnection;

import java.sql.*;
import java.util.*;

public class ProgressReportDAO {

    public List<ProgressReport> findByProjectId(UUID projectId) throws SQLException {
        List<ProgressReport> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.progress_reports WHERE project_id = ?::uuid ORDER BY submitted_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public UUID insert(ProgressReport r) throws SQLException {
        String sql = "INSERT INTO fyp.progress_reports (work_done, issues_faced, next_steps, project_id, student_id) " +
                     "VALUES (?, ?, ?, ?::uuid, ?::uuid) RETURNING report_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getWorkDone());
            ps.setString(2, r.getIssuesFaced());
            ps.setString(3, r.getNextSteps());
            ps.setString(4, r.getProjectId().toString());
            ps.setString(5, r.getStudentId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("report_id"));
            }
        }
        throw new SQLException("INSERT into fyp.progress_reports returned no ID.");
    }

    private ProgressReport mapRow(ResultSet rs) throws SQLException {
        ProgressReport r = new ProgressReport();
        r.setReportId(UUID.fromString(rs.getString("report_id")));
        r.setWorkDone(rs.getString("work_done"));
        r.setIssuesFaced(rs.getString("issues_faced"));
        r.setNextSteps(rs.getString("next_steps"));
        Timestamp ts = rs.getTimestamp("submitted_at");
        if (ts != null) r.setSubmittedAt(ts.toLocalDateTime());
        r.setProjectId(UUID.fromString(rs.getString("project_id")));
        r.setStudentId(UUID.fromString(rs.getString("student_id")));
        return r;
    }
}
