package com.ucc.chatbot.service;

import com.ucc.chatbot.dto.ChatResponse;

/**
 * AI Agent tools — controlled, authorized, auditable.
 * Each tool is a discrete capability the agent can invoke.
 */
public interface AIAgentTools {

    /** searchUccKnowledge() — public UCC knowledge base search */
    ChatResponse searchUccKnowledge(String query, String language);

    /** getStudentCourses() — authenticated, own record only */
    ChatResponse getStudentCourses(String studentId, String language);

    /** getStudentFees() — authenticated, own record only */
    ChatResponse getStudentFees(String studentId, String language);

    /** getStudentTimetable() — authenticated, own record only */
    ChatResponse getStudentTimetable(String studentId, String language);

    /** getStudentExams() — authenticated, own record only */
    ChatResponse getStudentExams(String studentId, String language);

    /** getCourseTeacher() — public course info or authorized student schedule */
    ChatResponse getCourseTeacher(String courseCode, String studentId, String language);

    /** getClassroom() — public course info or authorized student schedule */
    ChatResponse getClassroom(String courseCode, String studentId, String language);

    /** checkCourseAvailability() — authenticated */
    ChatResponse checkCourseAvailability(String courseCode, String studentId, String language);

    /** searchLmsCourse() — authenticated, authorized course */
    ChatResponse searchLmsCourse(String query, String studentId, String language);

    /** getCourseMaterials() — authenticated, authorized course */
    ChatResponse getCourseMaterials(String courseCode, String studentId, String language);

    /** getAssignments() — authenticated, authorized course */
    ChatResponse getAssignments(String courseCode, String studentId, String language);

    /** getProgrammeGuidance() — public or student context */
    ChatResponse getProgrammeGuidance(String query, String language);
}