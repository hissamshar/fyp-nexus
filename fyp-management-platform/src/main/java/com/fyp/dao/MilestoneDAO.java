package com.fyp.dao;

import com.fyp.model.Milestone;
import com.fyp.enums.MilestoneStatus;
import com.fyp.util.DBConnection;

import java.sql.*;
import java.util.*;

public class MilestoneDAO {

    public Optional<Milestone> findById(UUID milestoneId) throws SQLException {
        String sql = "SELECT * FROM fyp.milestones WHERE milestone_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, milestoneId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Milestone> findByProjectId(UUID projectId) throws SQLException {
        List<Milestone> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.milestones WHERE project_id = ?::uuid ORDER BY deadline ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public UUID insert(Milestone m) throws SQLException {
        String sql = "INSERT INTO fyp.milestones (title, description, deadline, weightage, status, project_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?::uuid) RETURNING milestone_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getTitle());
            ps.setString(2, m.getDescription());
            ps.setObject(3, m.getDeadline());
            ps.setInt(4, m.getWeightage());
            ps.setString(5, m.getStatus().name());
            ps.setString(6, m.getProjectId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("milestone_id"));
            }
        }
        throw new SQLException("INSERT into fyp.milestones returned no ID.");
    }

    public void updateStatus(UUID milestoneId, MilestoneStatus status) throws SQLException {
        String sql = "UPDATE fyp.milestones SET status = ? WHERE milestone_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, milestoneId.toString());
            ps.executeUpdate();
        }
    }

    public void update(Milestone m) throws SQLException {
        String sql = "UPDATE fyp.milestones SET title=?, description=?, deadline=?, weightage=?, status=? " +
                     "WHERE milestone_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getTitle());
            ps.setString(2, m.getDescription());
            ps.setObject(3, m.getDeadline());
            ps.setInt(4, m.getWeightage());
            ps.setString(5, m.getStatus().name());
            ps.setString(6, m.getMilestoneId().toString());
            ps.executeUpdate();
        }
    }

    public void delete(UUID milestoneId) throws SQLException {
        String sql = "DELETE FROM fyp.milestones WHERE milestone_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, milestoneId.toString());
            ps.executeUpdate();
        }
    }

    /** Returns (completed, total) milestone counts for a project */
    public int[] getProgressCounts(UUID projectId) throws SQLException {
        String sql = "SELECT COUNT(*) as total, " +
                     "SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END) as completed " +
                     "FROM fyp.milestones WHERE project_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new int[]{rs.getInt("completed"), rs.getInt("total")};
            }
        }
        return new int[]{0, 0};
    }

    private Milestone mapRow(ResultSet rs) throws SQLException {
        Milestone m = new Milestone();
        m.setMilestoneId(UUID.fromString(rs.getString("milestone_id")));
        m.setTitle(rs.getString("title"));
        m.setDescription(rs.getString("description"));
        Timestamp dl = rs.getTimestamp("deadline");
        if (dl != null) m.setDeadline(dl.toLocalDateTime());
        m.setWeightage(rs.getInt("weightage"));
        m.setStatus(MilestoneStatus.valueOf(rs.getString("status")));
        m.setProjectId(UUID.fromString(rs.getString("project_id")));
        return m;
    }
}
