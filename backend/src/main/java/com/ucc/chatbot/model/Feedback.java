package com.ucc.chatbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
public class Feedback {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "message_id", length = 36)
    private String messageId;

    @Column(name = "conversation_id", length = 36)
    private String conversationId;

    @Column(name = "user_id", length = 36)
    private String userId;

    private Integer rating;

    @Column(length = 10)
    private String thumbs;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "feedback_type", length = 50)
    private String feedbackType = "response";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getThumbs() { return thumbs; }
    public void setThumbs(String thumbs) { this.thumbs = thumbs; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getFeedbackType() { return feedbackType; }
    public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
