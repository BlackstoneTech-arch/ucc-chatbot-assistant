# UCC Chatbot Assistant - Project Report

**Document Version:** 1.0  
**Date:** August 2026  
**Prepared for:** University of Dar es Salaam Computing Centre (UCC)  
**System:** UCC Chatbot Assistant

---

## Executive Summary

The UCC Chatbot Assistant is an intelligent, multilingual conversational AI system developed for the University of Dar es Salaam Computing Centre (UCC). The system provides 24/7 automated customer-care support to students, prospective students, and staff, handling inquiries about academic programmes, admissions, fees, registration, ICT services, and general university information in both English and Kiswahili.

The system features a sophisticated Query Understanding Layer that normalizes informal language, detects user intent, extracts entities, and performs semantic query expansion before retrieving verified information from the knowledge base. An AI-powered response generation layer then produces accurate, contextually appropriate responses in the user's preferred language.

A comprehensive admin dashboard provides full system management capabilities including knowledge base management, AI training and prompt engineering, API integration management, and real-time system monitoring with error detection and logging.

---

## 1. Introduction

### 1.1 Background

The University of Dar es Salaam Computing Centre (UCC) receives a high volume of repetitive inquiries regarding academic programmes, admission procedures, fees, registration processes, and general information. Managing these inquiries through traditional channels requires significant human resources and often results in delayed responses during peak periods.

### 1.2 Problem Statement

- High volume of repetitive student inquiries
- Limited staff availability for 24/7 support
- Inconsistent response quality across different staff members
- Language barriers (English vs. Kiswahili)
- Slow response times during peak admission periods
- Difficulty tracking and analyzing common inquiry patterns

### 1.3 Objectives

1. Develop an AI-powered chatbot capable of handling 80% of common student inquiries
2. Provide multilingual support (English and Kiswahili)
3. Integrate with UCC's existing systems and knowledge base
4. Build a comprehensive admin dashboard for system management
5. Ensure accurate, verified responses from official UCC sources
6. Reduce response time from hours to seconds
7. Provide analytics and insights for administrative decision-making

### 1.4 Scope

**In Scope:**
- Academic programme information (DCIT, DBIT, CCIT, CBIT)
- Admission procedures and requirements
- Fee structures and payment information
- Registration processes
- ICT support services
- Contact information and locations
- Professional and short courses
- Student services

**Out of Scope:**
- Personalized academic advising
- Grade management
- Financial transactions
- Third-party system integrations (future enhancement)

---

## 2. System Analysis

### 2.1 Stakeholder Analysis

| Stakeholder | Role | Needs |
|-------------|------|-------|
| Students | Primary users | Quick answers to programme, fee, and admission questions |
| Prospective Students | Secondary users | Information about programmes and application process |
| UCC Staff | Administrators | Easy management of knowledge base and system monitoring |
| IT Administrators | Technical managers | API management, error tracking, system health monitoring |
| Management | Decision makers | Analytics, reports, and system performance metrics |

### 2.2 Functional Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-01 | Chat interface for user queries | High |
| FR-02 | Multilingual support (English/Kiswahili) | High |
| FR-03 | Intent classification and entity extraction | High |
| FR-04 | Knowledge base retrieval | High |
| FR-05 | AI-powered response generation | High |
| FR-06 | Conversation context maintenance | Medium |
| FR-07 | Admin authentication and authorization | High |
| FR-08 | Knowledge base CRUD operations | High |
| FR-09 | File upload for knowledge documents | Medium |
| FR-10 | FAQ management | Medium |
| FR-11 | Conversation history viewing | Medium |
| FR-12 | Analytics and reporting | Medium |
| FR-13 | System error logging | High |
| FR-14 | API integration management | Medium |
| FR-15 | AI prompt template management | Medium |

### 2.3 Non-Functional Requirements

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-01 | Response time | < 3 seconds |
| NFR-02 | System availability | 99.5% |
| NFR-03 | Concurrent users | 100+ |
| NFR-04 | Data accuracy | Verified sources only |
| NFR-05 | Language detection accuracy | > 95% |
| NFR-06 | Intent classification accuracy | > 90% |
| NFR-07 | Security | JWT + BCrypt + RBAC |
| NFR-08 | Scalability | Horizontal scaling ready |

