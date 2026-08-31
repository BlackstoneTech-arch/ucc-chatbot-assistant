# UCC Chatbot Assistant

AI-powered customer-care assistant for the **University of Dar es Salaam Computing Centre (UCC)**. Production-ready, bilingual (English + Kiswahili), RAG-based, with a complete admin dashboard.

## Quick Start (Windows)

```powershell
# 1. Build backend (downloads Java + Maven on first run)
.\build-backend.ps1 -Package

# 2. Start backend (port 8081) - in one terminal
.\start-backend.ps1

# 3. Start admin dashboard (port 3001) - in another terminal
.\start-admin.ps1

# 4. Open admin dashboard
#    http://localhost:3001/
#    Default: admin@ucc.co.tz / Admin@123
```

## Project Structure

```
ucc-chatbot-assistant/
├── backend/                # Spring Boot 3.3.4 + Java 17
│   ├── src/main/java/com/ucc/chatbot/
│   │   ├── config/         # Security, CORS, async, JWT filter
│   │   ├── controller/     # REST controllers (public + admin)
│   │   ├── dto/            # Request/response DTOs
│   │   ├── model/          # JPA entities
│   │   ├── repository/     # Spring Data JPA repositories
│   │   ├── service/        # Business logic (interfaces + impls)
│   │   └── util/           # EncryptionUtil, etc.
│   └── src/main/resources/
│       └── application.properties
├── admin/                  # Admin dashboard (vanilla HTML/CSS/JS)
│   ├── dashboard.html      # Single-page admin UI
│   ├── js/                 # admin.js, admin-auth.js
│   ├── css/                # admin.css
│   └── netlify.toml
├── website/                # Public website (static)
├── database/               # schema.sql + seed.sql (MySQL)
├── docs/
│   └── PROJECT_REPORT.md   # Full project report
├── logs/                   # Runtime logs (auto-created)
├── build-backend.ps1       # Windows build script
├── start-backend.ps1       # Windows backend runner
├── start-admin.ps1         # Windows admin dashboard runner
└── README.md
```

## Architecture

```
┌────────────┐         ┌──────────────────────────────────┐
│  Browser   │────────▶│  Spring Boot 3.3.4 (port 8081)    │
│ (public)   │  /api/* │                                    │
└────────────┘         │  ┌────────────────────────────┐   │
                       │  │  ChatController            │   │
┌────────────┐         │  │  ├─ QueryUnderstandingLayer│   │
│  Admin UI  │────────▶│  │  ├─ AIService (RAG)        │   │
│ (port 3001)│  /api/* │  │  └─ KnowledgeService       │   │
└────────────┘         │  └────────────────────────────┘   │
                       │  ┌────────────────────────────┐   │
                       │  │  AuthController + JWT      │   │
                       │  │  KnowledgeController       │   │
                       │  │  AITrainingController      │   │
                       │  │  IntegrationController     │   │
                       │  │  DashboardController       │   │
                       │  │  LogsController            │   │
                       │  │  FaqAdminController        │   │
                       │  └────────────────────────────┘   │
                       │                                    │
                       │  MySQL / PostgreSQL                │
                       └──────────────────────────────────┘
```

## Key Features

- **Query Understanding Layer** — bilingual NLP (English + Kiswahili), synonym/typo handling, intent classification, entity extraction
- **RAG pipeline** — knowledge base retrieval + LLM fallback (OpenAI-compatible)
- **JWT auth + refresh tokens**, RBAC (ADMIN, STAFF, EDITOR, VIEWER)
- **Website sync** — async scraper that indexes https://ucc.co.tz/ into the knowledge base
- **Admin dashboard** — knowledge base, AI training, integrations, logs, FAQs, contacts, users
- **AEC encryption** for stored API keys
- **Full audit logging** + system logs + AI logs
- **CORS** for Netlify + localhost (3000/3001/5500/8080/8081)
- **MySQL 8 + PostgreSQL** support (configurable)

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `PORT` | `8080` | Backend HTTP port |
| `DB_TYPE` | `postgres` | `mysql` or `postgres` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/ucc_chatbot_db` | JDBC URL |
| `DB_USERNAME` | `postgres` | DB user |
| `DB_PASSWORD` | (empty) | DB password |
| `DB_DRIVER` | `org.postgresql.Driver` | JDBC driver class |
| `DB_DIALECT` | `org.hibernate.dialect.PostgreSQLDialect` | Hibernate dialect |
| `JWT_SECRET` | (empty) | **Required in production** (>= 32 bytes) |
| `JWT_EXPIRATION_MS` | `604800000` | JWT TTL (default 7d) |
| `ADMIN_EMAIL` | (empty) | Default admin email |
| `ADMIN_PASSWORD` | (empty) | Default admin password |
| `ADMIN_NAME` | (empty) | Default admin name |
| `AI_API_KEY` | (empty) | OpenAI-compatible API key |
| `AI_API_URL` | `https://api.openai.com/v1` | API base URL |
| `AI_MODEL` | `gpt-4o-mini` | Default LLM model |
| `CORS_ALLOWED_ORIGINS` | (Netlify + localhost) | Comma-separated CORS origins |
| `FRONTEND_URL` | Netlify deploy URL | Public frontend |

