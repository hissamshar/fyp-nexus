package com.fyp.dao;

import com.fyp.model.IndustryProblem;
import com.fyp.util.DBConnection;

import java.sql.*;
import java.util.*;

public class IndustryProblemDAO {

    public List<IndustryProblem> findAll() throws SQLException {
        List<IndustryProblem> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.industry_problems WHERE is_active = TRUE ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<IndustryProblem> findByPartnerId(UUID partnerId) throws SQLException {
        List<IndustryProblem> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.industry_problems WHERE partner_id = ?::uuid ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, partnerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public UUID insert(IndustryProblem p) throws SQLException {
        String sql = "INSERT INTO fyp.industry_problems (title, description, domain, contact_email, partner_id) " +
                     "VALUES (?, ?, ?, ?, ?::uuid) RETURNING problem_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getTitle());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getDomain());
            ps.setString(4, p.getContactEmail());
            ps.setString(5, p.getPartnerId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("problem_id"));
            }
        }
        throw new SQLException("INSERT into fyp.industry_problems returned no ID.");
    }

    public void adopt(UUID problemId, UUID studentId) throws SQLException {
        String sql = "UPDATE fyp.industry_problems SET adopted_by = ?::uuid WHERE problem_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId.toString());
            ps.setString(2, problemId.toString());
            ps.executeUpdate();
        }
    }

    public List<IndustryProblem> searchByKeyword(String keyword) throws SQLException {
        List<IndustryProblem> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.industry_problems WHERE is_active = TRUE " +
                     "AND (LOWER(title) LIKE LOWER(?) OR LOWER(description) LIKE LOWER(?) OR LOWER(domain) LIKE LOWER(?)) " +
                     "ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private IndustryProblem mapRow(ResultSet rs) throws SQLException {
        IndustryProblem p = new IndustryProblem();
        p.setProblemId(UUID.fromString(rs.getString("problem_id")));
        p.setTitle(rs.getString("title"));
        p.setDescription(rs.getString("description"));
        p.setDomain(rs.getString("domain"));
        p.setContactEmail(rs.getString("contact_email"));
        p.setPartnerId(UUID.fromString(rs.getString("partner_id")));
        String ab = rs.getString("adopted_by");
        if (ab != null) p.setAdoptedBy(UUID.fromString(ab));
        p.setActive(rs.getBoolean("is_active"));
        return p;
    }
}
