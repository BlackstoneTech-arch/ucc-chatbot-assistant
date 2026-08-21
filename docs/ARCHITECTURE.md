# System Architecture

## Overview

The UCC Chatbot Assistant uses a modern, scalable architecture designed for accuracy, security, and maintainability.

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │   Frontend   │  │    Admin     │  │   Mobile/Tablet      │  │
│  │  (React/TS)  │  │ (React/TS)   │  │   Responsive Web     │  │
│  │  Port: 3000  │  │ Port: 3001   │  │                      │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘  │
└─────────┼─────────────────┼─────────────────────┼──────────────┘
          │                 │                     │
          └─────────────────┼─────────────────────┘
                            │ HTTPS
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Backend API Layer                          │
│                 Node.js + Express + TypeScript                  │
│                         Port: 5000                              │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌─────────────┐  │
│  │   Chat     │ │    Auth    │ │   Admin    │ │   Health     │  │
│  │  Routes    │ │  Routes    │ │  Routes    │ │   Check      │  │
│  └─────┬──────┘ └─────┬──────┘ └─────┬──────┘ └──────┬──────┘  │
│        │              │              │                │         │
│  ┌─────▼──────┐ ┌─────▼──────┐ ┌─────▼──────┐ ┌──────▼──────┐  │
│  │ Controllers│ │Controllers │ │Controllers │ │   Middle    │  │
│  │            │ │            │ │            │ │   ware      │  │
│  └─────┬──────┘ └─────┬──────┘ └─────┬──────┘ └─────────────┘  │
│        │              │              │                         │
│  ┌─────▼──────────────────▼──────┐                             │
│  │       Services Layer          │                             │
│  │  ┌────────────┐ ┌──────────┐ │                             │
│  │  │    AI      │ │    RAG    │ │                             │
│  │  │  Service   │ │  Service  │ │                             │
│  │  └────────────┘ └──────────┘ │                             │
│  └──────────────────────────────┘                             │
└───────────────────────────────┬───────────────────────────────┘
                                │
                ┌───────────────┼───────────────┐
                │               │               │
                ▼               ▼               ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│   PostgreSQL     │  │   AI/LLM API     │  │   File Storage   │
│   + pgvector     │  │   (OpenAI)       │  │   (Uploads)      │
│                  │  │                  │  │                  │
│  ┌────────────┐  │  │                  │  │                  │
│  │  Relational│  │  │                  │  │                  │
│  │   Tables   │  │  │                  │  │                  │
│  ├────────────┤  │  │                  │  │                  │
│  │  Vectors   │  │  │                  │  │                  │
│  │  (pgvector)│  │  │                  │  │                  │
│  └────────────┘  │  │                  │  │                  │
└──────────────────┘  └──────────────────┘  └──────────────────┘
```

## Layer Descriptions

### Client Layer
- **Frontend:** Public-facing chat interface and landing page
- **Admin:** Knowledge management dashboard for administrators
- **Responsive:** Works on desktop, tablet, and mobile devices

### Backend API Layer
- **Express.js:** HTTP server handling all API requests
- **Routes:** Organized endpoint groups (chat, auth, admin)
- **Controllers:** Request validation and response formatting
- **Middleware:** Authentication, rate limiting, security headers, CORS

### Services Layer
- **ChatService:** Orchestrates the RAG pipeline
- **AIService:** LLM integration for intent classification and response generation
- **KnowledgeBase:** Document retrieval, hybrid search, document management

### Data Layer
- **PostgreSQL:** Primary database for structured data
- **pgvector:** Vector extension for semantic search
- **File Storage:** Uploaded documents and knowledge base files

## Data Flow

### Chat Request Flow

```
User Question
    │
    ▼
┌─────────────────────────────────────┐
│  Chat Controller                    │
│  - Validate input                   │
│  - Rate limit check                 │
│  - Create session                   │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│  Chat Service                       │
│  - Intent classification            │
│  - Entity extraction                │
│  - Context retrieval                │
└─────────────────────────────────────┘
    │
    ├──────────────────┐
    ▼                  ▼
┌─────────────┐  ┌─────────────┐
│ Intent/Entity│  │   RAG       │
│   Classifier │  │  Retrieval  │
│   (AI/LLM)   │  │  (Hybrid)   │
└──────┬──────┘  └──────┬──────┘
       │                 │
       ▼                 ▼
┌─────────────────────────────────────┐
│  Response Generation                │
│  - Combine context + system rules   │
│  - Generate grounded response       │
│  - Add source attribution           │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│  Validation & Response              │
│  - Check hallucination risk         │
│  - Verify source authority          │
│  - Return to user                   │
└─────────────────────────────────────┘
```

### Knowledge Ingestion Flow

```
Document (PDF/DOCX/TXT/MD/HTML)
    │
    ▼
┌─────────────────────────────────────┐
│  Document Processing                │
│  - Extract text                     │
│  - Clean formatting                 │
│  - Chunk into segments              │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│  Metadata Attachment                │
│  - Title, category, year            │
│  - Source, status, version          │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│  Database Storage                   │
│  - Store document record            │
│  - Store chunks                     │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│  Embedding Generation               │
│  - Generate vector embeddings       │
│  - Store in pgvector                │
└─────────────────────────────────────┘
```

## Design Principles

1. **Accuracy First:** Official UCC information always takes precedence over AI generation
2. **Modular Architecture:** Clear separation of concerns across layers
3. **Security by Default:** Authentication, authorization, and input validation at every layer
4. **Scalability:** Stateless backend, connection pooling, indexed queries
5. **Maintainability:** TypeScript, clear naming, comprehensive documentation
6. **Observability:** Request tracking, logging, analytics, audit trails

## Technology Choices

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| Frontend | React + TypeScript | Type safety, large ecosystem, component reusability |
| Styling | Tailwind CSS | Rapid development, consistent design system |
| Backend | Node.js + Express | JavaScript/TypeScript unification, fast development |
| Database | PostgreSQL + pgvector | ACID compliance, vector search, mature ecosystem |
| AI | OpenAI-compatible API | Industry standard, easy provider switching |
| Testing | Vitest | Fast, modern, TypeScript-native |
