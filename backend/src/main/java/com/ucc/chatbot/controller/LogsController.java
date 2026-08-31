package com.ucc.chatbot.controller;

import com.ucc.chatbot.model.*;
import com.ucc.chatbot.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/logs")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://uccchatbot.netlify.app"})
public class LogsController {

    private final AILogRepository aiLogRepository;
    private final SystemLogRepository systemLogRepository;
    private final AuditLogRepository auditLogRepository;

    @Autowired
    public LogsController(AILogRepository aiLogRepository,
                           SystemLogRepository systemLogRepository,
                           AuditLogRepository auditLogRepository) {
        this.aiLogRepository = aiLogRepository;
        this.systemLogRepository = systemLogRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/ai")
    public ResponseEntity<List<AILog>> aiLogs(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(aiLogRepository.findTop100ByOrderByCreatedAtDesc().stream().limit(limit).toList());
    }

    @GetMapping("/system")
    public ResponseEntity<List<SystemLog>> systemLogs(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(systemLogRepository.findTop100ByOrderByCreatedAtDesc().stream().limit(limit).toList());
    }

    @GetMapping("/audit")
    public ResponseEntity<List<AuditLog>> auditLogs(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(auditLogRepository.findTop100ByOrderByCreatedAtDesc().stream().limit(limit).toList());
    }

    @GetMapping("/errors")
    public ResponseEntity<List<AILog>> errors(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(aiLogRepository.findTop100ByOrderByCreatedAtDesc().stream()
            .filter(l -> l.getStatus() != null && l.getStatus().contains("ERROR"))
            .limit(limit).toList());
    }
}
