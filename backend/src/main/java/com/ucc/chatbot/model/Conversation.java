package com.ucc.chatbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "conversations")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "session_id", unique = true, length = 100)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_programme", length = 50)
    private String lastProgramme;

    @Column(name = "last_concept", length = 100)
    private String lastConcept;

    @Column(name = "last_intent", length = 100)
    private String lastIntent;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Message> messages = new HashSet<>();

    @OneToOne(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Feedback feedback;

    @OneToOne(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Escalation escalation;

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public String getLastProgramme() { return lastProgramme; }
    public void setLastProgramme(String lastProgramme) { this.lastProgramme = lastProgramme; }
    public String getLastConcept() { return lastConcept; }
    public void setLastConcept(String lastConcept) { this.lastConcept = lastConcept; }
    public String getLastIntent() { return lastIntent; }
    public void setLastIntent(String lastIntent) { this.lastIntent = lastIntent; }
    public Set<Message> getMessages() { return messages; }
    public void setMessages(Set<Message> messages) { this.messages = messages; }
    public Feedback getFeedback() { return feedback; }
    public void setFeedback(Feedback feedback) { this.feedback = feedback; }
    public Escalation getEscalation() { return escalation; }
    public void setEscalation(Escalation escalation) { this.escalation = escalation; }
}
