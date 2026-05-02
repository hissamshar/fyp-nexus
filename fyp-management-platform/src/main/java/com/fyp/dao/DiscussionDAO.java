package com.fyp.dao;

import com.fyp.model.DiscussionBoard;
import com.fyp.model.DiscussionThread;
import com.fyp.model.Post;
import com.fyp.util.DBConnection;

import java.sql.*;
import java.util.*;

public class DiscussionDAO {

    // ── Board ──────────────────────────────────────────────────────────────────

    public Optional<DiscussionBoard> findBoardByProjectId(UUID projectId) throws SQLException {
        String sql = "SELECT * FROM fyp.discussion_boards WHERE project_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DiscussionBoard b = new DiscussionBoard();
                    b.setBoardId(UUID.fromString(rs.getString("board_id")));
                    b.setPrivate(rs.getBoolean("is_private"));
                    b.setProjectId(projectId);
                    return Optional.of(b);
                }
            }
        }
        return Optional.empty();
    }

    public UUID insertBoard(UUID projectId, boolean isPrivate) throws SQLException {
        String sql = "INSERT INTO fyp.discussion_boards (project_id, is_private) VALUES (?::uuid, ?) RETURNING board_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, projectId.toString());
            ps.setBoolean(2, isPrivate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("board_id"));
            }
        }
        throw new SQLException("INSERT into fyp.discussion_boards returned no ID.");
    }

    // ── Threads ────────────────────────────────────────────────────────────────

    public List<DiscussionThread> findThreadsByBoardId(UUID boardId) throws SQLException {
        List<DiscussionThread> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.discussion_threads WHERE board_id = ?::uuid ORDER BY is_pinned DESC, created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, boardId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DiscussionThread t = new DiscussionThread();
                    t.setThreadId(UUID.fromString(rs.getString("thread_id")));
                    t.setTopic(rs.getString("topic"));
                    t.setLocked(rs.getBoolean("is_locked"));
                    t.setPinned(rs.getBoolean("is_pinned"));
                    t.setBoardId(boardId);
                    list.add(t);
                }
            }
        }
        return list;
    }

    public UUID insertThread(UUID boardId, String topic) throws SQLException {
        String sql = "INSERT INTO fyp.discussion_threads (board_id, topic) VALUES (?::uuid, ?) RETURNING thread_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, boardId.toString());
            ps.setString(2, topic);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("thread_id"));
            }
        }
        throw new SQLException("INSERT into fyp.discussion_threads returned no ID.");
    }

    public void lockThread(UUID threadId, boolean locked) throws SQLException {
        String sql = "UPDATE fyp.discussion_threads SET is_locked = ? WHERE thread_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, locked);
            ps.setString(2, threadId.toString());
            ps.executeUpdate();
        }
    }

    public void pinThread(UUID threadId, boolean pinned) throws SQLException {
        String sql = "UPDATE fyp.discussion_threads SET is_pinned = ? WHERE thread_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, pinned);
            ps.setString(2, threadId.toString());
            ps.executeUpdate();
        }
    }

    // ── Posts ──────────────────────────────────────────────────────────────────

    public List<Post> findPostsByThreadId(UUID threadId) throws SQLException {
        List<Post> list = new ArrayList<>();
        String sql = "SELECT * FROM fyp.posts WHERE thread_id = ?::uuid AND is_deleted = FALSE ORDER BY timestamp ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, threadId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapPost(rs));
            }
        }
        return list;
    }

    public List<Post> searchPosts(UUID boardId, String query) throws SQLException {
        List<Post> list = new ArrayList<>();
        String sql = """
            SELECT p.* FROM fyp.posts p
            JOIN fyp.discussion_threads t ON t.thread_id = p.thread_id
            WHERE t.board_id = ?::uuid AND p.is_deleted = FALSE
              AND LOWER(p.content) LIKE LOWER(?)
            ORDER BY p.timestamp DESC
            """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, boardId.toString());
            ps.setString(2, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapPost(rs));
            }
        }
        return list;
    }

    public UUID insertPost(UUID threadId, UUID authorId, String content, String attachmentPath) throws SQLException {
        String sql = "INSERT INTO fyp.posts (thread_id, author_id, content, attachment_path) " +
                     "VALUES (?::uuid, ?::uuid, ?, ?) RETURNING post_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, threadId.toString());
            ps.setString(2, authorId.toString());
            ps.setString(3, content);
            ps.setString(4, attachmentPath);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("post_id"));
            }
        }
        throw new SQLException("INSERT into fyp.posts returned no ID.");
    }

    public void softDeletePost(UUID postId) throws SQLException {
        String sql = "UPDATE fyp.posts SET is_deleted = TRUE, content = '[deleted]' WHERE post_id = ?::uuid";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, postId.toString());
            ps.executeUpdate();
        }
    }

    private Post mapPost(ResultSet rs) throws SQLException {
        Post p = new Post();
        p.setPostId(UUID.fromString(rs.getString("post_id")));
        p.setContent(rs.getString("content"));
        p.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
        p.setThreadId(UUID.fromString(rs.getString("thread_id")));
        p.setAuthorId(UUID.fromString(rs.getString("author_id")));
        p.setAttachmentPath(rs.getString("attachment_path"));
        p.setDeleted(rs.getBoolean("is_deleted"));
        return p;
    }
}
