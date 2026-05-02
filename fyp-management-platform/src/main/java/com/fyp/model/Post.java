package com.fyp.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Post {
    private UUID postId;
    private String content;
    private LocalDateTime timestamp;
    private UUID threadId;
    private UUID authorId;
    private String attachmentPath;
    private boolean isDeleted;

    public Post() {}

    public Post(UUID postId, String content, LocalDateTime timestamp,
                UUID threadId, UUID authorId, String attachmentPath) {
        this.postId         = postId;
        this.content        = content;
        this.timestamp      = timestamp;
        this.threadId       = threadId;
        this.authorId       = authorId;
        this.attachmentPath = attachmentPath;
        this.isDeleted      = false;
    }

    public void editContent(String newText) {
        this.content = newText;
    }

    public void deletePost() {
        this.isDeleted = true;
        this.content   = "[deleted]";
    }

    public UUID getPostId()          { return postId; }
    public String getContent()       { return content; }
    public LocalDateTime getTimestamp(){ return timestamp; }
    public UUID getThreadId()        { return threadId; }
    public UUID getAuthorId()        { return authorId; }
    public String getAttachmentPath(){ return attachmentPath; }
    public boolean isDeleted()       { return isDeleted; }

    public void setPostId(UUID postId)                   { this.postId = postId; }
    public void setContent(String content)               { this.content = content; }
    public void setTimestamp(LocalDateTime timestamp)    { this.timestamp = timestamp; }
    public void setThreadId(UUID threadId)               { this.threadId = threadId; }
    public void setAuthorId(UUID authorId)               { this.authorId = authorId; }
    public void setAttachmentPath(String attachmentPath) { this.attachmentPath = attachmentPath; }
    public void setDeleted(boolean deleted)              { isDeleted = deleted; }
}