---

## 3. System Design

### 3.1 Architecture Overview

The system follows a layered architecture pattern:

1. **Presentation Layer:** Public frontend and admin dashboard
2. **API Layer:** REST controllers with JWT authentication
3. **Business Logic Layer:** Services for chat, AI, knowledge base, conversations
4. **Query Understanding Layer:** NLP pipeline for message processing
5. **Data Access Layer:** JPA repositories with MySQL
6. **Integration Layer:** LLM API client, file processing utilities

### 3.2 Query Understanding Layer Design

```
Input: "ada ngap dbit?"
  │
  ├─> Language Detection
  │   Input: "ada ngap dbit?"
  │   Output: sw
  │
  ├─> Normalization
  │   Input: "ada ngap dbit?"
  │   Step 1: Lowercase → "ada ngap dbit?"
  │   Step 2: Informal corrections → "ada ngapi dbit?"
  │   Step 3: Typo corrections → "ada ngapi dbit?"
  │   Output: "ada ngapi dbit?"
  │
  ├─> Entity Extraction
  │   Input: "ada ngapi dbit?"
  │   Output: { programme: "DBIT" }
  │
  ├─> Concept Detection
  │   Input: "ada ngapi dbit?"
  │   Output: ["FEES"]
  │
  ├─> Intent Classification
  │   Input: entities={programme: DBIT}, concepts=[FEES]
  │   Output: PROGRAMME_FEE_QUERY
  │
  ├─> Canonical Query Generation
  │   Output: "What are the official fees for the Diploma in Business Information Technology (DBIT) programme?"
  │
  └─> Query Expansion
      Output: [
        "DBIT fees",
        "Diploma in Business Information Technology fees",
        "DBIT tuition fees",
        "DBIT total fee"
      ]
```

### 3.3 Database Design

The database schema consists of 8 main tables:

- **users** - Admin and staff accounts
- **conversations** - Chat sessions with context tracking
- **messages** - Individual chat messages
- **knowledge_documents** - Structured knowledge base entries
- **faqs** - Frequently asked questions
- **feedback** - User ratings and comments
- **escalations** - Human escalation requests
- **audit_logs** - Admin action tracking

### 3.4 Security Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │────▶│   API GW    │────▶│  Backend    │
│  (Browser)  │     │  (Spring)   │     │ (Spring)    │
└─────────────┘     └─────────────┘     └─────────────┘
       │                   │                   │
       │              ┌────┴────┐              │
       │              │  Auth   │              │
       │              │ Filter  │              │
       │              └────┬────┘              │
       │                   │                   │
       │              ┌────┴────┐              │
       │              │  JWT    │              │
       │              │ Validate│              │
       │              └────┬────┘              │
       │                   │                   │
       │              ┌────┴────┐              │
       │              │  RBAC   │              │
       │              │  Check  │              │
       │              └────┬────┘              │
       │                   │                   │
       │              ┌────┴────┐              │
       │              │  Input  │              │
       │              │Validate │              │
       │              └─────────┘              │
       │                                       │
       │                              ┌────────┴────────┐
       │                              │  Audit Log      │
       │                              │  (All Actions)  │
       │                              └─────────────────┘
