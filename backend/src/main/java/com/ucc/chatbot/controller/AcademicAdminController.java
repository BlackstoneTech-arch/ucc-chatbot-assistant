package com.ucc.chatbot.controller;

import com.ucc.chatbot.model.*;
import com.ucc.chatbot.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/academic")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:5500", "https://uccchatbot.netlify.app", "https://agent-6a87a4d1bce5537b6d8d53a5--uccchatbot.netlify.app", "https://ucc-chatbot-assistant.blackstone-tech02.workers.dev"})
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class AcademicAdminController {

    private final CourseRepository courseRepository;
    private final ServiceRepository serviceRepository;
    private final DemoArisRepository arisRepository;
    private final DemoLmsRepository lmsRepository;
    private final AuditLogRepository auditLogRepository;

    public AcademicAdminController(CourseRepository courseRepository, ServiceRepository serviceRepository,
                                   DemoArisRepository arisRepository, DemoLmsRepository lmsRepository,
                                   AuditLogRepository auditLogRepository) {
        this.courseRepository = courseRepository;
        this.serviceRepository = serviceRepository;
        this.arisRepository = arisRepository;
        this.lmsRepository = lmsRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // --- Courses ---
    @GetMapping("/courses")
    public ResponseEntity<List<Course>> listCourses() {
        return ResponseEntity.ok(courseRepository.findAll());
    }

    @PostMapping("/courses")
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        return ResponseEntity.ok(courseRepository.save(course));
    }

    @PutMapping("/courses/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable String id, @RequestBody Course course) {
        Course existing = courseRepository.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        existing.setName(course.getName());
        existing.setDescription(course.getDescription());
        existing.setDuration(course.getDuration());
        existing.setEntryRequirements(course.getEntryRequirements());
        existing.setFee(course.getFee());
        existing.setStatus(course.getStatus());
        return ResponseEntity.ok(courseRepository.save(existing));
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable String id) {
        courseRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // --- Teacher assignments ---
    @GetMapping("/teachers")
    public ResponseEntity<List<DemoAris>> listTeachers() {
        return ResponseEntity.ok(arisRepository.findAll());
    }

    @PostMapping("/teachers")
    public ResponseEntity<DemoAris> assignTeacher(@RequestBody DemoAris assignment) {
        return ResponseEntity.ok(arisRepository.save(assignment));
    }

    @PutMapping("/teachers/{id}")
    public ResponseEntity<DemoAris> updateTeacher(@PathVariable String id, @RequestBody DemoAris assignment) {
        DemoAris existing = arisRepository.findById(java.util.UUID.fromString(id)).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        existing.setTeacherAssignments(assignment.getTeacherAssignments());
        existing.setClassroomAssignments(assignment.getClassroomAssignments());
        return ResponseEntity.ok(arisRepository.save(existing));
    }

    // --- Classroom assignments ---
    @GetMapping("/classrooms")
    public ResponseEntity<List<DemoAris>> listClassrooms() {
        return ResponseEntity.ok(arisRepository.findAll());
    }

    @PostMapping("/classrooms")
    public ResponseEntity<DemoAris> assignClassroom(@RequestBody DemoAris assignment) {
        return ResponseEntity.ok(arisRepository.save(assignment));
    }

    // --- Semesters ---
    @GetMapping("/semesters")
    public ResponseEntity<List<DemoAris>> listSemesters() {
        return ResponseEntity.ok(arisRepository.findAll());
    }

    @PostMapping("/semesters")
    public ResponseEntity<DemoAris> createSemester(@RequestBody DemoAris semester) {
        return ResponseEntity.ok(arisRepository.save(semester));
    }

    // --- Course availability ---
    @GetMapping("/availability")
    public ResponseEntity<List<DemoAris>> listAvailability() {
        return ResponseEntity.ok(arisRepository.findAll());
    }

    @PostMapping("/availability")
    public ResponseEntity<DemoAris> setAvailability(@RequestBody DemoAris availability) {
        return ResponseEntity.ok(arisRepository.save(availability));
    }

    // --- Audit log ---
    @GetMapping("/audit")
    public ResponseEntity<List<AuditLog>> listAudit() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }
}