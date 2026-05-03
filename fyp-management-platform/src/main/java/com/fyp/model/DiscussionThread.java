package com.fyp.model;

import java.util.UUID;

public class DiscussionThread {
    private UUID threadId;
    private String topic;
    private boolean isLocked;
    private boolean isPinned;
    private UUID boardId;

    public DiscussionThread() {}

    public DiscussionThread(UUID threadId, String topic, boolean isLocked,
                            boolean isPinned, UUID boardId) {
        this.threadId = threadId;
        this.topic    = topic;
        this.isLocked = isLocked;
        this.isPinned = isPinned;
        this.boardId  = boardId;
    }

    public void addPost(String content) {
        // Delegated to DiscussionService
    }

    public void pinThread() {
        this.isPinned = true;
    }

    public UUID getThreadId()  { return threadId; }
    public String getTopic()   { return topic; }
    public boolean isLocked()  { return isLocked; }
    public boolean isPinned()  { return isPinned; }
    public UUID getBoardId()   { return boardId; }

    public void setThreadId(UUID threadId) { this.threadId = threadId; }
    public void setTopic(String topic)     { this.topic = topic; }
    public void setLocked(boolean locked)  { isLocked = locked; }
    public void setPinned(boolean pinned)  { isPinned = pinned; }
    public void setBoardId(UUID boardId)   { this.boardId = boardId; }

    // Alias for UI binding
    public String getTitle()           { return topic; }
    public void setTitle(String title) { this.topic = title; }
}