```

---

## 4. Implementation Details

### 4.1 Query Understanding Service

The `QueryUnderstandingServiceImpl` implements the complete NLP pipeline:

**Key Features:**
- Language detection using keyword matching and indicator word counting
- Greeting/farewell/thank-you/help normalization with direct responses
- Informal Kiswahili normalization (e.g., "ada ngap" → "ada ngapi")
- Typo correction with word-boundary safety (regex `\b` boundaries)
- Entity extraction for programme codes (DBIT, DCIT, CCIT, CBIT)
- Concept synonym mapping for 6 major concept categories
- Intent classification based on entity + concept combinations
- Canonical query generation in English for retrieval
- Semantic query expansion for improved recall

**Code Location:** `backend/src/main/java/com/ucc/chatbot/service/impl/QueryUnderstandingServiceImpl.java`

### 4.2 AI Service

The `AIServiceImpl` handles response generation:

**Key Features:**
- Static knowledge base for verified UCC information
- LLM integration with OpenAI-compatible API
- Context-aware response generation
- Conversation context injection
- Language detection and response localization
- Graceful fallback for API failures

**Code Location:** `backend/src/main/java/com/ucc/chatbot/service/impl/AIServiceImpl.java`

### 4.3 Chat Service

The `ChatServiceImpl` orchestrates the request flow:

**Key Features:**
- Query understanding integration
- Conversation context management
- Retrieval context building
- Direct response handling for greetings/farewells
- AI response generation for knowledge queries
- Message persistence

**Code Location:** `backend/src/main/java/com/ucc/chatbot/service/impl/ChatServiceImpl.java`

### 4.4 Knowledge Service

Handles knowledge base document management:

**Key Features:**
- CRUD operations for knowledge documents
- File upload and text extraction
- Document search and filtering
- Approval workflow support
- Version tracking

### 4.5 Admin Dashboard

A single-page application (SPA) with the following modules:

**Dashboard Modules:**
1. **Overview** - Key metrics and system health
2. **Knowledge Base** - Document management with file upload
3. **AI Training** - Prompt template management and testing
4. **API Integrations** - External API management
5. **Conversations** - Chat history and monitoring
6. **System Logs** - Error detection and log viewing
7. **FAQs** - FAQ management

---

## 5. Testing

### 5.1 Unit Tests

The backend includes unit tests for:
- Query understanding service
- Language detection
- Intent classification
- Entity extraction
- Knowledge service operations

### 5.2 Integration Tests

API endpoint testing for:
- Chat endpoint with various query types
- Authentication flow
- Knowledge base CRUD operations
- Admin dashboard data loading

### 5.3 Manual Testing Results

| Test Case | Input | Expected Output | Status |
|-----------|-------|-----------------|--------|
| Greeting (English) | "hello" | Greeting response in English | PASS |
| Greeting (Swahili) | "mambo" | Greeting response in Swahili | PASS |
| Farewell (Swahili) | "kwaheri" | Farewell response in Swahili | PASS |
| Programme Query | "What programmes does UCC offer?" | List of programmes | PASS |
| Fee Query (informal) | "ada ngap dbit?" | DBIT fee details | PASS |
| Mixed Language | "How much ni ada ya DBIT?" | DBIT fee details in Swahili | PASS |
| Entry Requirements | "vigezo vya DCIT" | DCIT entry requirements | PASS |
| Duration Query | "DBIT inachukua miaka mingapi?" | DBIT duration | PASS |
| Contact Query | "how can I contact UCC?" | Contact details | PASS |
| Context Follow-up | "ada je?" (after DBIT query) | DBIT fee details | PASS |

---

## 6. Deployment

### 6.1 Deployment Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Production Environment                 │
│                                                          │
│  ┌──────────────┐    ┌──────────────┐   ┌────────────┐ │
│  │   Frontend   │    │   Admin      │   │  Backend   │ │
│  │  (Netlify)   │    │  Dashboard   │   │ (Tomcat)   │ │
│  │  Port 3000   │    │  Port 3001   │   │ Port 8081  │ │
│  └──────────────┘    └──────────────┘   └─────┬──────┘ │
│                                                  │        │
│  ┌──────────────────────────────────────────────▼──────┐ │
│  │                    MySQL Database                    │ │
│  │                  (RDS / Dedicated)                   │ │
│  └──────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### 6.2 Environment Configuration

**Production Environment Variables:**
- `SPRING_PROFILES_ACTIVE=prod`
- `AI_API_KEY` - Production LLM API key
- `JWT_SECRET` - Strong production secret
- `DB_URL` - Production database URL
- `CORS_ALLOWED_ORIGINS` - Production frontend URLs

### 6.3 CI/CD Pipeline

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│   Git    │───▶│   CI/    │───▶│   Test  │───▶│  Deploy  │
│  Commit  │    │   Build  │    │  Stage  │    │  Prod    │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
```

