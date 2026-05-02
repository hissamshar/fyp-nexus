package com.fyp.dao;

import com.fyp.model.MeetingRequest;
import com.fyp.enums.MeetingStatus;
import com.fyp.util.DBConnection;

import java.sql.*;
import java.util.*;

public class MeetingRequestDAO {

    public List<MeetingRequest> findByStudentId(UUID studentId) throws SQLException {
        List<MeetingRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.meeting_requests WHERE student_id = ?::uuid ORDER BY proposed_time DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<MeetingRequest> findBySupervisorId(UUID supervisorId) throws SQLException {
        List<MeetingRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.meeting_requests WHERE supervisor_id = ?::uuid ORDER BY proposed_time DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, supervisorId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public UUID insert(MeetingRequest m) throws SQLException {
        String sql = "INSERT INTO fyp.meeting_requests (proposed_time, location, agenda, status, student_id, supervisor_id) " +
                     "VALUES (?, ?, ?, ?, ?::uuid, ?::uuid) RETURNING request_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, m.getProposedTime());
            ps.setString(2, m.getLocation());
            ps.setString(3, m.getAgenda());
            ps.setString(4, MeetingStatus.REQUESTED.name());
            ps.setString(5, m.getStudentId().toString());
            ps.setString(6, m.getSupervisorId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("request_id"));
            }
        }
        throw new SQLException("INSERT into fyp.meeting_requests returned no ID.");
    }

    public void updateStatus(UUID requestId, MeetingStatus status) throws SQLException {
        String sql = "UPDATE fyp.meeting_requests SET status = ? WHERE request_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, requestId.toString());
            ps.executeUpdate();
        }
    }

    public void reschedule(UUID requestId, java.time.LocalDateTime counterTime) throws SQLException {
        String sql = "UPDATE fyp.meeting_requests SET status = 'RESCHEDULED', counter_time = ? " +
                     "WHERE request_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, counterTime);
            ps.setString(2, requestId.toString());
            ps.executeUpdate();
        }
    }

    private MeetingRequest mapRow(ResultSet rs) throws SQLException {
        MeetingRequest m = new MeetingRequest();
        m.setRequestId(UUID.fromString(rs.getString("request_id")));
        m.setProposedTime(rs.getTimestamp("proposed_time").toLocalDateTime());
        m.setLocation(rs.getString("location"));
        m.setAgenda(rs.getString("agenda"));
        m.setStatus(MeetingStatus.valueOf(rs.getString("status")));
        m.setStudentId(UUID.fromString(rs.getString("student_id")));
        m.setSupervisorId(UUID.fromString(rs.getString("supervisor_id")));
        Timestamp ct = rs.getTimestamp("counter_time");
        if (ct != null) m.setCounterTime(ct.toLocalDateTime());
        return m;
    }
}
