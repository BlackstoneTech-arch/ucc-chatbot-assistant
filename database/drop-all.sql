-- Drop all tables to allow schema regeneration
-- MySQL-compatible: disables foreign key checks before dropping
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS ai_logs;
DROP TABLE IF EXISTS ai_prompts;
DROP TABLE IF EXISTS api_integrations;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS escalations;
DROP TABLE IF EXISTS integrations;
DROP TABLE IF EXISTS knowledge_chunks;
DROP TABLE IF EXISTS knowledge_documents;
DROP TABLE IF EXISTS knowledge_versions;
DROP TABLE IF EXISTS knowledge_categories;
DROP TABLE IF EXISTS knowledge_gaps;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS conversations;
DROP TABLE IF EXISTS prompt_templates;
DROP TABLE IF EXISTS system_logs;
DROP TABLE IF EXISTS faqs;
DROP TABLE IF EXISTS contacts;
DROP TABLE IF EXISTS feedbacks;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS news;
DROP TABLE IF EXISTS events;
DROP TABLE IF EXISTS ucc_services;
DROP TABLE IF EXISTS services;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS website_pages;
DROP TABLE IF EXISTS website_sync_jobs;
DROP TABLE IF EXISTS ai_settings;
DROP TABLE IF EXISTS permissions_roles;

SET FOREIGN_KEY_CHECKS = 1;
