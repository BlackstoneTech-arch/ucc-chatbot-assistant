package com.ucc.chatbot.service;

import com.ucc.chatbot.model.DemoAris;
import java.util.List;

public interface DemoArisService {
    List<DemoAris> getStudentCourses(String studentId);
    DemoAris getStudentFees(String studentId);
    DemoAris getStudentTimetable(String studentId);
    DemoAris getStudentExams(String studentId);
    DemoAris getTeacherAndClassroom(String studentId);
    DemoAris getFullProfile(String studentId);
}