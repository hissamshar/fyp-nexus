package com.fyp.dao;

import com.fyp.model.Project;
import com.fyp.enums.ProjectStatus;
import com.fyp.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class ProjectDAO {

    public Optional<Project> findById(UUID projectId) throws SQLException {
        String sql = "SELECT * FROM fyp.projects WHERE project_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<Project> findByProposalId(UUID proposalId) throws SQLException {
        String sql = "SELECT * FROM fyp.projects WHERE proposal_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, proposalId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Project> findAll() throws SQLException {
        List<Project> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.projects ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Project> findByStatus(ProjectStatus status) throws SQLException {
        List<Project> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.projects WHERE status = ? ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Project> findBySupervisorId(UUID supervisorId) throws SQLException {
        List<Project> list = new ArrayList<>();
        String sql = """
            SELECT p.* FROM fyp.projects p
            JOIN fyp.supervisor_assignments sa ON sa.project_id = p.project_id
            WHERE sa.supervisor_id = ?::uuid
            ORDER BY p.created_at DESC
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, supervisorId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public UUID insert(UUID proposalId) throws SQLException {
        String sql = "INSERT INTO fyp.projects (proposal_id, start_date, status) " +
                     "VALUES (?::uuid, ?, 'INITIATED') RETURNING project_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, proposalId.toString());
            ps.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("project_id"));
            }
        }
        throw new SQLException("INSERT into fyp.projects returned no ID.");
    }

    public void updateStatus(UUID projectId, ProjectStatus status) throws SQLException {
        String sql = "UPDATE fyp.projects SET status = ? WHERE project_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, projectId.toString());
            ps.executeUpdate();
        }
    }

    public void updateRepoUrl(UUID projectId, String repoUrl) throws SQLException {
        String sql = "UPDATE fyp.projects SET repo_url = ? WHERE project_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, repoUrl);
            ps.setString(2, projectId.toString());
            ps.executeUpdate();
        }
    }

    // Count by status for analytics
    public Map<String, Integer> countByStatus() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT status, COUNT(*) as cnt FROM fyp.projects GROUP BY status";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) map.put(rs.getString("status"), rs.getInt("cnt"));
        }
        return map;
    }

    private Project mapRow(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setProjectId(UUID.fromString(rs.getString("project_id")));
        java.sql.Date sd = rs.getDate("start_date");
        if (sd != null) p.setStartDate(sd.toLocalDate());
        java.sql.Date ed = rs.getDate("end_date");
        if (ed != null) p.setEndDate(ed.toLocalDate());
        p.setRepoUrl(rs.getString("repo_url"));
        p.setStatus(ProjectStatus.valueOf(rs.getString("status")));
        p.setProposalId(UUID.fromString(rs.getString("proposal_id")));
        return p;
    }
}
