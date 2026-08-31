package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, String> {
    List<Event> findByIsPublishedTrueOrderByEventDateAsc();
    List<Event> findAllByOrderByEventDateDesc();
}
