package com.ucc.chatbot.service;

import com.ucc.chatbot.model.DemoLms;
import java.util.List;

public interface DemoLmsService {
    List<DemoLms> getLmsCourses(String studentId);
    DemoLms getLmsCourse(String studentId, String courseCode);
    DemoLms getCourseMaterials(String studentId, String courseCode);
    DemoLms getAssignments(String studentId, String courseCode);
    DemoLms getQuizzes(String studentId, String courseCode);
}