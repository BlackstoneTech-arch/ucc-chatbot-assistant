# UCC Chatbot Assistant

> **AI-powered customer-care assistant for the University of Dar es Salaam Computing Centre (UCC).**
> Production-ready, bilingual (English + Kiswahili), RAG-based, with a complete admin dashboard.

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8-blue.svg)](https://www.mysql.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-Internal-yellow.svg)](#license)

---

## Table of Contents

- [What it does](#what-it-does)
- [Quick start (5 minutes)](#quick-start-5-minutes)
- [Architecture](#architecture)
- [Key features](#key-features)
- [Repository layout](#repository-layout)
- [Environment variables](#environment-variables)
- [API reference](#api-reference)
- [Admin dashboard](#admin-dashboard)
- [Database schema](#database-schema)
- [Deployment](#deployment)
- [Security](#security)
- [Performance & cost](#performance--cost)
- [Verification (acceptance test)](#verification-acceptance-test)
- [Documentation map](#documentation-map)
- [Contributing](#contributing)
- [License](#license)

---

## What it does

The UCC Chatbot Assistant is a complete customer-care platform that:

1. **Answers visitor questions** about UCC programmes (DCIT, DBIT, CCIT, CBIT), fees, admission requirements, contacts, branches, and services
2. **Speaks two languages** — English and Kiswahili — with automatic language detection
3. **Retrieves from a knowledge base** the admin team can manage without touching code
4. **Falls back to an LLM** (OpenAI, OpenRouter, Ollama, or any compatible API) for questions the KB can't answer
5. **Logs every conversation** for review, compliance, and improvement
6. **Provides a full admin dashboard** for content management, AI training, user management, and observability

The public chatbot is currently live at **https://agent-6a87a4d1bce5537b6d8d53a5--uccchatbot.netlify.app/**.

---

## Quick start (5 minutes)

### Windows

```powershell
# 1. Build (downloads Java + Maven on first run, takes ~2 min)
.\build-backend.ps1 -Package

# 2. Copy and fill the environment file
cp .env.example .env
# Edit .env: set DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, ADMIN_PASSWORD

# 3. Start the backend
.\start-backend.ps1

# 4. In a NEW terminal, start the admin dashboard
.\start-admin.ps1

# 5. Open http://localhost:3001 and log in with:
#    admin@ucc.co.tz / (whatever you set ADMIN_PASSWORD to in .env)
```

### macOS / Linux

```bash
# 1. Build
cd backend && mvn clean package -DskipTests && cd ..

# 2. Configure
cp .env.example .env
# Edit .env: set DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, ADMIN_PASSWORD

# 3. Run (loads .env automatically)
set -a; source .env; set +a
java -jar backend/target/ucc-chatbot-1.0.0.jar --server.port=${PORT:-8080}

# 4. In a NEW terminal, serve the admin dashboard
cd admin && npx serve . -l 3001
```

### Docker Compose (the easiest path)

```bash
cp .env.example .env
# Edit .env (DB_*, JWT_SECRET, ADMIN_PASSWORD)
docker compose up -d
# Wait ~30 seconds for MySQL + backend to start
curl http://localhost:8080/api/health
```

### Verify it works

```bash
# Health check
curl http://localhost:8080/api/health
# Expected: {"status":"UP","database":"UP","aiService":"UP",...}

# Public chat
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"What programmes do you offer?","conversationId":"test-1"}'

# Admin login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@ucc.co.tz","password":"YourAdminPassword"}'
```

For a thorough step-by-step guide (including how to provision a real database, configure secrets, deploy to production, troubleshoot issues, and maintain the system), see **[`docs/SETUP_GUIDE.md`](docs/SETUP_GUIDE.md)**.

---

## Architecture

```
┌──────────────────┐
│  Public Browser  │  https://uccchatbot.your-domain.com
│  (visitor)       │
└────────┬─────────┘
         │  /api/chat, /api/faqs, /api/health
         ▼
┌──────────────────────────────────────────────────────────────────┐
│                  Spring Boot 3.3.4 / Java 17 (port 8080)         │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  Public Layer (no auth)                                    │  │
│  │  ├─ ChatController       POST /api/chat                    │  │
│  │  └─ FaqAdminController  GET  /api/faqs, /api/contacts    │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  Chat Pipeline                                             │  │
│  │  ChatRequest                                               │  │
│  │     ▼                                                      │  │
│  │  ChatService.processMessage()                              │  │
│  │     ▼                                                      │  │
│  │  QueryUnderstandingService.understand()                     │  │
│  │     ├─ normalize text (lowercase, strip, fix typos)        │  │
│  │     ├─ detect language (EN/SW/mixed)                       │  │
│  │     ├─ classify intent (GREETING/FAREWELL/THANKS/HELP/    │  │
│  │     │    PROGRAMME_INQUIRY/FEE_INQUIRY/ADMISSION/.../      │  │
│  │     │    CONTACT/LOCATION/HOURS)                           │  │
│  │     ├─ extract entities (programme code, concept)          │  │
│  │     ├─ check conversation context (lastProgramme, etc.)    │  │
│  │     └─ decide: direct response OR retrieval needed         │  │
│  │     ▼                                                      │  │
│  │  if direct → build templated bilingual response            │  │
│  │  else        ▼                                             │  │
│  │  AIService.generateResponse()                              │  │
│  │     ├─ KnowledgeService.retrieveAndRank()  (RAG)           │  │
│  │     │     └─ scores documents vs query (title 3x,          │  │
│  │     │        content 1x, category 2x)                       │  │
│  │     ├─ build system prompt with context                    │  │
│  │     ├─ call OpenAI-compatible API (optional, encrypted key)│  │
│  │     └─ fall back to static UCC knowledge base              │  │
│  │     ▼                                                      │  │
│  │  save messages + log to AILog + update context             │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  Admin Layer (JWT required)                                │  │
│  │  ├─ AuthController        /api/auth/{login,refresh,...}    │  │
│  │  ├─ DashboardController   /api/admin/dashboard/*          │  │
│  │  ├─ KnowledgeController   /api/admin/knowledge/*          │  │
│  │  ├─ AITrainingController  /api/admin/ai/*                 │  │
│  │  ├─ IntegrationController /api/admin/integrations/*       │  │
│  │  ├─ LogsController        /api/admin/logs/*               │  │
│  │  ├─ FaqAdminController    /api/admin/faqs/*               │  │
│  │  ├─ ConversationAdminController  /api/admin/conversations  │  │
│  │  └─ AdminController       /api/admin/dashboard            │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  Cross-cutting                                             │  │
│  │  ├─ SecurityConfig        Spring Security + RBAC           │  │
│  │  ├─ JwtAuthFilter         parses Bearer, sets auth ctx     │  │
│  │  ├─ CorsConfig            6 allowed origins               │  │
│  │  ├─ EncryptionUtil        AES-256 for stored API keys      │  │
│  │  ├─ GlobalExceptionHandler uniform error responses         │  │
│  │  └─ AsyncConfig           @Async (website sync)            │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  Data Layer (JPA / Hibernate, ddl-auto=update)             │  │
│  │  User · RefreshToken · Role · Permission                   │  │
│  │  Conversation · Message                                    │  │
│  │  KnowledgeDocument · KnowledgeChunk · KnowledgeVersion     │  │
│  │  · KnowledgeCategory · KnowledgeGap                        │  │
│  │  FAQ · Contact                                             │  │
│  │  AIPrompt · AISetting · AILog · Feedback                   │  │
│  │  Integration · ApiIntegration · PromptTemplate             │  │
│  │  Escalation · WebsitePage · WebsiteSyncJob                 │  │
│  │  SystemLog · AuditLog · News · Event · UCCService · Course │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────┬───────────────────────────────────────┘
                           │  JDBC
                           ▼
              ┌─────────────────────────┐
              │  MySQL 8  /  PostgreSQL  │
              └─────────────────────────┘

┌──────────────────┐
│  Admin Browser   │  https://admin.your-domain.com
│  (operator)      │  (vanilla HTML/CSS/JS SPA)
└────────┬─────────┘
         │  /api/auth/login, /api/admin/*
         ▼   (Bearer JWT in Authorization header)
       (same backend as public)
```

---

## Key features

### For visitors (public chatbot)

- **Bilingual NLP** — auto-detects English, Kiswahili, or mixed-language input and replies in kind
- **Typo & synonym tolerance** — `"prog"`, `"programme"`, `"kozi"`, `"programu"` all map to the same intent
- **Intent classification** — 11 built-in intents: `GREETING`, `FAREWELL`, `THANKS`, `HELP_REQUEST`, `PROGRAMME_INQUIRY`, `FEE_INQUIRY`, `ADMISSION_INQUIRY`, `ENTRY_REQUIREMENTS`, `DURATION`, `CONTACT_INQUIRY`, `LOCATION`
- **Entity extraction** — recognises `DCIT`, `DBIT`, `CCIT`, `CBIT` and free-text programme names
- **Conversation memory** — "what are the fees?" after asking about DCIT gets DCIT fees, not generic
- **RAG pipeline** — retrieves from the live knowledge base before answering
- **LLM fallback** — OpenAI / OpenRouter / Ollama / any OpenAI-compatible API
- **Static KB fallback** — works fully offline if no LLM key is configured
- **Confidence scoring** — every response includes a 0.0–1.0 confidence
- **Escalation hint** — low-confidence answers flag for human handoff
- **Feedback collection** — thumbs up/down + comments per message
- **Works without backend** — frontend has its own `ucc-kb.js` with the full UCC static knowledge for resilience

### For admins (admin dashboard)

- **Single-page SPA** — vanilla HTML/CSS/JS, no build step, no framework, fast
- **Login + JWT** — secure, with refresh tokens, role-based access
- **Dashboard** — KPIs: total/active/today/week/month conversations, messages, users, knowledge docs, AI logs, feedback, escalations
- **Activity feed** — last 20 system events
- **Knowledge base** — CRUD, categories, search, approve/reject workflow, file upload (PDF, text, markdown), reindex
- **AI training** — manage prompt templates, configure AI settings (API key encrypted at rest), test the AI live, browse AI logs
- **Integrations** — manage external integrations (Slack, WhatsApp, etc.), test connectivity
- **FAQs** — full CRUD with categories, priority, publish/draft
- **Contacts** — view all UCC contact channels
- **Conversations** — browse all chats, read full transcripts, delete
- **Logs** — AI / system / audit / error log viewers
- **Website sync** — one-click scrape of https://ucc.co.tz/ with diff detection

### For operators (infrastructure)

- **MySQL 8 + PostgreSQL 14+** — same code, switchable via env var
- **Spring Boot 3.3.4** — mainstream LTS
- **Java 17 target** — runs on JDK 17+ (verified on JDK 25)
- **Auto schema migration** — `ddl-auto=update` creates tables on first run
- **AES-256 encryption** for stored API keys (configurable secret)
- **JWT** stateless auth (HS256, 7-day default)
- **Refresh tokens** stored in DB, revocable
- **CORS** configurable per environment
- **Docker Compose** — full stack (MySQL + backend + nginx) in one command
- **Health endpoints** — `/api/health`, `/api/admin/dashboard/health`
- **Structured logging** — INFO/DEBUG levels, request IDs, SLF4J
- **Audit logs** — every admin write is recorded
- **Production-ready scripts** — Windows `.ps1` + Unix `mvn`/`java` paths

---

## Repository layout

```
ucc-chatbot-assistant/
│
├── backend/                                # Spring Boot 3.3.4 / Java 17
│   ├── src/main/java/com/ucc/chatbot/
│   │   ├── UccChatbotApplication.java      # Main class
│   │   ├── config/                         # 6 files: Security, CORS, JWT filter,
│   │   │                                   #           Async, RestTemplate, DataLoader
│   │   ├── controller/                     # 12 REST controllers
│   │   │                                   # (1 public chat, 9 admin, 1 legacy admin,
│   │   │                                   #  1 auth)
│   │   ├── dto/                            # 7 DTOs: ChatRequest, ChatResponse,
│   │   │                                   #     QueryUnderstandingResult, FAQRequest,
│   │   │                                   #     KnowledgeRequest, LoginRequest,
│   │   │                                   #     LoginResponse, ApiResponse
│   │   ├── exception/                      # GlobalExceptionHandler
│   │   ├── model/                          # 28 JPA entities (User, Conversation,
│   │   │                                   #   Message, Knowledge*, FAQ, Contact,
│   │   │                                   #   AI*, Integration, Audit, etc.)
│   │   ├── repository/                     # 28 Spring Data JPA repositories
│   │   ├── service/                        # 9 service interfaces
│   │   ├── service/impl/                   # 9 service implementations
│   │   │   ├─ ChatServiceImpl              # Orchestrates the chat pipeline
│   │   │   ├─ QueryUnderstandingServiceImpl # The NLP engine
│   │   │   ├─ AIServiceImpl                # LLM + static KB fallback
│   │   │   ├─ KnowledgeServiceImpl         # RAG retrieval, chunking, indexing
│   │   │   ├─ WebsiteSyncService           # Async ucc.co.tz scraper
│   │   │   ├─ AuthServiceImpl              # JWT + refresh + RBAC
│   │   │   ├─ ConversationServiceImpl      # Message persistence + context
│   │   │   ├─ RetrievalServiceImpl         # FAQ search
│   │   │   └─ JwtServiceImpl               # Token generation/validation
│   │   └── util/                           # EncryptionUtil (AES-256)
│   ├── src/main/resources/
│   │   └── application.properties          # All config is env-driven
│   └── pom.xml                             # Maven build, all dependencies
│
├── admin/                                  # Admin dashboard (static SPA)
│   ├── index.html                          # Login page
│   ├── dashboard.html                      # Single-page app (15+ tabs)
│   ├── js/
│   │   ├── admin-auth.js                   # login / logout / token refresh
│   │   └── admin.js                        # All dashboard functionality
│   ├── css/
│   │   └── admin.css                       # Responsive styles
│   ├── netlify.toml                        # Netlify deploy config
│   └── package.json                        # (for `npx serve` local dev)
│
├── frontend/                               # Public website + chatbot widget
│   ├── index.html                          # Homepage
│   ├── chat.html                           # Standalone chatbot
│   ├── about.html, courses.html, contact.html, services.html
│   └── js/
│       ├── app.js                          # Site-wide interactivity
│       ├── chatbot.js                      # Chat widget (talks to /api/chat)
│       └── ucc-kb.js                       # Bilingual static KB fallback
│
├── database/
│   ├── schema.sql                          # MySQL 8 schema (30+ tables)
│   ├── seed.sql                            # Default admin, FAQs, contacts, AI settings
│   ├── postgresql_schema.sql               # PostgreSQL equivalent
│   ├── seeds/                              # Additional seed data
│   ├── migrations/                         # Versioned migrations (Flyway-style)
│   └── backups/                            # Auto backup target
│
├── docs/
│   ├── PROJECT_REPORT.md                   # Architecture, ERD, design deep-dive
│   └── SETUP_GUIDE.md                      # ← Start here for manual setup
│
├── nginx/                                  # Production reverse-proxy config
│
├── tests/                                  # Test scripts and fixtures
│
├── .github/                                # GitHub Actions workflows (CI/CD)
│
├── logs/                                   # Created at runtime
│
├── build-backend.ps1                       # Windows: builds the JAR (auto-installs JDK + Maven)
├── start-backend.ps1                       # Windows: runs the backend
├── start-admin.ps1                         # Windows: serves admin/ on :3001
│
├── docker-compose.yml                      # MySQL + backend + nginx, one command
├── Dockerfile                              # Backend container image
│
├── .env.example                            # Copy to .env, fill in
├── .gitignore
├── package.json
└── README.md                               # ← you are here
```

---

## Environment variables

Every setting is environment-driven. The `.env.example` file is the canonical reference.

| Variable | Default | Required | Description |
|---|---|---|---|
| `PORT` | `8080` | no | Backend HTTP port |
| `DB_TYPE` | `postgres` | no | `mysql` or `postgres` (informational) |
| `DB_URL` | `jdbc:postgresql://localhost:5432/ucc_chatbot_db` | **yes** | Full JDBC URL |
| `DB_USERNAME` | `postgres` | **yes** | DB user |
| `DB_PASSWORD` | (empty) | **yes** | DB password |
| `DB_DRIVER` | `org.postgresql.Driver` | **yes** | `com.mysql.cj.jdbc.Driver` for MySQL |
| `DB_DIALECT` | `org.hibernate.dialect.PostgreSQLDialect` | **yes** | `org.hibernate.dialect.MySQLDialect` for MySQL |
| `JWT_SECRET` | (empty) | **yes in prod** | ≥ 32 random bytes. Generate with `node -e "console.log(require('crypto').randomBytes(48).toString('base64'))"` |
| `JWT_EXPIRATION_MS` | `604800000` (7 days) | no | JWT access-token TTL |
| `ADMIN_EMAIL` | (empty) | first run | Default admin email, seeded automatically |
| `ADMIN_PASSWORD` | (empty) | first run | Default admin password |
| `ADMIN_NAME` | (empty) | first run | Default admin display name |
| `AI_API_KEY` | (empty) | no | OpenAI-compatible API key. Empty = static KB only |
| `AI_API_URL` | `https://api.openai.com/v1` | no | Any OpenAI-compatible endpoint |
| `AI_MODEL` | `gpt-4o-mini` | no | LLM model name |
| `AI_EMBEDDING_MODEL` | `text-embedding-3-small` | no | For future vector search |
| `CORS_ALLOWED_ORIGINS` | (Netlify + localhost) | no | Comma-separated origin list |
| `FRONTEND_URL` | Netlify deploy | no | Used in canonical links / OG tags |
| `ENCRYPTION_KEY` | default | no | 32-byte AES key for stored API keys |

---

## API reference

### Public endpoints (no auth)

| Method | Path | Description |
|---|---|---|
| `GET`  | `/api/health` | System health check |
| `POST` | `/api/chat` | Send a message, get a response |
| `GET`  | `/api/chat/history/{conversationId}` | Retrieve conversation history |
| `POST` | `/api/chat/feedback` | Submit thumbs-up/down + comment |
| `GET`  | `/api/faqs` | List published FAQs |
| `GET`  | `/api/contacts` | List UCC contact channels |
| `GET`  | `/api/courses` | List academic courses |
| `GET`  | `/api/services` | List UCC services |
| `GET`  | `/api/auth/me` | Current user info (requires JWT) |

### Admin endpoints (JWT required, `Authorization: Bearer <token>`)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/login` | Login → `{token, refreshToken, user}` |
| `POST` | `/api/auth/refresh` | Exchange refresh token for new access token |
| `POST` | `/api/auth/logout` | Revoke a refresh token |
| `POST` | `/api/auth/register` | Create a new user (ADMIN only) |
| `GET`  | `/api/admin/dashboard/stats` | Dashboard KPIs (16 metrics) |
| `GET`  | `/api/admin/dashboard/activity` | Recent activity feed (last 20) |
| `GET`  | `/api/admin/dashboard/health` | System health (DB, AI, timestamp) |
| `GET`  | `/api/admin/conversations` | List all conversations |
| `GET`  | `/api/admin/conversations/{id}` | Get conversation + all messages |
| `DELETE` | `/api/admin/conversations/{id}` | Delete a conversation |
| `GET`  | `/api/admin/knowledge` | List/filter knowledge documents |
| `POST` | `/api/admin/knowledge` | Create a knowledge document |
| `PUT`  | `/api/admin/knowledge/{id}` | Update a knowledge document |
| `DELETE` | `/api/admin/knowledge/{id}` | Delete a knowledge document |
| `POST` | `/api/admin/knowledge/{id}/approve` | Approve a PENDING document |
| `POST` | `/api/admin/knowledge/{id}/reject` | Reject a PENDING document |
| `POST` | `/api/admin/knowledge/upload` | Multipart file upload (auto-chunked) |
| `POST` | `/api/admin/knowledge/text` | Add raw text as a document |
| `POST` | `/api/admin/knowledge/reindex` | Rebuild chunk index for all docs |
| `GET`  | `/api/admin/knowledge/categories` | List knowledge categories |
| `GET`  | `/api/admin/ai/prompts` | List prompt templates |
| `POST` | `/api/admin/ai/prompts` | Create a prompt template |
| `PUT`  | `/api/admin/ai/prompts/{id}` | Update a prompt template |
| `DELETE` | `/api/admin/ai/prompts/{id}` | Delete a prompt template |
| `GET`  | `/api/admin/ai/settings` | List AI settings (key values masked) |
| `PUT`  | `/api/admin/ai/settings/{key}` | Update setting (encrypted if marked) |
| `GET`  | `/api/admin/ai/logs` | Recent AI logs |
| `POST` | `/api/admin/ai/test` | Test the AI with a sample message |
| `GET`  | `/api/admin/integrations` | List integrations |
| `POST` | `/api/admin/integrations` | Create an integration |
| `PUT`  | `/api/admin/integrations/{id}` | Update an integration |
| `DELETE` | `/api/admin/integrations/{id}` | Delete an integration |
| `POST` | `/api/admin/integrations/{id}/test` | Test integration connectivity |
| `GET`  | `/api/admin/integrations/prompts` | List prompt templates (legacy) |
| `POST` | `/api/admin/integrations/prompts` | Create prompt template (legacy) |
| `GET`  | `/api/admin/logs/ai` | AI request/response logs |
| `GET`  | `/api/admin/logs/system` | System event logs |
| `GET`  | `/api/admin/logs/audit` | Admin action audit trail |
| `GET`  | `/api/admin/logs/errors` | Error-only logs |
| `GET`  | `/api/admin/faqs` | All FAQs (including unpublished) |
| `POST` | `/api/admin/faqs` | Create an FAQ |
| `PUT`  | `/api/admin/faqs/{id}` | Update an FAQ |
| `DELETE` | `/api/admin/faqs/{id}` | Delete an FAQ |
| `GET`  | `/api/admin/faqs/contacts` | List UCC contacts |

### Example: chat request

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What are the fees for DCIT?",
    "conversationId": "user-abc-123",
    "language": "en"
  }'
```

Response:
```json
{
  "answer": "For the Diploma in Computing and Information Technology (DCIT), the total fee is TZS 3,020,000...",
  "language": "en",
  "conversationId": "user-abc-123",
  "sources": [
    {"title": "UCC Static Knowledge Base", "url": "https://ucc.co.tz/"}
  ],
  "confidence": 0.7,
  "escalationRequired": false
}
```

### Example: admin login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@ucc.co.tz","password":"YourPassword"}'
```

Response:
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "abc123-uuid-456",
  "user": {
    "id": "uuid",
    "email": "admin@ucc.co.tz",
    "fullName": "UCC Administrator",
    "role": "ADMIN",
    "isActive": true
  },
  "expiresIn": 86400
}
```

---

## Admin dashboard

The admin dashboard is a single-page app at `admin/dashboard.html` (login at `admin/index.html`).

### Tabs / modules

| Tab | Capabilities |
|---|---|
| **Dashboard** | 16 KPIs (conversations, messages, users, knowledge, AI logs, feedback, escalations) + recent activity feed + system health |
| **Conversations** | List all chats, view full transcript, delete |
| **Knowledge** | CRUD documents, categories, search, approve/reject, file upload (multipart), reindex all |
| **AI Training** | Prompt template CRUD, AI settings (encrypted key entry), AI log viewer, live "Test" the AI |
| **Integrations** | External integration CRUD, test connectivity, legacy prompt templates |
| **FAQs** | Full CRUD, category, priority, publish/draft |
| **Contacts** | View all UCC contact channels |
| **Logs** | AI / system / audit / error log viewers (100 each) |
| **Profile / Logout** | View current user, sign out |

### Configuration

Only one file to change before deploying the admin:
```js
// admin/js/admin-auth.js line 5-7
const ADMIN_CONFIG = {
  API_BASE_URL: "https://api.your-domain.com/api"
};
```

### Roles

| Role | Can read | Can write |
|---|---|---|
| `ADMIN` | everything | everything |
| `STAFF` | everything | knowledge, FAQs, AI training |
| `EDITOR` | everything | knowledge, FAQs only |
| `VIEWER` | everything | nothing |
| `USER` | (public endpoints) | (n/a) |

---

## Database schema

The system ships with two complete schemas:

- **`database/schema.sql`** — MySQL 8 (30+ tables, 50+ indexes, 4 stored procedures)
- **`database/postgresql_schema.sql`** — PostgreSQL 14+ equivalent (18 tables)

Both use **UUID primary keys** and the `utf8mb4_unicode_ci` collation (or `utf8` for PostgreSQL).

### Key tables

| Table | Purpose | Key columns |
|---|---|---|
| `users` | Admin/staff accounts | `id`, `email` (unique), `password_hash`, `role`, `is_active` |
| `refresh_tokens` | JWT refresh tokens | `user_id`, `token` (unique), `expires_at`, `revoked` |
| `roles`, `permissions` | RBAC | `name`, `description` |
| `conversations` | One per chat session | `id`, `session_id` (unique), `user_id`, `last_programme`, `last_intent` |
| `messages` | Individual chat messages | `conversation_id`, `role` (USER/ASSISTANT), `content`, `language`, `intent`, `confidence` |
| `knowledge_documents` | KB items | `title`, `content`, `category`, `approval_status`, `is_active`, `is_indexed`, `version` |
| `knowledge_chunks` | RAG chunks | `document_id`, `chunk_index`, `chunk_text`, `embedding_vector` |
| `knowledge_versions` | Edit history | `document_id`, `version_number`, `change_note` |
| `knowledge_categories` | KB taxonomy | `name`, `display_order`, `is_active` |
| `faqs` | Canned Q&A | `question`, `answer`, `category`, `priority`, `is_published` |
| `contacts` | Phone/email/address | `name`, `type`, `value`, `display_order` |
| `ai_prompts` | System prompt templates | `name` (unique), `type`, `content`, `category` |
| `ai_settings` | Runtime config | `key` (unique), `value` (encrypted), `is_encrypted` |
| `ai_logs` | Per-request AI log | `session_id`, `user_message`, `ai_response`, `confidence`, `status` |
| `feedback` | User feedback | `session_id`, `message_id`, `rating`, `comment` |
| `integrations` | External systems | `name`, `type`, `base_url`, `api_key` (encrypted), `webhook_url` |
| `escalations` | Human handoff | `conversation_id`, `reason`, `status`, `assigned_to` |
| `system_logs` | System events | `level`, `message`, `created_at` |
| `audit_logs` | Admin actions | `user_id`, `action`, `resource_type`, `resource_id` |
| `website_pages`, `website_sync_jobs` | Scrape cache | `url`, `content_hash`, `last_status` |
| `events`, `news` | CMS | `title`, `summary`, `published_at` |
| `ucc_services`, `courses` | Content | per UCC structure |

`Hibernate's ddl-auto=update` will auto-create the tables on first run. To use the pre-built schema instead, load it manually before starting the app.

---

## Deployment

### Backend — any Java 17 host

| Platform | Free tier | Setup |
|---|---|---|
| **Render** | 750 hrs/mo | [render.com](https://render.com) → New Web Service → connect repo |
| **Railway** | $5 credit | [railway.app](https://railway.app) → New Project → Deploy from repo |
| **Fly.io** | 3 VMs | `fly launch` then `fly deploy` |
| **Heroku** | Eco dyno | `heroku create` then `git push heroku main` |
| **Any Linux VM** | — | `java -jar ucc-chatbot-1.0.0.jar` behind nginx/Caddy |

Common settings:
- **Build command:** `cd backend && mvn clean package -DskipTests`
- **Start command:** `java -jar target/ucc-chatbot-1.0.0.jar`
- **Health check path:** `/api/health`
- **Port:** `${PORT:-8080}`

Set all env vars from the [Environment variables](#environment-variables) table.

### Admin dashboard — any static host

| Platform | Free tier | Setup |
|---|---|---|
| **Netlify** | 100 GB/mo | Drop the `admin/` folder at [app.netlify.com/drop](https://app.netlify.com/drop) |
| **Cloudflare Pages** | Unlimited | Connect repo, build cmd `echo`, publish `admin` |
| **Vercel** | 100 GB/mo | `vercel deploy admin --prod` |
| **S3 + CloudFront** | ~$1/mo | `aws s3 sync admin/ s3://bucket/ && cloudfront invalidate` |

**Before deploying**, edit `admin/js/admin-auth.js` and change `API_BASE_URL` to your backend URL.

### Frontend — already deployed

The public chatbot is at **https://agent-6a87a4d1bce5537b6d8d53a5--uccchatbot.netlify.app/**. To redeploy, update `frontend/js/chatbot.js` with the backend URL and drop the `frontend/` folder on any static host.

### All-in-one with Docker Compose

```bash
cp .env.example .env
# Edit .env
docker compose up -d
```

This starts MySQL 8 + the backend + nginx on a single host. Edit `nginx/conf.d/default.conf` to add your domain + TLS.

---

## Security

| Concern | Mitigation |
|---|---|
| **Password storage** | BCrypt with strength 10 (Spring Security default) |
| **API key storage** | AES-256 encryption (`EncryptionUtil`), 32-byte configurable key |
| **JWT secrets** | HS256, env-driven `JWT_SECRET` (must be ≥ 32 bytes in production) |
| **Auth bypass** | Spring Security + `@PreAuthorize` on every admin controller, role check on every `JwtAuthFilter` invocation |
| **CORS** | Whitelist of allowed origins, not `*` |
| **CSRF** | Disabled (stateless API), only POST/PUT/DELETE are not safe but require valid JWT |
| **SQL injection** | JPA parameterized queries everywhere; no string concatenation |
| **File upload** | 10 MB limit, content stored as text (not executed) |
| **Brute force** | Account lockout field exists (`locked_until`, `failed_login_count`) — not yet auto-enforced |
| **TLS** | Enforced by your reverse proxy (Caddy auto-TLS, or Let's Encrypt + nginx) |
| **Audit** | Every admin write is logged in `audit_logs` |
| **Secret management** | All secrets env-driven; never committed (`.env` in `.gitignore`) |

**Hardening checklist before going live:**
- [ ] `JWT_SECRET` is at least 48 random bytes
- [ ] `ADMIN_PASSWORD` is at least 12 characters, not the default
- [ ] TLS is enabled
- [ ] `CORS_ALLOWED_ORIGINS` is restricted to your real domains
- [ ] Database backups are running daily
- [ ] Uploads directory is backed up
- [ ] Logs are shipped off-host
- [ ] Rate limiting is enabled (nginx `limit_req` or CDN)

---

## Performance & cost

### Backend performance (single 1-vCPU VM, default settings)

| Metric | Typical |
|---|---|
| Cold start | 6–10 seconds |
| Warm chat response (static KB) | 50–200 ms |
| Warm chat response (with LLM call) | 1–3 seconds |
| Concurrent users | ~100 |
| Memory footprint | 300–500 MB |
| DB connections | 10 (HikariCP default) |
| JAR size | 60 MB |

### LLM cost (gpt-4o-mini)

| Scenario | Tokens in/out | Cost per chat |
|---|---|---|
| Static KB answers everything | 0 | $0 |
| KB + small LLM polish | 800 / 200 | $0.00018 |
| KB empty, full LLM | 1500 / 400 | $0.00027 |
| Local Ollama (no API cost) | n/a | $0 |

**10,000 chats ≈ $0.60–$2.70** on gpt-4o-mini. Local Ollama is free.

### Database growth

| What | Per 1000 records |
|---|---|
| Conversation | ~30 KB |
| Message (avg 200 chars) | ~50 KB |
| Knowledge document (avg 2 KB) | ~2 MB |
| AI log (per chat) | ~2 KB |
| System log | ~1 KB |

10,000 chats = ~80 MB. The DB stays under 1 GB for years of typical use.

---

## Verification (acceptance test)

After deploying, run through this in order:

```bash
# 1. Backend alive
curl https://api.your-domain.com/api/health
# Expected: {"status":"UP",...}

# 2. Admin login works
curl -X POST https://api.your-domain.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@ucc.co.tz","password":"..."}'
# Expected: {"success":true,"token":"...","user":{"role":"ADMIN",...}}

# 3. Public chat works
curl -X POST https://api.your-domain.com/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"What programmes do you offer?","conversationId":"verify-1"}'
# Expected: answer mentions DCIT, DBIT, CCIT, CBIT

# 4. Bilingual chat works
curl -X POST https://api.your-domain.com/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Habari, naomba kujua kuhusu DCIT","conversationId":"verify-2"}'
# Expected: Swahili response with DCIT details

# 5. Open admin dashboard
# Visit https://admin.your-domain.com and log in

# 6. Add a knowledge document in the admin UI
# Title: "Library hours"
# Content: "Monday-Friday 8am-9pm, Saturday 9am-5pm, closed Sunday"

# 7. Ask the chatbot about it
curl -X POST https://api.your-domain.com/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"What are the library hours?","conversationId":"verify-3"}'
# Expected: answer matches the document you just added

# 8. Admin token is required for /api/admin/*
curl https://api.your-domain.com/api/admin/dashboard/stats
# Expected: 401 Unauthorized

# 9. With token, admin endpoint works
TOKEN="<paste token from step 2>"
curl https://api.your-domain.com/api/admin/dashboard/stats \
  -H "Authorization: Bearer $TOKEN"
# Expected: JSON with stats
```

If all 9 pass, the system is working end-to-end.

---

## Documentation map

| Document | Purpose |
|---|---|
| **[README.md](README.md)** (this file) | Overview, quick start, API reference, deployment |
| **[docs/SETUP_GUIDE.md](docs/SETUP_GUIDE.md)** | Step-by-step manual setup, troubleshooting, maintenance runbook |
| **[docs/PROJECT_REPORT.md](docs/PROJECT_REPORT.md)** | Architecture deep-dive, ERD, design decisions, requirements, testing, roadmap |
| **[.env.example](.env.example)** | All configuration variables with explanations |
| **[database/schema.sql](database/schema.sql)** | Full MySQL schema with comments |
| **[database/seed.sql](database/seed.sql)** | Default data |

---

## Contributing

Internal use. For changes:

1. Create a feature branch: `git checkout -b feat/your-feature`
2. Make your changes
3. Build: `.\build-backend.ps1 -Package`
4. Run the acceptance test (§Verification above)
5. Commit: `git commit -m "feat: description"`
6. Push and open a PR

**Style guide:**
- Java: standard Spring Boot conventions, 4-space indent
- JavaScript: ES2020+, vanilla (no frameworks in admin)
- SQL: uppercase keywords, snake_case identifiers
- Documentation: Markdown, ATX headings, code fences with language hints

---

## License

Internal use — University of Dar es Salaam Computing Centre (UCC). All rights reserved.

---

**Project lead:** UCC IT Department
**Repository:** `ucc-chatbot-assistant`
**Live:** https://agent-6a87a4d1bce5537b6d8d53a5--uccchatbot.netlify.app/
**Last updated:** 2026-09-01
