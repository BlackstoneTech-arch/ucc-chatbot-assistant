package com.ucc.chatbot.config;

import com.ucc.chatbot.model.User;
import com.ucc.chatbot.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = System.getenv("ADMIN_EMAIL");
            String adminPassword = System.getenv("ADMIN_PASSWORD");
            String adminName = System.getenv("ADMIN_NAME");

            if (adminEmail == null || adminPassword == null || adminName == null) {
                return;
            }

            if (userRepository.count() == 0) {
                User admin = new User();
                admin.setEmail(adminEmail);
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                admin.setFullName(adminName);
                admin.setRole("ADMIN");
                admin.setIsActive(true);
                userRepository.save(admin);
            }
        };
    }
}
