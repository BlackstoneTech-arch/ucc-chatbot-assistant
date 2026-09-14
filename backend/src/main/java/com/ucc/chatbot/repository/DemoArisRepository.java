package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.DemoAris;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

public interface DemoArisRepository extends JpaRepository<DemoAris, UUID> {
    List<DemoAris> findByStudentId(String studentId);
    DemoAris findByStudentIdAndSemester(String studentId, String semester);
}