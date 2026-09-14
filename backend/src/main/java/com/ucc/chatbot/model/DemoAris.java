package com.ucc.chatbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Demo ARIS — fictional academic record source for the prototype.
 * Demonstrates the integration architecture without assuming live UCC database access.
 */
@Entity
@Table(name = "demo_aris")
public class DemoAris {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String studentName;

    @Column
    private String programme;

    @Column
    private String semester;

    @Column
    private String courses;

    @Column
    private String timetable;

    @Column
    private String fees;

    @Column
    private Double outstandingBalance;

    @Column
    private String exams;

    @Column
    private String teacherAssignments;

    @Column
    private String classroomAssignments;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    public DemoAris() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getProgramme() { return programme; }
    public void setProgramme(String programme) { this.programme = programme; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getCourses() { return courses; }
    public void setCourses(String courses) { this.courses = courses; }
    public String getTimetable() { return timetable; }
    public void setTimetable(String timetable) { this.timetable = timetable; }
    public String getFees() { return fees; }
    public void setFees(String fees) { this.fees = fees; }
    public Double getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(Double outstandingBalance) { this.outstandingBalance = outstandingBalance; }
    public String getExams() { return exams; }
    public void setExams(String exams) { this.exams = exams; }
    public String getTeacherAssignments() { return teacherAssignments; }
    public void setTeacherAssignments(String teacherAssignments) { this.teacherAssignments = teacherAssignments; }
    public String getClassroomAssignments() { return classroomAssignments; }
    public void setClassroomAssignments(String classroomAssignments) { this.classroomAssignments = classroomAssignments; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}