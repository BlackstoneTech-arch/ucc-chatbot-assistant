package com.ucc.chatbot.controller;

import com.ucc.chatbot.model.AuditLog;
import com.ucc.chatbot.repository.AuditLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/logs")
@CrossOrigin
public class LogsController {

    private final AuditLogRepository auditLogRepository;

    public LogsController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public ResponseEntity<List<AuditLog>> getLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }
}
