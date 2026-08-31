package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.WebsiteSyncJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WebsiteSyncJobRepository extends JpaRepository<WebsiteSyncJob, String> {
    List<WebsiteSyncJob> findAllByOrderByCreatedAtDesc();
}
