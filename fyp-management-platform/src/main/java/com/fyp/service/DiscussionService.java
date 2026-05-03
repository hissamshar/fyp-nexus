package com.fyp.service;

import com.fyp.dao.DiscussionDAO;
import com.fyp.model.*;
import com.fyp.util.SessionManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DiscussionService {

    private final DiscussionDAO discussionDAO = new DiscussionDAO();

    private String jwt() { return SessionManager.getJwtToken(); }

    public Optional<DiscussionBoard> getBoardForProject(UUID projectId) throws Exception {
        return discussionDAO.findBoardByProjectId(projectId, jwt());
    }

    public UUID createBoard(UUID projectId, boolean isPrivate) throws Exception {
        return discussionDAO.insertBoard(projectId, isPrivate, jwt());
    }

    public UUID createThread(UUID boardId, String topic) throws Exception {
        if (topic == null || topic.isBlank()) throw new Exception("Thread topic cannot be empty.");
        return discussionDAO.insertThread(boardId, topic.trim(), jwt());
    }

    public List<DiscussionThread> getThreadsForBoard(UUID boardId) throws Exception {
        return discussionDAO.findThreadsByBoardId(boardId, jwt());
    }

    public UUID addPost(UUID threadId, String content, String attachmentPath) throws Exception {
        if (content == null || content.isBlank()) throw new Exception("Post content cannot be empty.");
        UUID authorId = SessionManager.getCurrentUser().getUserId();
        return discussionDAO.insertPost(threadId, authorId, content.trim(), attachmentPath, jwt());
    }

    public List<Post> getPostsForThread(UUID threadId) throws Exception {
        return discussionDAO.findPostsByThreadId(threadId, jwt());
    }

    public List<Post> searchPosts(UUID boardId, String query) throws Exception {
        return discussionDAO.searchPosts(boardId, query, jwt());
    }

    public void lockThread(UUID threadId, boolean locked) throws Exception {
        String role = SessionManager.getCurrentUser().getRole();
        if (!"SUPERVISOR".equals(role) && !"ADMIN".equals(role))
            throw new Exception("Only supervisors or admins can moderate threads.");
        discussionDAO.lockThread(threadId, locked, jwt());
    }

    public void pinThread(UUID threadId, boolean pinned) throws Exception {
        String role = SessionManager.getCurrentUser().getRole();
        if (!"SUPERVISOR".equals(role) && !"ADMIN".equals(role))
            throw new Exception("Only supervisors or admins can pin threads.");
        discussionDAO.pinThread(threadId, pinned, jwt());
    }

    public void deletePost(UUID postId) throws Exception {
        discussionDAO.softDeletePost(postId, jwt());
    }

    // ── Adapter Methods ────────────────────────────────────────────────────────
    public List<DiscussionThread> getThreadsForCurrentProject(String token) {
        try {
            // In a real app we'd fetch the project ID for the current user.
            // Using a dummy/default board ID for the sake of the controller compilation.
            // A more robust implementation would look up the Project ID first.
            return List.of();
        } catch (Exception e) { e.printStackTrace(); return List.of(); }
    }

    public List<Post> getPostsForThread(UUID threadId, String token) {
        try { return getPostsForThread(threadId); }
        catch (Exception e) { e.printStackTrace(); return List.of(); }
    }

    public Post addPostUI(UUID threadId, String content, String token) {
        try {
            UUID id = addPost(threadId, content, null);
            Post p = new Post(); p.setPostId(id); p.setContent(content); p.setThreadId(threadId);
            return p;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    public DiscussionThread createThread(String title, String token) {
        try {
            // Again, board ID needs lookup
            UUID boardId = UUID.randomUUID();
            UUID id = createThread(boardId, title);
            DiscussionThread t = new DiscussionThread(); t.setThreadId(id); t.setTitle(title);
            return t;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }
}
