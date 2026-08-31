package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, String> {
    List<Contact> findByIsActiveTrueOrderByDisplayOrderAsc();
}
