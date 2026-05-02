package com.fyp.dao;

import com.fyp.model.Rubric;
import com.fyp.model.RubricCriterion;
import com.fyp.util.DBConnection;

import java.sql.*;
import java.util.*;

public class RubricDAO {

    public Optional<Rubric> findById(UUID rubricId) throws SQLException {
        String sql = "SELECT * FROM fyp.rubrics WHERE rubric_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rubricId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Rubric r = mapRubric(rs);
                    r.setCriteria(findCriteriaByRubricId(rubricId));
                    return Optional.of(r);
                }
            }
        }
        return Optional.empty();
    }

    public List<Rubric> findAll() throws SQLException {
        List<Rubric> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.rubrics ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRubric(rs));
        }
        return list;
    }

    public UUID insertRubric(String name, int totalPoints, String version) throws SQLException {
        String sql = "INSERT INTO fyp.rubrics (name, total_points, version) VALUES (?, ?, ?) RETURNING rubric_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, totalPoints);
            ps.setString(3, version);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("rubric_id"));
            }
        }
        throw new SQLException("INSERT into fyp.rubrics returned no ID.");
    }

    public UUID insertCriterion(RubricCriterion c) throws SQLException {
        String sql = "INSERT INTO fyp.rubric_criteria (criterion_name, max_score, description, rubric_id) " +
                     "VALUES (?, ?, ?, ?::uuid) RETURNING criterion_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getCriterionName());
            ps.setInt(2, c.getMaxScore());
            ps.setString(3, c.getDescription());
            ps.setString(4, c.getRubricId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("criterion_id"));
            }
        }
        throw new SQLException("INSERT into fyp.rubric_criteria returned no ID.");
    }

    public List<RubricCriterion> findCriteriaByRubricId(UUID rubricId) throws SQLException {
        List<RubricCriterion> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.rubric_criteria WHERE rubric_id = ?::uuid ORDER BY criterion_name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rubricId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RubricCriterion c = new RubricCriterion();
                    c.setCriterionId(UUID.fromString(rs.getString("criterion_id")));
                    c.setCriterionName(rs.getString("criterion_name"));
                    c.setMaxScore(rs.getInt("max_score"));
                    c.setDescription(rs.getString("description"));
                    c.setRubricId(rubricId);
                    list.add(c);
                }
            }
        }
        return list;
    }

    private Rubric mapRubric(ResultSet rs) throws SQLException {
        Rubric r = new Rubric();
        r.setRubricId(UUID.fromString(rs.getString("rubric_id")));
        r.setTotalPoints(rs.getInt("total_points"));
        r.setVersion(rs.getString("version"));
        r.setName(rs.getString("name"));
        return r;
    }
}
