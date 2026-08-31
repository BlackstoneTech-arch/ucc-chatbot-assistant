package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.WebsitePage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WebsitePageRepository extends JpaRepository<WebsitePage, String> {
    Optional<WebsitePage> findByUrl(String url);
    List<WebsitePage> findByCategory(String category);
}
