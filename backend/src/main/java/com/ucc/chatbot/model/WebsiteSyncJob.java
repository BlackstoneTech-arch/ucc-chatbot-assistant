package com.ucc.chatbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "website_sync_jobs")
public class WebsiteSyncJob {
    @Id
    @Column(length = 36)
    private String id;

    @Column(length = 20)
    private String status = "PENDING";

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "pages_scanned")
    private Integer pagesScanned = 0;

    @Column(name = "pages_new")
    private Integer pagesNew = 0;

    @Column(name = "pages_updated")
    private Integer pagesUpdated = 0;

    @Column(name = "pages_unchanged")
    private Integer pagesUnchanged = 0;

    @Column(name = "pages_failed")
    private Integer pagesFailed = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_by", length = 36)
    private String startedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Integer getPagesScanned() { return pagesScanned; }
    public void setPagesScanned(Integer pagesScanned) { this.pagesScanned = pagesScanned; }
    public Integer getPagesNew() { return pagesNew; }
    public void setPagesNew(Integer pagesNew) { this.pagesNew = pagesNew; }
    public Integer getPagesUpdated() { return pagesUpdated; }
    public void setPagesUpdated(Integer pagesUpdated) { this.pagesUpdated = pagesUpdated; }
    public Integer getPagesUnchanged() { return pagesUnchanged; }
    public void setPagesUnchanged(Integer pagesUnchanged) { this.pagesUnchanged = pagesUnchanged; }
    public Integer getPagesFailed() { return pagesFailed; }
    public void setPagesFailed(Integer pagesFailed) { this.pagesFailed = pagesFailed; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getStartedBy() { return startedBy; }
    public void setStartedBy(String startedBy) { this.startedBy = startedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
