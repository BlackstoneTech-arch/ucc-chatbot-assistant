package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.News;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NewsRepository extends JpaRepository<News, String> {
    List<News> findByIsPublishedTrueOrderByPublishedAtDesc();
    List<News> findAllByOrderByCreatedAtDesc();
}
