package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, String> {
    List<Course> findByStatus(String status);
}
