-- ============================================
-- UCC AI Assistant — MySQL Database Schema
-- Version 2.0 — Complete Production Schema
-- ============================================

CREATE DATABASE IF NOT EXISTS ucc_chatbot_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ucc_chatbot_db;

-- ============================================
-- USERS, ROLES, PERMISSIONS (RBAC)
-- ============================================

CREATE TABLE IF NOT EXISTS roles (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS permissions (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(30) NOT NULL DEFAULT 'STAFF',
    is_active BOOLEAN DEFAULT TRUE,
    failed_login_count INT DEFAULT 0,
    locked_until DATETIME NULL,
    last_login DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_role (role)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token VARCHAR(500) UNIQUE NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_refresh_user (user_id),
    INDEX idx_refresh_token (token)
) ENGINE=InnoDB;

-- ============================================
-- CONVERSATIONS & MESSAGES
-- ============================================

CREATE TABLE IF NOT EXISTS conversations (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(36) NULL,
    user_agent TEXT,
    ip_address VARCHAR(45),
    language VARCHAR(10) DEFAULT 'en',
    last_programme VARCHAR(50) NULL,
    last_concept VARCHAR(255) NULL,
    last_intent VARCHAR(50) NULL,
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ended_at DATETIME NULL,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_conv_session (session_id),
    INDEX idx_conv_user (user_id),
    INDEX idx_conv_started (started_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS messages (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    language VARCHAR(10),
    intent VARCHAR(50),
    confidence DOUBLE,
    sources JSON,
    entities JSON,
    response_time_ms INT,
    model VARCHAR(100),
    is_escalated BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    INDEX idx_msg_conv (conversation_id),
    INDEX idx_msg_created (created_at),
    INDEX idx_msg_intent (intent)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS feedback (
    id VARCHAR(36) PRIMARY KEY,
    message_id VARCHAR(36) NULL,
    conversation_id VARCHAR(36) NULL,
    user_id VARCHAR(36) NULL,
    rating INT,
    thumbs VARCHAR(10),
    comment TEXT,
    feedback_type VARCHAR(50) DEFAULT 'response',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE SET NULL,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_fb_message (message_id),
    INDEX idx_fb_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS escalations (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NULL,
    message_id VARCHAR(36) NULL,
    user_id VARCHAR(36) NULL,
    question TEXT NOT NULL,
    detected_intent VARCHAR(50),
    confidence DOUBLE,
    retrieved_sources JSON,
    reason VARCHAR(255),
    priority VARCHAR(20) DEFAULT 'NORMAL',
    status VARCHAR(20) DEFAULT 'OPEN',
    assigned_to VARCHAR(36) NULL,
    resolution TEXT,
    notes TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at DATETIME NULL,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE SET NULL,
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_esc_status (status),
    INDEX idx_esc_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS support_tickets (
    id VARCHAR(36) PRIMARY KEY,
    escalation_id VARCHAR(36) NULL,
    subject VARCHAR(500) NOT NULL,
    description TEXT,
    priority VARCHAR(20) DEFAULT 'NORMAL',
    status VARCHAR(20) DEFAULT 'OPEN',
    assigned_to VARCHAR(36) NULL,
    created_by VARCHAR(36) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    closed_at DATETIME NULL,
    FOREIGN KEY (escalation_id) REFERENCES escalations(id) ON DELETE SET NULL,
    INDEX idx_ticket_status (status)
) ENGINE=InnoDB;

-- ============================================
-- KNOWLEDGE BASE (with RAG chunks)
-- ============================================

CREATE TABLE IF NOT EXISTS knowledge_categories (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(500),
    parent_id VARCHAR(36) NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES knowledge_categories(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS knowledge_documents (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    content LONGTEXT NOT NULL,
    category_id VARCHAR(36) NULL,
    category VARCHAR(100),
    source_url VARCHAR(500),
    source_type VARCHAR(50),
    academic_year VARCHAR(20),
    language VARCHAR(10) DEFAULT 'en',
    version INT DEFAULT 1,
    effective_date DATE,
    expiry_date DATE,
    approval_status VARCHAR(20) DEFAULT 'PENDING',
    is_active BOOLEAN DEFAULT TRUE,
    content_hash VARCHAR(64),
    uploaded_by VARCHAR(36) NULL,
    approved_by VARCHAR(36) NULL,
    approved_at DATETIME NULL,
    is_indexed BOOLEAN DEFAULT FALSE,
    indexed_at DATETIME NULL,
    metadata JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES knowledge_categories(id) ON DELETE SET NULL,
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_kd_category (category),
    INDEX idx_kd_status (approval_status),
    INDEX idx_kd_active (is_active),
    INDEX idx_kd_year (academic_year),
    INDEX idx_kd_hash (content_hash),
    FULLTEXT INDEX idx_kd_content (title, content)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS knowledge_chunks (
    id VARCHAR(36) PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL,
    chunk_index INT NOT NULL,
    chunk_text LONGTEXT NOT NULL,
    embedding_vector LONGTEXT,
    token_count INT,
    metadata JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES knowledge_documents(id) ON DELETE CASCADE,
    INDEX idx_chunk_doc (document_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS knowledge_versions (
    id VARCHAR(36) PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL,
    version_number INT NOT NULL,
    title VARCHAR(500),
    content LONGTEXT,
    content_hash VARCHAR(64),
    changed_by VARCHAR(36) NULL,
    change_note TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES knowledge_documents(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_kv_doc (document_id)
) ENGINE=InnoDB;

-- ============================================
-- FAQ
-- ============================================

CREATE TABLE IF NOT EXISTS faqs (
    id VARCHAR(36) PRIMARY KEY,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    category VARCHAR(100),
    subcategory VARCHAR(100),
    keywords TEXT,
    academic_year VARCHAR(20),
    language VARCHAR(10) DEFAULT 'en',
    priority INT DEFAULT 0,
    is_published BOOLEAN DEFAULT TRUE,
    created_by VARCHAR(36) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_faq_category (category),
    INDEX idx_faq_published (is_published)
) ENGINE=InnoDB;

-- ============================================
-- COURSES & SERVICES
-- ============================================

CREATE TABLE IF NOT EXISTS courses (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(50),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    level VARCHAR(50),
    duration VARCHAR(100),
    academic_year VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_course_code (code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS services (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    icon VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================
-- NEWS & EVENTS
-- ============================================

CREATE TABLE IF NOT EXISTS news (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    summary TEXT,
    content LONGTEXT NOT NULL,
    image_url VARCHAR(500),
    source_url VARCHAR(500),
    published_at DATETIME,
    expires_at DATETIME,
    is_published BOOLEAN DEFAULT FALSE,
    created_by VARCHAR(36) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_news_published (is_published, published_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS events (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    event_date DATE,
    event_time TIME,
    location VARCHAR(255),
    registration_url VARCHAR(500),
    is_published BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_event_date (event_date, is_published)
) ENGINE=InnoDB;

-- ============================================
-- CONTACTS
-- ============================================

CREATE TABLE IF NOT EXISTS contacts (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255),
    title VARCHAR(255),
    department VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    office_location VARCHAR(255),
    is_primary BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_contact_active (is_active)
) ENGINE=InnoDB;

-- ============================================
-- AI PROMPTS, INTEGRATIONS, LOGS
-- ============================================

CREATE TABLE IF NOT EXISTS ai_prompts (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    type VARCHAR(50) NOT NULL,
    content LONGTEXT NOT NULL,
    variables JSON,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ai_prompt_versions (
    id VARCHAR(36) PRIMARY KEY,
    prompt_id VARCHAR(36) NOT NULL,
    version_number INT NOT NULL,
    content LONGTEXT,
    changed_by VARCHAR(36) NULL,
    change_note TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (prompt_id) REFERENCES ai_prompts(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ai_settings (
    id VARCHAR(36) PRIMARY KEY,
    setting_key VARCHAR(100) UNIQUE NOT NULL,
    setting_value TEXT,
    description VARCHAR(500),
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ai_logs (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NULL,
    message_id VARCHAR(36) NULL,
    question TEXT,
    language VARCHAR(10),
    detected_intent VARCHAR(50),
    retrieved_documents JSON,
    confidence DOUBLE,
    model VARCHAR(100),
    prompt_tokens INT,
    completion_tokens INT,
    processing_time_ms INT,
    success BOOLEAN,
    error_message TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ailog_conv (conversation_id),
    INDEX idx_ailog_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS api_integrations (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    base_url VARCHAR(500),
    api_key VARCHAR(500),
    config JSON,
    timeout INT DEFAULT 5000,
    retry_count INT DEFAULT 3,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================
-- WEBSITE SYNC
-- ============================================

CREATE TABLE IF NOT EXISTS website_pages (
    id VARCHAR(36) PRIMARY KEY,
    url VARCHAR(500) UNIQUE NOT NULL,
    title VARCHAR(500),
    category VARCHAR(100),
    language VARCHAR(10) DEFAULT 'en',
    last_hash VARCHAR(64),
    last_scanned_at DATETIME,
    last_status VARCHAR(20),
    knowledge_document_id VARCHAR(36) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (knowledge_document_id) REFERENCES knowledge_documents(id) ON DELETE SET NULL,
    INDEX idx_wp_url (url(255))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS website_sync_jobs (
    id VARCHAR(36) PRIMARY KEY,
    status VARCHAR(20) DEFAULT 'PENDING',
    started_at DATETIME,
    completed_at DATETIME,
    pages_scanned INT DEFAULT 0,
    pages_new INT DEFAULT 0,
    pages_updated INT DEFAULT 0,
    pages_unchanged INT DEFAULT 0,
    pages_failed INT DEFAULT 0,
    error_message TEXT,
    started_by VARCHAR(36) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (started_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_wsj_status (status)
) ENGINE=InnoDB;

-- ============================================
-- LOGS & AUDIT
-- ============================================

CREATE TABLE IF NOT EXISTS system_logs (
    id VARCHAR(36) PRIMARY KEY,
    level VARCHAR(20) NOT NULL,
    component VARCHAR(100),
    message TEXT NOT NULL,
    details JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_slog_level (level),
    INDEX idx_slog_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NULL,
    action VARCHAR(255) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(36),
    old_values JSON,
    new_values JSON,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_created (created_at),
    INDEX idx_audit_action (action)
) ENGINE=InnoDB;

-- ============================================
-- ANALYTICS
-- ============================================

CREATE TABLE IF NOT EXISTS analytics_daily (
    id VARCHAR(36) PRIMARY KEY,
    date DATE NOT NULL UNIQUE,
    total_conversations INT DEFAULT 0,
    total_messages INT DEFAULT 0,
    total_users INT DEFAULT 0,
    successful_responses INT DEFAULT 0,
    failed_responses INT DEFAULT 0,
    escalations INT DEFAULT 0,
    avg_response_time_ms INT DEFAULT 0,
    avg_confidence DOUBLE DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS knowledge_gaps (
    id VARCHAR(36) PRIMARY KEY,
    question TEXT NOT NULL,
    language VARCHAR(10),
    detected_intent VARCHAR(50),
    count INT DEFAULT 1,
    resolved BOOLEAN DEFAULT FALSE,
    resolved_by VARCHAR(36) NULL,
    resolved_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_kg_resolved (resolved)
) ENGINE=InnoDB;
