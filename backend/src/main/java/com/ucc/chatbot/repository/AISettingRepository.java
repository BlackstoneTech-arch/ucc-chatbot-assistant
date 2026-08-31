package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.AISetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface AISettingRepository extends JpaRepository<AISetting, String> {
    Optional<AISetting> findByKeyAndIsActiveTrue(String key);
    List<AISetting> findByIsActiveTrue();
}
