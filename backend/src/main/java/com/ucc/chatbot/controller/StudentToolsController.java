package com.ucc.chatbot.controller;

import com.ucc.chatbot.model.DemoAris;
import com.ucc.chatbot.model.DemoLms;
import com.ucc.chatbot.service.DemoAcademicService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentToolsController {

    private final DemoAcademicService academicService;

    public StudentToolsController(DemoAcademicService academicService) {
        this.academicService = academicService;
    }

    private String currentStudentId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
            return auth.getName();
        }
        return null;
    }

    @GetMapping("/courses")
    public ResponseEntity<List<DemoAris>> getCourses() {
        String sid = currentStudentId();
        if (sid == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(academicService.getStudentCourses(sid));
    }

    @GetMapping("/fees")
    public ResponseEntity<DemoAris> getFees() {
        String sid = currentStudentId();
        if (sid == null) return ResponseEntity.status(401).build();
        DemoAris data = academicService.getStudentFees(sid);
        if (data == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/timetable")
    public ResponseEntity<DemoAris> getTimetable() {
        String sid = currentStudentId();
        if (sid == null) return ResponseEntity.status(401).build();
        DemoAris data = academicService.getStudentTimetable(sid);
        if (data == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/exams")
    public ResponseEntity<DemoAris> getExams() {
        String sid = currentStudentId();
        if (sid == null) return ResponseEntity.status(401).build();
        DemoAris data = academicService.getStudentExams(sid);
        if (data == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/teacher-classroom")
    public ResponseEntity<DemoAris> getTeacherAndClassroom() {
        String sid = currentStudentId();
        if (sid == null) return ResponseEntity.status(401).build();
        DemoAris data = academicService.getTeacherAndClassroom(sid);
        if (data == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/profile")
    public ResponseEntity<DemoAris> getProfile() {
        String sid = currentStudentId();
        if (sid == null) return ResponseEntity.status(401).build();
        DemoAris data = academicService.getFullProfile(sid);
        if (data == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/lms/courses")
    public ResponseEntity<List<DemoLms>> getLmsCourses() {
        String sid = currentStudentId();
        if (sid == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(academicService.getLmsCourses(sid));
    }

    @GetMapping("/lms/course")
    public ResponseEntity<DemoLms> getLmsCourse(@RequestParam String code) {
        String sid = currentStudentId();
        if (sid == null) return ResponseEntity.status(401).build();
        DemoLms data = academicService.getLmsCourse(sid, code);
        if (data == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/lms/materials")
    public ResponseEntity<DemoLms> getMaterials(@RequestParam String code) {
        String sid = currentStudentId();
        if (sid == null) return ResponseEntity.status(401).build();
        DemoLms data = academicService.getCourseMaterials(sid, code);
        if (data == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/lms/assignments")
    public ResponseEntity<DemoLms> getAssignments(@RequestParam String code) {
        String sid = currentStudentId();
        if (sid == null) return ResponseEntity.status(401).build();
        DemoLms data = academicService.getAssignments(sid, code);
        if (data == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/lms/quizzes")
    public ResponseEntity<DemoLms> getQuizzes(@RequestParam String code) {
        String sid = currentStudentId();
        if (sid == null) return ResponseEntity.status(401).build();
        DemoLms data = academicService.getQuizzes(sid, code);
        if (data == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(data);
    }
}