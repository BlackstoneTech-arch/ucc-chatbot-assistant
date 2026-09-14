package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.DemoLms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

public interface DemoLmsRepository extends JpaRepository<DemoLms, UUID> {
    List<DemoLms> findByStudentId(String studentId);
    List<DemoLms> findByStudentIdAndCourseCode(String studentId, String courseCode);
}