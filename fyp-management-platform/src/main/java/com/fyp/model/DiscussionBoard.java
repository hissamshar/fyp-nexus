package com.fyp.model;

import java.util.List;
import java.util.UUID;

public class DiscussionBoard {
    private UUID boardId;
    private boolean isPrivate;
    private UUID projectId;

    public DiscussionBoard() {}

    public DiscussionBoard(UUID boardId, boolean isPrivate, UUID projectId) {
        this.boardId   = boardId;
        this.isPrivate = isPrivate;
        this.projectId = projectId;
    }

    public DiscussionThread createThread(String topic) {
        DiscussionThread thread = new DiscussionThread();
        thread.setTopic(topic);
        thread.setBoardId(this.boardId);
        return thread;
    }

    public List<Post> searchPosts(String query) {
        // Delegated to DiscussionService
        return List.of();
    }

    public UUID getBoardId()    { return boardId; }
    public boolean isPrivate()  { return isPrivate; }
    public UUID getProjectId()  { return projectId; }

    public void setBoardId(UUID boardId)   { this.boardId = boardId; }
    public void setPrivate(boolean priv)   { isPrivate = priv; }
    public void setProjectId(UUID projectId){ this.projectId = projectId; }
}
