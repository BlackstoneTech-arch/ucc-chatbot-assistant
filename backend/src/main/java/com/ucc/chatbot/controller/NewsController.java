package com.ucc.chatbot.controller;

import com.ucc.chatbot.model.News;
import com.ucc.chatbot.repository.NewsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://uccchatbot.netlify.app"})
public class NewsController {

    private final NewsRepository newsRepository;

    public NewsController(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    /** Public endpoint — published news only, newest first. */
    @GetMapping
    public ResponseEntity<List<News>> listPublished() {
        return ResponseEntity.ok(newsRepository.findByIsPublishedTrueOrderByPublishedAtDesc());
    }

    /** Admin endpoint — all news (including drafts), newest first. */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','EDITOR')")
    public ResponseEntity<List<News>> listAll() {
        return ResponseEntity.ok(newsRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','EDITOR')")
    public ResponseEntity<News> get(@PathVariable String id) {
        return newsRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','EDITOR')")
    public ResponseEntity<News> create(@RequestBody News news) {
        if (news.getIsPublished() == null) news.setIsPublished(false);
        if (news.getCreatedAt() == null) news.setCreatedAt(java.time.LocalDateTime.now());
        if (news.getCreatedBy() == null) news.setCreatedBy(currentUserEmail());
        return ResponseEntity.ok(newsRepository.save(news));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','EDITOR')")
    public ResponseEntity<News> update(@PathVariable String id, @RequestBody News news) {
        News existing = newsRepository.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        existing.setTitle(news.getTitle());
        existing.setSummary(news.getSummary());
        existing.setContent(news.getContent());
        existing.setImageUrl(news.getImageUrl());
        existing.setSourceUrl(news.getSourceUrl());
        existing.setPublishedAt(news.getPublishedAt());
        existing.setExpiresAt(news.getExpiresAt());
        existing.setIsPublished(news.getIsPublished());
        return ResponseEntity.ok(newsRepository.save(existing));
    }

    /** Toggle publish state without replacing the whole record. */
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','EDITOR')")
    public ResponseEntity<News> togglePublish(@PathVariable String id, @RequestBody Map<String, Object> body) {
        News existing = newsRepository.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        Object val = body.get("isPublished");
        if (val instanceof Boolean b) existing.setIsPublished(b);
        if (body.containsKey("publishedAt") && body.get("publishedAt") != null) {
            existing.setPublishedAt(java.time.LocalDateTime.parse(String.valueOf(body.get("publishedAt"))));
        }
        return ResponseEntity.ok(newsRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','EDITOR')")
    public ResponseEntity<?> delete(@PathVariable String id) {
        newsRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private String currentUserEmail() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
                return auth.getName();
            }
        } catch (Exception ignored) { }
        return null;
    }
}