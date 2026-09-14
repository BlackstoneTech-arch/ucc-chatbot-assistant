package com.ucc.chatbot.service;

import com.ucc.chatbot.model.DemoAris;
import com.ucc.chatbot.model.DemoLms;

import java.util.List;

public interface DemoArisService {
    List<DemoAris> getStudentCourses(String studentId);
    DemoAris getStudentFees(String studentId);
    DemoAris getStudentTimetable(String studentId);
    DemoAris getStudentExams(String studentId);
    DemoAris getTeacherAndClassroom(String studentId);
    DemoAris getFullProfile(String studentId);
}

public interface DemoLmsService {
    List<DemoLms> getLmsCourses(String studentId);
    DemoLms getLmsCourse(String studentId, String courseCode);
    DemoLms getCourseMaterials(String studentId, String courseCode);
    DemoLms getAssignments(String studentId, String courseCode);
    DemoLms getQuizzes(String studentId, String courseCode);
}