package com.fyp.service;

import com.fyp.dao.DiscussionDAO;
import com.fyp.dao.ProjectDAO;
import com.fyp.model.*;
import com.fyp.util.SessionManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DiscussionService {

    private final DiscussionDAO discussionDAO = new DiscussionDAO();
    private final ProjectDAO projectDAO = new ProjectDAO();

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

    // ── Adapter Methods ─────────────────────────────────────────────────────────

    /**
     * Gets all threads for the current user's active project.
     * Resolves project → board → threads.
     */
    public List<DiscussionThread> getThreadsForCurrentProject(String token) {
        try {
            User user = SessionManager.getCurrentUser();
            Optional<UUID> boardId = resolveBoardId(user, token);
            if (boardId.isEmpty()) return List.of();
            return discussionDAO.findThreadsByBoardId(boardId.get(), token);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public List<Post> getPostsForThread(UUID threadId, String token) {
        try { return getPostsForThread(threadId); }
        catch (Exception e) { e.printStackTrace(); return List.of(); }
    }

    public Post addPostUI(UUID threadId, String content, String token) {
        try {
            UUID id = addPost(threadId, content, null);
            Post p = new Post();
            p.setPostId(id);
            p.setContent(content);
            p.setThreadId(threadId);
            p.setAuthorId(SessionManager.getCurrentUser().getUserId());
            return p;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a new thread in the current user's active project board.
     * Resolves project → board, then inserts the thread.
     */
    public DiscussionThread createThread(String title, String token) {
        try {
            User user = SessionManager.getCurrentUser();
            Optional<UUID> boardIdOpt = resolveBoardId(user, token);
            UUID boardId;
            if (boardIdOpt.isPresent()) {
                boardId = boardIdOpt.get();
            } else {
                // No board yet — try to create one from the user's project
                Optional<UUID> projectId = resolveProjectId(user, token);
                if (projectId.isEmpty()) return null;
                boardId = discussionDAO.insertBoard(projectId.get(), false, token);
            }
            UUID id = createThread(boardId, title);
            DiscussionThread t = new DiscussionThread();
            t.setThreadId(id);
            t.setTitle(title);
            t.setBoardId(boardId);
            return t;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private Optional<UUID> resolveProjectId(User user, String token) {
        try {
            Optional<com.fyp.model.Project> project =
                projectDAO.findByStudentUserId(user.getUserId(), token);
            return project.map(com.fyp.model.Project::getProjectId);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Optional<UUID> resolveBoardId(User user, String token) {
        try {
            Optional<UUID> projectId = resolveProjectId(user, token);
            if (projectId.isEmpty()) return Optional.empty();
            Optional<DiscussionBoard> board =
                discussionDAO.findBoardByProjectId(projectId.get(), token);
            return board.map(DiscussionBoard::getBoardId);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
