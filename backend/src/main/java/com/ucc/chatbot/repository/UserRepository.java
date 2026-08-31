package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(String role);
    List<User> findByIsActiveTrue();
    boolean existsByEmail(String email);
    long countByRole(String role);
}
