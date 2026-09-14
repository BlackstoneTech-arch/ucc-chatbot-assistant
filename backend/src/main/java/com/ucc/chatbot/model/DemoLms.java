package com.ucc.chatbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Demo LMS — fictional learning management system for the prototype.
 * Demonstrates LMS integration architecture without assuming live UCC LMS access.
 */
@Entity
@Table(name = "demo_lms")
public class DemoLms {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String studentId;

    @Column
    private String courseCode;

    @Column
    private String courseName;

    @Column
    private String instructor;

    @Column
    private String status;

    @Column
    private String materials;

    @Column
    private String assignments;

    @Column
    private String quizzes;

    @Column
    private String announcements;

    @Column
    private String discussionInfo;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    public DemoLms() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getInstructor() { return instructor; }
    public void setInstructor(String instructor) { this.instructor = instructor; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMaterials() { return materials; }
    public void setMaterials(String materials) { this.materials = materials; }
    public String getAssignments() { return assignments; }
    public void setAssignments(String assignments) { this.assignments = assignments; }
    public String getQuizzes() { return quizzes; }
    public void setQuizzes(String quizzes) { this.quizzes = quizzes; }
    public String getAnnouncements() { return announcements; }
    public void setAnnouncements(String announcements) { this.announcements = announcements; }
    public String getDiscussionInfo() { return discussionInfo; }
    public void setDiscussionInfo(String discussionInfo) { this.discussionInfo = discussionInfo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}