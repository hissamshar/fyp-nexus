package com.fyp.dao;

import com.fyp.model.ProjectProposal;
import com.fyp.enums.ProposalStatus;
import com.fyp.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class ProjectProposalDAO {

    public Optional<ProjectProposal> findById(UUID proposalId) throws SQLException {
        String sql = "SELECT * FROM fyp.project_proposals WHERE proposal_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, proposalId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<ProjectProposal> findByStudentId(UUID studentId) throws SQLException {
        List<ProjectProposal> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.project_proposals WHERE student_id = ?::uuid ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<ProjectProposal> findBySupervisorId(UUID supervisorId) throws SQLException {
        List<ProjectProposal> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.project_proposals WHERE supervisor_id = ?::uuid ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, supervisorId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<ProjectProposal> findByStatus(ProposalStatus status) throws SQLException {
        List<ProjectProposal> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.project_proposals WHERE status = ? ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<ProjectProposal> findAll() throws SQLException {
        List<ProjectProposal> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.project_proposals ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public UUID insert(ProjectProposal p) throws SQLException {
        String sql = """
            INSERT INTO fyp.project_proposals
              (title, abstract, objectives, methodology, expected_outcomes,
               description, submission_date, status, student_id, supervisor_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::uuid, ?::uuid)
            RETURNING proposal_id
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getTitle());
            ps.setString(2, p.getAbstract());
            ps.setString(3, p.getObjectives());
            ps.setString(4, p.getMethodology());
            ps.setString(5, p.getExpectedOutcome());
            ps.setString(6, p.getDescription());
            ps.setDate(7, p.getSubmissionDate() != null ? java.sql.Date.valueOf(p.getSubmissionDate()) : null);
            ps.setString(8, p.getStatus().name());
            ps.setString(9, p.getStudentId().toString());
            ps.setString(10, p.getSupervisorId() != null ? p.getSupervisorId().toString() : null);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("proposal_id"));
            }
        }
        throw new SQLException("INSERT into fyp.project_proposals returned no ID.");
    }

    public void updateStatus(UUID proposalId, ProposalStatus status, String rejectionComment) throws SQLException {
        String sql = "UPDATE fyp.project_proposals SET status = ?, rejection_comment = ? " +
                     "WHERE proposal_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, rejectionComment);
            ps.setString(3, proposalId.toString());
            ps.executeUpdate();
        }
    }

    public void update(ProjectProposal p) throws SQLException {
        String sql = """
            UPDATE fyp.project_proposals
            SET title = ?, abstract = ?, objectives = ?, methodology = ?,
                expected_outcomes = ?, description = ?, status = ?, supervisor_id = ?::uuid
            WHERE proposal_id = ?::uuid
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getTitle());
            ps.setString(2, p.getAbstract());
            ps.setString(3, p.getObjectives());
            ps.setString(4, p.getMethodology());
            ps.setString(5, p.getExpectedOutcome());
            ps.setString(6, p.getDescription());
            ps.setString(7, p.getStatus().name());
            ps.setString(8, p.getSupervisorId() != null ? p.getSupervisorId().toString() : null);
            ps.setString(9, p.getProposalId().toString());
            ps.executeUpdate();
        }
    }

    private ProjectProposal mapRow(ResultSet rs) throws SQLException {
        ProjectProposal p = new ProjectProposal();
        p.setProposalId(UUID.fromString(rs.getString("proposal_id")));
        p.setTitle(rs.getString("title"));
        p.setAbstract(rs.getString("abstract"));
        p.setObjectives(rs.getString("objectives"));
        p.setMethodology(rs.getString("methodology"));
        p.setExpectedOutcome(rs.getString("expected_outcomes"));
        p.setDescription(rs.getString("description"));
        java.sql.Date d = rs.getDate("submission_date");
        if (d != null) p.setSubmissionDate(d.toLocalDate());
        p.setStatus(ProposalStatus.valueOf(rs.getString("status")));
        p.setRejectionComment(rs.getString("rejection_comment"));
        p.setStudentId(UUID.fromString(rs.getString("student_id")));
        String supId = rs.getString("supervisor_id");
        if (supId != null) p.setSupervisorId(UUID.fromString(supId));
        return p;
    }
}
