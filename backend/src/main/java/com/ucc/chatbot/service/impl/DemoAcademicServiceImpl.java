package com.ucc.chatbot.service.impl;

import com.ucc.chatbot.model.DemoAris;
import com.ucc.chatbot.model.DemoLms;
import com.ucc.chatbot.repository.DemoArisRepository;
import com.ucc.chatbot.repository.DemoLmsRepository;
import com.ucc.chatbot.service.DemoAcademicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DemoAcademicServiceImpl implements DemoAcademicService {

    private final DemoArisRepository arisRepository;
    private final DemoLmsRepository lmsRepository;

    @Autowired
    public DemoAcademicServiceImpl(DemoArisRepository arisRepository, DemoLmsRepository lmsRepository) {
        this.arisRepository = arisRepository;
        this.lmsRepository = lmsRepository;
    }

    @Override
    public List<DemoAris> getStudentCourses(String studentId) {
        return arisRepository.findByStudentId(studentId);
    }

    @Override
    public DemoAris getStudentFees(String studentId) {
        return arisRepository.findByStudentId(studentId).stream().findFirst().orElse(null);
    }

    @Override
    public DemoAris getStudentTimetable(String studentId) {
        return arisRepository.findByStudentId(studentId).stream().findFirst().orElse(null);
    }

    @Override
    public DemoAris getStudentExams(String studentId) {
        return arisRepository.findByStudentId(studentId).stream().findFirst().orElse(null);
    }

    @Override
    public DemoAris getTeacherAndClassroom(String studentId) {
        return arisRepository.findByStudentId(studentId).stream().findFirst().orElse(null);
    }

    @Override
    public DemoAris getFullProfile(String studentId) {
        return arisRepository.findByStudentId(studentId).stream().findFirst().orElse(null);
    }
}

@Service
class DemoLmsServiceImpl implements DemoLmsService {

    private final DemoLmsRepository lmsRepository;

    @Autowired
    public DemoLmsServiceImpl(DemoLmsRepository lmsRepository) {
        this.lmsRepository = lmsRepository;
    }

    @Override
    public List<DemoLms> getLmsCourses(String studentId) {
        return lmsRepository.findByStudentId(studentId);
    }

    @Override
    public DemoLms getLmsCourse(String studentId, String courseCode) {
        return lmsRepository.findByStudentIdAndCourseCode(studentId, courseCode).stream().findFirst().orElse(null);
    }

    @Override
    public DemoLms getCourseMaterials(String studentId, String courseCode) {
        return getLmsCourse(studentId, courseCode);
    }

    @Override
    public DemoLms getAssignments(String studentId, String courseCode) {
        return getLmsCourse(studentId, courseCode);
    }

    @Override
    public DemoLms getQuizzes(String studentId, String courseCode) {
        return getLmsCourse(studentId, courseCode);
    }
}