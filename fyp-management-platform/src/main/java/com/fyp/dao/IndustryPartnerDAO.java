package com.fyp.dao;

import com.fyp.model.IndustryPartner;
import com.fyp.util.DBConnection;

import java.sql.*;
import java.util.*;

public class IndustryPartnerDAO {

    public Optional<IndustryPartner> findByUserId(UUID userId) throws SQLException {
        String sql = """
            SELECT u.user_id, u.name, u.email, u.password_hash, u.is_active, u.is_email_verified,
                   ip.partner_id, ip.company_name, ip.contact_email
            FROM fyp.users u
            JOIN fyp.industry_partners ip ON ip.user_id = u.user_id
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

    public List<IndustryPartner> findAll() throws SQLException {
        List<IndustryPartner> list = new ArrayList<>();
        String sql = """
            SELECT u.user_id, u.name, u.email, u.password_hash, u.is_active, u.is_email_verified,
                   ip.partner_id, ip.company_name, ip.contact_email
            FROM fyp.users u
            JOIN fyp.industry_partners ip ON ip.user_id = u.user_id
            WHERE u.is_active = TRUE ORDER BY u.name
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public UUID insert(UUID userId, String companyName, String contactEmail) throws SQLException {
        String sql = "INSERT INTO fyp.industry_partners (user_id, company_name, contact_email) " +
                     "VALUES (?::uuid, ?, ?) RETURNING partner_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setString(2, companyName);
            ps.setString(3, contactEmail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("partner_id"));
            }
        }
        throw new SQLException("INSERT into fyp.industry_partners returned no ID.");
    }

    private IndustryPartner mapRow(ResultSet rs) throws SQLException {
        return new IndustryPartner(
            UUID.fromString(rs.getString("user_id")),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getBoolean("is_active"),
            rs.getBoolean("is_email_verified"),
            UUID.fromString(rs.getString("partner_id")),
            rs.getString("company_name"),
            rs.getString("contact_email")
        );
    }
}