---

## 7. Future Enhancements

### 7.1 Planned Features

1. **Advanced NLP Pipeline**
   - Transformer-based intent classification
   - Named Entity Recognition (NER) for courses and staff names
   - Sentiment analysis for escalation triggers
   - Multi-language support expansion

2. **Enhanced Knowledge Base**
   - Vector database integration (Pinecone/Weaviate)
   - Semantic search with embeddings
   - Automatic document ingestion from UCC website
   - Document versioning and rollback

3. **Advanced Admin Features**
   - Real-time conversation monitoring
   - Live chat takeover by human agents
   - Advanced analytics with predictive insights
   - Custom report builder

4. **Integration Expansion**
   - Student Information System (SIS) integration
   - Learning Management System (LMS) integration
   - Payment gateway integration
   - SMS/WhatsApp notification system
   - Email campaign management

5. **AI Improvements**
   - Fine-tuned UCC-specific language model
   - Multi-modal support (image/document analysis)
   - Voice input/output support
   - Proactive notification system

### 7.2 Technical Debt

1. Migrate from static knowledge base to vector database
2. Implement comprehensive unit and integration test suite
3. Add rate limiting and request throttling
4. Implement request/response caching
5. Add comprehensive API documentation (OpenAPI/Swagger)

---

## 8. Conclusion

The UCC Chatbot Assistant successfully addresses the core challenges of providing timely, accurate, and multilingual support to the UCC community. The system's sophisticated Query Understanding Layer ensures that user queries are properly interpreted regardless of language, formality, or phrasing variations.

The comprehensive admin dashboard empowers UCC staff to manage the knowledge base, train the AI, monitor system health, and analyze conversation patterns without requiring technical expertise. The modular architecture ensures that the system can be extended with new features, integrations, and AI capabilities as UCC's needs evolve.

The deployment of this system is expected to:
- Reduce inquiry response time from hours to seconds
- Handle 80% of common inquiries automatically
- Provide 24/7 availability for student support
- Generate valuable analytics for administrative planning
- Reduce operational costs associated with manual inquiry handling

---

## Appendix A: Technology Decisions

| Decision | Options Considered | Selected | Rationale |
|----------|-------------------|----------|-----------|
| Backend Framework | Spring Boot, Django, Express | Spring Boot | Enterprise-grade, strong ecosystem, Java expertise |
| Database | MySQL, PostgreSQL, MongoDB | MySQL | Existing infrastructure, ACID compliance |
| Frontend | React, Vue, Vanilla JS | Vanilla JS | No build step required, fast deployment |
| AI Model | GPT-4, Claude, Local LLM | OpenAI-compatible API | Cost-effective, easy to swap providers |
| Authentication | Session, JWT, OAuth2 | JWT | Stateless, scalable, mobile-friendly |
| File Processing | Apache Tika, custom parsers | Apache PDFBox + POI | Lightweight, sufficient for needs |

## Appendix B: Team Roles

| Role | Responsibilities |
|------|-----------------|
| Project Manager | Overall coordination, requirements gathering |
| Backend Developer | Java/Spring Boot development, API design |
| NLP Engineer | Query understanding layer, AI integration |
| Frontend Developer | Public website and admin dashboard |
| Database Administrator | Schema design, optimization, backups |
| DevOps Engineer | Deployment, CI/CD, monitoring |

## Appendix C: Timeline

| Phase | Duration | Deliverables |
|-------|----------|--------------|
| Requirements | 2 weeks | Requirements document, user stories |
| Design | 2 weeks | Architecture diagrams, database schema |
| Backend Development | 6 weeks | API endpoints, services, database |
| Frontend Development | 4 weeks | Public site, admin dashboard |
| AI/NLP Development | 4 weeks | Query understanding, AI integration |
| Testing | 2 weeks | Test cases, bug fixes |
| Deployment | 1 week | Production setup, documentation |
| **Total** | **21 weeks** | **Production-ready system** |

---

*Document prepared by the UCC Chatbot Assistant Development Team*
