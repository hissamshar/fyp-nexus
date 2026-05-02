package com.fyp.service;

import com.fyp.dao.DiscussionDAO;
import com.fyp.model.*;
import com.fyp.util.SessionManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DiscussionService {

    private final DiscussionDAO discussionDAO = new DiscussionDAO();

    public Optional<DiscussionBoard> getBoardForProject(UUID projectId) throws Exception {
        return discussionDAO.findBoardByProjectId(projectId);
    }

    public UUID createBoard(UUID projectId, boolean isPrivate) throws Exception {
        return discussionDAO.insertBoard(projectId, isPrivate);
    }

    public UUID createThread(UUID boardId, String topic) throws Exception {
        if (topic == null || topic.isBlank()) throw new Exception("Thread topic cannot be empty.");
        return discussionDAO.insertThread(boardId, topic.trim());
    }

    public List<DiscussionThread> getThreadsForBoard(UUID boardId) throws Exception {
        return discussionDAO.findThreadsByBoardId(boardId);
    }

    public UUID addPost(UUID threadId, String content, String attachmentPath) throws Exception {
        if (content == null || content.isBlank()) throw new Exception("Post content cannot be empty.");
        UUID authorId = SessionManager.getCurrentUser().getUserId();
        return discussionDAO.insertPost(threadId, authorId, content.trim(), attachmentPath);
    }

    public List<Post> getPostsForThread(UUID threadId) throws Exception {
        return discussionDAO.findPostsByThreadId(threadId);
    }

    public List<Post> searchPosts(UUID boardId, String query) throws Exception {
        return discussionDAO.searchPosts(boardId, query);
    }

    public void lockThread(UUID threadId, boolean locked) throws Exception {
        String role = SessionManager.getCurrentUser().getRole();
        if (!"SUPERVISOR".equals(role) && !"ADMIN".equals(role))
            throw new Exception("Only supervisors or admins can moderate threads.");
        discussionDAO.lockThread(threadId, locked);
    }

    public void pinThread(UUID threadId, boolean pinned) throws Exception {
        String role = SessionManager.getCurrentUser().getRole();
        if (!"SUPERVISOR".equals(role) && !"ADMIN".equals(role))
            throw new Exception("Only supervisors or admins can pin threads.");
        discussionDAO.pinThread(threadId, pinned);
    }

    public void deletePost(UUID postId) throws Exception {
        discussionDAO.softDeletePost(postId);
    }
}
