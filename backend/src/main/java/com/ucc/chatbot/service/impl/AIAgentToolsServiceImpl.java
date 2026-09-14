package com.ucc.chatbot.service.impl;

import com.ucc.chatbot.dto.ChatResponse;
import com.ucc.chatbot.model.DemoAris;
import com.ucc.chatbot.model.DemoLms;
import com.ucc.chatbot.repository.DemoArisRepository;
import com.ucc.chatbot.repository.DemoLmsRepository;
import com.ucc.chatbot.service.AIAgentTools;
import com.ucc.chatbot.service.HybridRetrievalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AIAgentToolsServiceImpl implements AIAgentTools {

    private final HybridRetrievalService retrievalService;
    private final DemoArisRepository arisRepository;
    private final DemoLmsRepository lmsRepository;

    @Autowired
    public AIAgentToolsServiceImpl(HybridRetrievalService retrievalService,
                                   DemoArisRepository arisRepository,
                                   DemoLmsRepository lmsRepository) {
        this.retrievalService = retrievalService;
        this.arisRepository = arisRepository;
        this.lmsRepository = lmsRepository;
    }

    @Override
    public ChatResponse searchUccKnowledge(String query, String language) {
        com.ucc.chatbot.model.KnowledgeDocument doc = retrievalService.findBest(query);
        if (doc == null) {
            return ChatResponse.builder()
                    .answer("sw".equals(language)
                            ? "Samahani, sikuweza kuthibitisha taarifa hii kutoka kwenye mfumo wa UCC."
                            : "I couldn't verify this information from the UCC knowledge base.")
                    .language(language)
                    .confidence(0.0)
                    .escalationRequired(true)
                    .build();
        }
        return ChatResponse.builder()
                .answer(doc.getContent())
                .language(language)
                .sources(List.of(Map.of("title", doc.getTitle(), "url", "https://ucc.co.tz/")))
                .confidence(0.85)
                .escalationRequired(false)
                .build();
    }

    @Override
    public ChatResponse getStudentCourses(String studentId, String language) {
        return getStudentProfile(studentId, language, "courses");
    }

    @Override
    public ChatResponse getStudentFees(String studentId, String language) {
        return getStudentProfile(studentId, language, "fees");
    }

    @Override
    public ChatResponse getStudentTimetable(String studentId, String language) {
        return getStudentProfile(studentId, language, "timetable");
    }

    @Override
    public ChatResponse getStudentExams(String studentId, String language) {
        return getStudentProfile(studentId, language, "exams");
    }

    @Override
    public ChatResponse getCourseTeacher(String courseCode, String studentId, String language) {
        return getStudentProfile(studentId, language, "teacher");
    }

    @Override
    public ChatResponse getClassroom(String courseCode, String studentId, String language) {
        return getStudentProfile(studentId, language, "classroom");
    }

    @Override
    public ChatResponse checkCourseAvailability(String courseCode, String studentId, String language) {
        return getStudentProfile(studentId, language, "availability");
    }

    @Override
    public ChatResponse searchLmsCourse(String query, String studentId, String language) {
        List<DemoLms> courses = lmsRepository.findByStudentId(studentId);
        if (courses.isEmpty()) {
            return notFound(language);
        }
        return ChatResponse.builder()
                .answer(courses.stream()
                        .map(c -> (c.getCourseCode() != null ? c.getCourseCode() : "") + " - " +
                                  (c.getCourseName() != null ? c.getCourseName() : ""))
                        .reduce((a, b) -> a + "\n" + b).orElse(""))
                .language(language)
                .confidence(0.8)
                .escalationRequired(false)
                .build();
    }

    @Override
    public ChatResponse getCourseMaterials(String courseCode, String studentId, String language) {
        DemoLms course = lmsRepository.findByStudentIdAndCourseCode(studentId, courseCode)
                .stream().findFirst().orElse(null);
        if (course == null) return notFound(language);
        return ChatResponse.builder()
                .answer(course.getMaterials() != null ? course.getMaterials() : "No materials available.")
                .language(language)
                .confidence(0.85)
                .escalationRequired(false)
                .build();
    }

    @Override
    public ChatResponse getAssignments(String courseCode, String studentId, String language) {
        DemoLms course = lmsRepository.findByStudentIdAndCourseCode(studentId, courseCode)
                .stream().findFirst().orElse(null);
        if (course == null) return notFound(language);
        return ChatResponse.builder()
                .answer(course.getAssignments() != null ? course.getAssignments() : "No assignments available.")
                .language(language)
                .confidence(0.85)
                .escalationRequired(false)
                .build();
    }

    @Override
    public ChatResponse getProgrammeGuidance(String query, String language) {
        com.ucc.chatbot.model.KnowledgeDocument doc = retrievalService.findBest(query);
        if (doc == null) {
            return ChatResponse.builder()
                    .answer("sw".equals(language)
                            ? "Ninaweza kukupa maelekezo ya jumla kuhusu programu za UCC, lakini kwa maelezo ya kina, wasiliana na UCC."
                            : "I can give general guidance about UCC programmes, but for specific advice, contact UCC directly.")
                    .language(language)
                    .confidence(0.6)
                    .escalationRequired(false)
                    .build();
        }
        return ChatResponse.builder()
                .answer(doc.getContent())
                .language(language)
                .sources(List.of(Map.of("title", doc.getTitle(), "url", "https://ucc.co.tz/")))
                .confidence(0.8)
                .escalationRequired(false)
                .build();
    }

    private ChatResponse getStudentProfile(String studentId, String language, String field) {
        DemoAris profile = arisRepository.findByStudentId(studentId).stream().findFirst().orElse(null);
        if (profile == null) return notFound(language);
        String value;
        switch (field) {
            case "courses": value = profile.getCourses(); break;
            case "fees": value = profile.getFees(); break;
            case "timetable": value = profile.getTimetable(); break;
            case "exams": value = profile.getExams(); break;
            case "teacher": value = profile.getTeacherAssignments(); break;
            case "classroom": value = profile.getClassroomAssignments(); break;
            case "availability": value = profile.getCourses(); break;
            default: value = null;
        }
        if (value == null || value.isBlank()) return notFound(language);
        return ChatResponse.builder()
                .answer(value)
                .language(language)
                .confidence(0.9)
                .escalationRequired(false)
                .build();
    }

    private ChatResponse notFound(String language) {
        return ChatResponse.builder()
                .answer("sw".equals(language)
                        ? "Hakuna taarifa inayopatikana kwa mwanafunzi huyu."
                        : "No information found for this student.")
                .language(language)
                .confidence(0.0)
                .escalationRequired(true)
                .build();
    }
}