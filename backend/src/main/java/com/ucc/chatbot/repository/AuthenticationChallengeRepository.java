package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.AuthenticationChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface AuthenticationChallengeRepository extends JpaRepository<AuthenticationChallenge, String> {
    Optional<AuthenticationChallenge> findByToken(String token);

    @Modifying
    @Query("UPDATE AuthenticationChallenge c SET c.used = true, c.usedAt = :now WHERE c.token = :token")
    void markUsed(@Param("token") String token, @Param("now") java.time.LocalDateTime now);
}