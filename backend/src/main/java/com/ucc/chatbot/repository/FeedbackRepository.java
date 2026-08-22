package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, String> {
    List<Feedback> findAllByOrderByCreatedAtDesc();
}