## API Reference (Public)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/chat` | Send a message and get a response |
| `GET`  | `/api/chat/history/{sessionId}` | Get chat history |
| `POST` | `/api/chat/feedback` | Submit feedback |
| `GET`  | `/api/auth/me` | Get current user (requires JWT) |
| `GET`  | `/api/faqs` | List published FAQs |
| `GET`  | `/api/contacts` | List UCC contacts |
| `GET`  | `/api/courses` | List courses |
| `GET`  | `/api/services` | List services |
| `GET`  | `/api/health` | Health check |

## API Reference (Admin)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/login` | Admin login (returns JWT + refresh) |
| `POST` | `/api/auth/refresh` | Refresh access token |
| `POST` | `/api/auth/logout` | Revoke refresh token |
| `GET`  | `/api/admin/dashboard/stats` | Dashboard KPIs |
| `GET`  | `/api/admin/dashboard/activity` | Recent activity feed |
| `GET`  | `/api/admin/dashboard/health` | System health |
| `GET`  | `/api/admin/conversations` | List conversations |
| `GET`  | `/api/admin/conversations/{id}` | Get conversation + messages |
| `GET`  | `/api/admin/knowledge` | List/filter knowledge docs |
| `POST` | `/api/admin/knowledge` | Create knowledge doc |
| `PUT`  | `/api/admin/knowledge/{id}` | Update knowledge doc |
| `DELETE` | `/api/admin/knowledge/{id}` | Delete knowledge doc |
| `POST` | `/api/admin/knowledge/{id}/approve` | Approve a pending doc |
| `POST` | `/api/admin/knowledge/{id}/reject` | Reject a pending doc |
| `POST` | `/api/admin/knowledge/upload` | Upload a file (multipart) |
| `POST` | `/api/admin/knowledge/text` | Add raw text |
| `POST` | `/api/admin/knowledge/reindex` | Rebuild chunk index |
| `GET`  | `/api/admin/ai/prompts` | List prompt templates |
| `POST` | `/api/admin/ai/prompts` | Create prompt template |
| `PUT`  | `/api/admin/ai/prompts/{id}` | Update prompt |
| `DELETE` | `/api/admin/ai/prompts/{id}` | Delete prompt |
| `GET`  | `/api/admin/ai/settings` | List AI settings (key values masked) |
| `PUT`  | `/api/admin/ai/settings/{key}` | Update setting (encrypted if `is_encrypted`) |
| `GET`  | `/api/admin/ai/logs` | Recent AI logs |
| `POST` | `/api/admin/ai/test` | Test the AI with a message |
| `GET`  | `/api/admin/integrations` | List integrations |
| `POST` | `/api/admin/integrations` | Create integration |
| `PUT`  | `/api/admin/integrations/{id}` | Update integration |
| `DELETE` | `/api/admin/integrations/{id}` | Delete integration |
| `POST` | `/api/admin/integrations/{id}/test` | Test integration connectivity |
| `GET`  | `/api/admin/logs/ai` | AI logs |
| `GET`  | `/api/admin/logs/system` | System logs |
| `GET`  | `/api/admin/logs/audit` | Audit logs |
| `GET`  | `/api/admin/logs/errors` | Error logs only |
| `GET`  | `/api/admin/faqs` | All FAQs |
| `POST` | `/api/admin/faqs` | Create FAQ |
| `PUT`  | `/api/admin/faqs/{id}` | Update FAQ |
| `DELETE` | `/api/admin/faqs/{id}` | Delete FAQ |
| `GET`  | `/api/admin/faqs/contacts` | List UCC contacts |

## Database Setup (MySQL)

```sql
CREATE DATABASE ucc_chatbot_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Run schema + seed (optional):
```bash
mysql -u root -p ucc_chatbot_db < database/schema.sql
mysql -u root -p ucc_chatbot_db < database/seed.sql
```

Or set `spring.jpa.hibernate.ddl-auto=update` (default) to let Hibernate auto-create tables.

## Deployment

- **Backend** — Render, Railway, Fly.io, or any Java 17 host. Set all env vars above. Build command: `mvn clean package -DskipTests`. Start command: `java -jar target/ucc-chatbot-1.0.0.jar`
- **Admin dashboard** — Netlify. Build command: `echo "no build"`. Publish directory: `admin`. Set env var `API_BASE_URL` in `admin/js/admin-auth.js` to your backend URL.
- **Frontend** — already deployed at https://agent-6a87a4d1bce5537b6d8d53a5--uccchatbot.netlify.app/

## License

Internal use — University of Dar es Salaam Computing Centre.
