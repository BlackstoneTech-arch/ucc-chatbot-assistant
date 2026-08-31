# UCC Chatbot Assistant — "Aisha"

AI-powered premium customer-care assistant for the University of Dar es Salaam Computing Centre (UCC).

**Official UCC Website:** https://ucc.co.tz/
**Admission Portal:** https://admission.ucc.co.tz/
**Live Chatbot:** https://agent-6a87a4d1bce5537b6d8d53a5--uccchatbot.netlify.app/
**Live Admin Dashboard:** https://admin-uccchatbot.netlify.app/

---

## Meet Aisha — Your UCC Virtual Customer-Care Assistant

> *"Hello there! I'm Aisha, your UCC Virtual Customer Care Assistant. It's a real pleasure to have you here today."*

Aisha is the warm, polished, concierge-grade persona behind the UCC Chatbot. She greets every visitor with genuine warmth, speaks fluent English and Kiswahili, and uses verified information from https://ucc.co.tz/ to answer questions about programmes, admissions, fees, IT services, and software products.

---

## Table of Contents

1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Query Understanding Layer](#query-understanding-layer)
4. [Technology Stack](#technology-stack)
5. [Project Structure](#project-structure)
6. [Database Schema](#database-schema)
7. [API Reference](#api-reference)
8. [Knowledge Base Management](#knowledge-base-management)
9. [AI Training & Prompt Engineering](#ai-training--prompt-engineering)
10. [Admin Dashboard](#admin-dashboard)
11. [Deployment Guide](#deployment-guide)
12. [Configuration](#configuration)
13. [Security](#security)
14. [Testing](#testing)
15. [Troubleshooting](#troubleshooting)
16. [License](#license)

---

## Overview

The UCC Chatbot Assistant is an intelligent conversational AI system designed to provide 24/7 customer-care support for the University of Dar es Salaam Computing Centre (UCC). It handles student inquiries about academic programmes, admissions, fees, registration, ICT services, and general university information in both English and Kiswahili.

### Key Features

- **Premium Persona — "Aisha":** Warm, charming, concierge-grade customer-care tone
- **Multilingual Support:** English, Kiswahili, and mixed-language queries
- **Query Understanding Layer:** Normalizes informal language, detects intent, extracts entities, and expands queries semantically
- **Knowledge Base Retrieval:** Retrieves verified information from structured documents
- **AI-Powered Responses:** Uses LLM APIs with retrieval-augmented generation (RAG)
- **Conversation Context:** Maintains context across follow-up questions
- **Admin Dashboard:** Full management interface for knowledge base, AI training, API integrations, and system monitoring
- **Escalation System:** Routes unverified queries to human agents
- **Verified Data:** All programme, fee, admission, and contact data verified against https://ucc.co.tz/

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        User Interface                           │
│  ┌─────────────────────────────┐  ┌──────────────────────────┐  │
│  │     Public Frontend         │  │    Admin Dashboard       │  │
│  │  (HTML/CSS/JavaScript)      │  │  (Management Console)    │  │
│  └──────────────┬──────────────┘  └───────────┬──────────────┘  │
└─────────────────┼──────────────────────────────┼────────────────┘
                  │                              │
                  │    HTTP/REST API             │
                  │                              │
┌─────────────────▼──────────────────────────────▼────────────────┐
│                    Spring Boot Backend (Port 8081)              │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    API Gateway / Controllers              │   │
│  │  ChatController | AuthController | AdminController       │   │
│  │  KnowledgeController | FAQController                     │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                  Query Understanding Layer                │   │
│  │  Language Detection → Normalization → Translation        │   │
│  │  → Synonym Detection → Intent Classification             │   │
│  │  → Entity Extraction → Canonical Query Generation         │   │
│  │  → Semantic Query Expansion                               │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                Knowledge Base Retrieval                   │   │
│  │  Document Matching → Context Building → Verification      │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                  AI Service (LLM Integration)             │   │
│  │  RAG Pipeline → Response Generation → Language Detection  │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                  Conversation Management                  │   │
│  │  Session Tracking → Context Persistence → History         │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                  │
                  │   JPA / JDBC
                  │
┌─────────────────▼────────────────────────────────────────────────┐
│                        MySQL Database                            │
│  users | conversations | messages | knowledge_documents          │
│  faqs | feedback | escalations | courses | services              │
└─────────────────────────────────────────────────────────────────┘
```

---

## Query Understanding Layer

The Query Understanding Layer is the core NLP pipeline that processes user messages before retrieval.

### Pipeline Steps

1. **Language Detection** → `en` / `sw` / `mixed`
2. **Greeting/Farewell/Thank-You/Help Normalization** → Direct responses, no retrieval
3. **Spelling & Informal Language Normalization** → Corrects typos and informal expressions
4. **Translation to Retrieval Language** → English for KB retrieval
5. **Synonym Detection** → Maps variants to canonical concepts
6. **Intent Classification** → Identifies user intent
7. **Entity Extraction** → Extracts programmes, courses, etc.
8. **Canonical Query Generation** → Creates normalized query
9. **Semantic Query Expansion** → Generates retrieval variations
10. **Knowledge Base Retrieval** → Fetches relevant context
11. **Answer Generation** → LLM generates response in user's language

### Supported Concepts

| Concept | English Synonyms | Kiswahili Synonyms |
|---------|-----------------|-------------------|
| `FEES` | fee, fees, cost, price, payment, tuition | ada, gharama, bei, malipo, kiasi cha kulipa |
| `ADMISSION` | admission, apply, application | kuomba, maombi, udahili, kujiunga |
| `ENTRY_REQUIREMENTS` | requirements, qualifications, criteria | vigezo, sifa, masharti |
| `PROGRAMME_DURATION` | duration, how long, years | muda, inachukua muda gani, miaka mingapi |
| `CONTACT_INFORMATION` | contact, phone, email | mawasiliano, namba, simu, email |
| `LOCATION` | location, address, where | mahali, anwani, mko wapi |

### Informal Kiswahili Normalization

| Informal | Normalized |
|----------|-----------|
| ada ngap | ada ngapi |
| kozi ipo | kozi zipo |
| nataka kujoin | nataka kujiunga |
| vigezo vya kujoin | vigezo vya kujiunga |
| naapply aje | naomba kuapply vipi |
| db it bei gan | db it ada ngapi |

---

## Technology Stack

### Frontend
- **Public Site:** HTML5, CSS3, Vanilla JavaScript
- **Admin Dashboard:** HTML5, CSS3, Vanilla JavaScript (no build step)

### Backend
- **Language:** Java 17+
- **Framework:** Spring Boot 3.3.4
- **Security:** Spring Security, JWT Authentication
- **ORM:** Spring Data JPA (Hibernate 6.5)
- **Database:** MySQL 8.0+
- **Build Tool:** Maven 3.9+
- **AI Integration:** OpenAI-compatible API (GPT-4o-mini / custom LLM)

### Infrastructure
- **Frontend Hosting:** Netlify
- **Backend Hosting:** Spring Boot embedded Tomcat
- **Database:** MySQL

---

## Project Structure

```
ucc-chatbot-assistant/
├── frontend/                          # Public-facing website
│   ├── index.html                     # Homepage
│   ├── chat.html                      # Chat interface
│   ├── about.html                     # About UCC
│   ├── courses.html                   # Programmes listing
│   ├── services.html                  # Services listing
│   ├── contact.html                   # Contact page
│   ├── css/
│   │   └── style.css                  # Public site styles
│   └── js/
│       ├── config.js                  # Configuration
│       ├── api.js                     # API client
│       ├── chatbot.js                 # Chatbot widget logic
│       └── app.js                     # General app logic
│
├── admin/                             # Admin dashboard
│   ├── index.html                     # Login page
│   ├── dashboard.html                 # Main dashboard (SPA)
│   ├── css/
│   │   └── admin.css                  # Admin styles
│   └── js/
│       ├── admin-auth.js              # Authentication logic
│       └── admin.js                   # Dashboard logic
│
├── backend/                           # Spring Boot backend
│   ├── pom.xml                        # Maven dependencies
│   └── src/
│       ├── main/
│       │   ├── java/com/ucc/chatbot/
│       │   │   ├── UccChatbotApplication.java
│       │   │   ├── config/             # Security, CORS, etc.
│       │   │   ├── controller/         # REST controllers
│       │   │   │   ├── ChatController.java
│       │   │   │   ├── AuthController.java
│       │   │   │   ├── AdminController.java
│       │   │   │   ├── KnowledgeController.java
│       │   │   │   ├── FAQController.java
│       │   │   │   └── CourseController.java
│       │   │   ├── service/            # Business logic
│       │   │   │   ├── ChatService.java
│       │   │   │   ├── AIService.java
│       │   │   │   ├── QueryUnderstandingService.java
│       │   │   │   ├── KnowledgeService.java
│       │   │   │   ├── ConversationService.java
│       │   │   │   └── ...
│       │   │   ├── model/              # JPA entities
│       │   │   │   ├── User.java
│       │   │   │   ├── Conversation.java
│       │   │   │   ├── Message.java
│       │   │   │   ├── KnowledgeDocument.java
│       │   │   │   ├── FAQ.java
│       │   │   │   ├── Course.java
│       │   │   │   ├── Service.java
│       │   │   │   ├── Feedback.java
│       │   │   │   └── Escalation.java
│       │   │   ├── repository/         # JPA repositories
│       │   │   ├── dto/                # Data Transfer Objects
│       │   │   │   ├── ChatRequest.java
│       │   │   │   ├── ChatResponse.java
│       │   │   │   ├── QueryUnderstandingResult.java
│       │   │   │   ├── KnowledgeRequest.java
│       │   │   │   └── ...
│       │   │   ├── exception/          # Custom exceptions
│       │   │   └── util/               # Utilities
│       │   └── resources/
│       │       ├── application.properties
│       │       ├── application-dev.properties
│       │       └── db/
│       │           └── schema.sql
│       └── test/
│           └── java/com/ucc/chatbot/
│
├── knowledge-base/                     # Source documents for KB
│   ├── admissions/
│   │   └── admissions.md
│   ├── programmes/
│   │   └── programmes.md
│   ├── fees/
│   │   └── fees.md
│   ├── registration/
│   │   └── registration.md
│   ├── ict-support/
│   │   └── ict-support.md
│   ├── student-services/
│   │   └── student-services.md
│   ├── contacts/
│   │   └── contacts.md
│   ├── professional-training/
│   │   └── professional-training.md
│   ├── examinations/
│   │   └── examinations.md
│   ├── accommodation/
│   │   └── accommodation.md
│   ├── events/
│   │   └── events.md
│   ├── news/
│   │   └── news.md
│   ├── infrastructure/
│   │   └── infrastructure.md
│   ├── consulting/
│   │   └── consulting.md
│   ├── campuses/
│   │   └── campuses.md
│   ├── regulations/
│   │   └── regulations.md
│   ├── software-services/
│   │   └── software-services.md
│   ├── faqs/
│   │   └── faqs.md
│   └── about-ucc/
│       └── about-ucc.md
│
├── docs/                               # Documentation
│   ├── PROJECT_REPORT.md
│   ├── API_DOCUMENTATION.md
│   └── ADMIN_GUIDE.md
│
├── scripts/                            # Utility scripts
│   ├── import-knowledge-base.sh
│   └── backup-db.sh
│
└── README.md
```

---

## Database Schema

### Core Tables

#### `users`
| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `email` | VARCHAR(255) | Login email |
| `password` | VARCHAR(255) | BCrypt hashed password |
| `full_name` | VARCHAR(255) | Display name |
| `role` | ENUM | `ADMIN`, `STAFF`, `SUPER_ADMIN` |
| `created_at` | TIMESTAMP | Account creation |
| `updated_at` | TIMESTAMP | Last update |

#### `conversations`
| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `session_id` | VARCHAR(100) | Browser session ID |
| `user_id` | UUID | Linked user (nullable) |
| `started_at` | TIMESTAMP | Conversation start |
| `updated_at` | TIMESTAMP | Last activity |
| `is_active` | BOOLEAN | Active flag |
| `last_programme` | VARCHAR(50) | Context: last programme mentioned |
| `last_concept` | VARCHAR(100) | Context: last concept |
| `last_intent` | VARCHAR(100) | Context: last intent |

#### `messages`
| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `conversation_id` | UUID | FK to conversations |
| `sender` | ENUM | `USER` or `ASSISTANT` |
| `content` | TEXT | Message content |
| `created_at` | TIMESTAMP | Message timestamp |

#### `knowledge_documents`
| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `title` | VARCHAR(500) | Document title |
| `content` | TEXT | Full text content |
| `category` | VARCHAR(100) | Document category |
| `source_url` | VARCHAR(500) | Original source URL |
| `source_type` | VARCHAR(50) | `WEB`, `FILE`, `MANUAL` |
| `academic_year` | VARCHAR(20) | Academic year label |
| `version` | VARCHAR(20) | Document version |
| `approval_status` | ENUM | `PENDING`, `APPROVED`, `REJECTED` |
| `is_active` | BOOLEAN | Active flag |
| `created_at` | TIMESTAMP | Creation time |
| `updated_at` | TIMESTAMP | Last update |

#### `faqs`
| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `question` | TEXT | FAQ question |
| `answer` | TEXT | FAQ answer |
| `category` | VARCHAR(100) | Category |
| `is_active` | BOOLEAN | Active flag |
| `created_at` | TIMESTAMP | Creation time |
| `updated_at` | TIMESTAMP | Last update |

#### `feedback`
| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `conversation_id` | UUID | FK to conversations |
| `rating` | INT | 1-5 rating |
| `comment` | TEXT | Optional comment |
| `created_at` | TIMESTAMP | Submission time |

#### `escalations`
| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `conversation_id` | UUID | FK to conversations |
| `reason` | TEXT | Escalation reason |
| `status` | ENUM | `OPEN`, `IN_PROGRESS`, `RESOLVED` |
| `created_at` | TIMESTAMP | Creation time |

#### `audit_logs`
| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `user_id` | UUID | Actor |
| `action` | VARCHAR(100) | Action performed |
| `entity_type` | VARCHAR(100) | Target entity |
| `entity_id` | UUID | Target ID |
| `old_values` | JSON | Previous state |
| `new_values` | JSON | New state |
| `ip_address` | VARCHAR(45) | Client IP |
| `created_at` | TIMESTAMP | Action time |

---

## API Reference

### Public Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/chat` | Send a chat message |
| `GET` | `/api/faqs` | List public FAQs |
| `GET` | `/api/courses` | List public courses |
| `GET` | `/api/services` | List public services |
| `GET` | `/api/contacts` | List public contacts |
| `GET` | `/api/health` | Health check |

### Authentication Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/login` | User login (returns JWT) |
| `POST` | `/api/auth/refresh` | Refresh JWT token |
| `POST` | `/api/auth/logout` | Invalidate token |

### Admin Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/admin/dashboard` | Dashboard analytics |
| `GET` | `/api/admin/conversations` | List all conversations |
| `GET` | `/api/admin/conversations/{id}` | Get conversation details |
| `GET` | `/api/admin/conversations/{id}/messages` | Get conversation messages |
| `GET` | `/api/admin/feedback` | List all feedback |
| `GET` | `/api/admin/escalations` | List all escalations |
| `PUT` | `/api/admin/escalations/{id}` | Update escalation status |

### Knowledge Base Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/admin/knowledge` | List all documents |
| `GET` | `/api/admin/knowledge/{id}` | Get document by ID |
| `POST` | `/api/admin/knowledge` | Create new document |
| `PUT` | `/api/admin/knowledge/{id}` | Update document |
| `DELETE` | `/api/admin/knowledge/{id}` | Delete document |
| `POST` | `/api/admin/knowledge/upload` | Upload file (PDF, DOCX, MD, TXT) |
| `POST` | `/api/admin/knowledge/import` | Bulk import from folder |
| `GET` | `/api/admin/knowledge/export` | Export all documents (JSON) |

### FAQ Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/admin/faqs` | List all FAQs |
| `POST` | `/api/admin/faqs` | Create FAQ |
| `PUT` | `/api/admin/faqs/{id}` | Update FAQ |
| `DELETE` | `/api/admin/faqs/{id}` | Delete FAQ |

### AI Training Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/admin/ai/prompts` | List prompt templates |
| `POST` | `/api/admin/ai/prompts` | Create prompt template |
| `PUT` | `/api/admin/ai/prompts/{id}` | Update prompt template |
| `DELETE` | `/api/admin/ai/prompts/{id}` | Delete prompt template |
| `POST` | `/api/admin/ai/test` | Test AI with custom prompt |
| `GET` | `/api/admin/ai/logs` | List AI interaction logs |

### API Integration Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/admin/integrations` | List all integrations |
| `POST` | `/api/admin/integrations` | Add new integration |
| `PUT` | `/api/admin/integrations/{id}` | Update integration |
| `DELETE` | `/api/admin/integrations/{id}` | Delete integration |
| `POST` | `/api/admin/integrations/{id}/test` | Test integration connection |

### System Logs & Errors

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/admin/logs` | List system logs |
| `GET` | `/api/admin/logs/errors` | List error logs only |
| `GET` | `/api/admin/logs/errors/{id}` | Get error details |
| `PUT` | `/api/admin/logs/errors/{id}/resolve` | Mark error as resolved |
| `GET` | `/api/admin/logs/export` | Export logs (JSON/CSV) |

---

## Knowledge Base Management

### Knowledge Document Model

```json
{
  "id": "uuid",
  "title": "DBIT Fees 2026/2027",
  "content": "Full text content here...",
  "category": "fees",
  "sourceUrl": "https://ucc.co.tz/course/...",
  "sourceType": "WEB",
  "academicYear": "2026/2027",
  "version": "1.0",
  "approvalStatus": "APPROVED",
  "isActive": true,
  "createdAt": "2026-01-01T00:00:00",
  "updatedAt": "2026-01-01T00:00:00"
}
```

### Supported File Formats

- **PDF** (.pdf) - Text extraction via Apache PDFBox
- **Word** (.docx) - Text extraction via Apache POI
- **Markdown** (.md) - Direct import
- **Plain Text** (.txt) - Direct import
- **JSON** (.json) - Structured import
- **CSV** (.csv) - Tabular data import

### Import Process

1. Admin uploads file via dashboard
2. Backend extracts text content
3. System chunks content into retrievable segments
4. Documents are indexed with metadata
5. Admin reviews and approves

---

## AI Training & Prompt Engineering

### Prompt Template Structure

```json
{
  "id": "uuid",
  "name": "Programme Info Response",
  "type": "SYSTEM_PROMPT",
  "content": "You are the UCC Chatbot Assistant...",
  "variables": ["programme_name", "academic_year"],
  "isActive": true,
  "version": "1.0",
  "createdAt": "2026-01-01T00:00:00"
}
```

### Training Workflow

1. **Prompt Management:** Create/edit system prompts and response templates
2. **Test Console:** Test prompts against sample queries in real-time
3. **A/B Testing:** Compare different prompt versions
4. **Performance Metrics:** Track response quality, relevance, and user satisfaction

---

## Admin Dashboard

### Dashboard Tabs

#### 1. Overview Dashboard
- Total conversations (24h / 7d / 30d)
- Total messages
- Average response time
- User satisfaction rating
- Active knowledge documents
- Error rate
- System health status

#### 2. Knowledge Base Management
- Document list with search/filter
- Create/Edit/Delete documents
- File upload (drag & drop)
- Bulk import from folder
- Export knowledge base
- Approval workflow
- Version history

#### 3. AI Training
- Prompt template management
- Create/edit system prompts
- Test console (real-time testing)
- A/B test runner
- Training data management
- Model configuration

#### 4. API Integrations
- Registered integrations list
- Add/edit/delete integrations
- Connection testing
- API key management
- Webhook configuration
- Rate limit monitoring
- Integration logs

#### 5. Conversations
- Conversation list with filters
- Conversation detail view
- Message timeline
- User session tracking
- Export conversations

#### 6. System Logs
- Real-time log stream
- Error log highlighting
- Log filtering by level/date/component
- Error detail view with stack traces
- Mark as resolved workflow
- Log export (JSON/CSV)

#### 7. FAQ Management
- FAQ list
- Create/edit/delete FAQs
- Category management
- Reorder FAQs
- Preview changes

---

## Deployment Guide

### Prerequisites

- Java 17+
- Maven 3.9+
- MySQL 8.0+
- Node.js 18+ (for serving frontend)

### Backend Setup

```bash
cd backend

# Configure application.properties
# Set DB_URL, DB_USERNAME, DB_PASSWORD, AI_API_KEY, JWT_SECRET

# Run with Maven
mvn spring-boot:run

# Or build JAR and run
mvn clean package
java -jar target/ucc-chatbot-1.0.0.jar
```

### Frontend Setup

```bash
# Serve public frontend
cd frontend
npx serve . -l 3000

# Serve admin dashboard
cd admin
npx serve . -l 3001
```

### Database Setup

```bash
# Create database
mysql -u root -p
CREATE DATABASE ucc_chatbot CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# Run schema
mysql -u root -p ucc_chatbot < backend/src/main/resources/db/schema.sql

# Seed initial data
mysql -u root -p ucc_chatbot < backend/src/main/resources/db/seed.sql
```

---

## Configuration

### application.properties

```properties
# Server
server.port=8081
server.servlet.context-path=/

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/ucc_chatbot?useSSL=false&serverTimezone=UTC
spring.datasource.username=ucc_admin
spring.datasource.password=secure_password
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# AI Configuration
ai.api.url=https://api.openai.com/v1
ai.api.key=sk-...
ai.model=gpt-4o-mini

# JWT
jwt.secret=your-256-bit-secret-key-here
jwt.expiration=86400000

# File Upload
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB

# Logging
logging.level.root=INFO
logging.level.com.ucc.chatbot=DEBUG
logging.file.name=logs/ucc-chatbot.log
```

---

## Security

- **Authentication:** JWT-based stateless authentication
- **Authorization:** Role-based access control (RBAC)
  - `SUPER_ADMIN` - Full system access
  - `ADMIN` - Knowledge base and analytics access
  - `STAFF` - Read-only access to conversations
- **Password Storage:** BCrypt with strength 12
- **CORS:** Configured for specific origins
- **Input Validation:** Jakarta Validation on all DTOs
- **SQL Injection Prevention:** JPA parameterized queries
- **XSS Prevention:** Output escaping in frontend
- **Rate Limiting:** Configurable per endpoint
- **Audit Logging:** All admin actions logged

---

## Testing

### Backend Tests

```bash
cd backend
mvn test
```

### Manual API Testing

```bash
# Health check
curl http://localhost:8081/api/health

# Login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@ucc.co.tz","password":"password"}'

# Chat (with token)
curl -X POST http://localhost:8081/api/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"message":"What programmes does UCC offer?","conversationId":"test-123"}'
```

---

## Troubleshooting

### Common Issues

1. **Port 8081 already in use**
   ```bash
   # Windows
   netstat -ano | findstr :8081
   taskkill /PID <pid> /F

   # Linux/Mac
   lsof -ti:8081 | xargs kill -9
   ```

2. **Database connection refused**
   - Verify MySQL is running
   - Check `spring.datasource.url` in `application.properties`
   - Ensure database `ucc_chatbot` exists

3. **AI API errors**
   - Verify `AI_API_KEY` is set correctly
   - Check API endpoint URL
   - Ensure API key has sufficient credits/quota

4. **CORS errors in frontend**
   - Verify backend CORS configuration
   - Check frontend API_BASE_URL in config

---

## License

Proprietary - University of Dar es Salaam Computing Centre (UCC)

Copyright (c) 2026 UCC. All rights reserved.
