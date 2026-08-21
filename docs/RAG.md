# RAG Pipeline Documentation

## Overview

The Retrieval-Augmented Generation (RAG) pipeline is the core intelligence system of the UCC Chatbot Assistant. It retrieves relevant UCC information from the knowledge base and uses it to generate accurate, sourced responses.

## Pipeline Architecture

```
┌─────────────┐
│ User Query  │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────┐
│  1. Query Processing                │
│  - Input validation                 │
│  - Length check                     │
│  - Sanitization                     │
└─────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  2. Intent Classification           │
│  - LLM-based classification         │
│  - Confidence scoring               │
│  - Entity extraction                │
└─────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  3. Knowledge Retrieval             │
│  - Hybrid search (semantic + keyword)│
│  - Metadata filtering               │
│  - Top-K selection                  │
└─────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  4. Reranking                       │
│  - Relevance scoring                │
│  - Source authority weighting       │
│  - Academic year preference         │
└─────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  5. Context Assembly                │
│  - Combine retrieved chunks         │
│  - Add metadata                     │
│  - Format for LLM                   │
└─────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  6. Response Generation             │
│  - System prompt injection          │
│  - Context injection                │
│  - LLM completion                   │
└─────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  7. Validation                      │
│  - Hallucination check              │
│  - Source verification              │
│  - Escalation decision              │
└─────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  8. Response Delivery               │
│  - Format response                  │
│  - Add sources                      │
│  - Return to user                   │
└─────────────────────────────────────┘
```

## Component Details

### 1. Query Processing

**File:** `backend/src/services/chatService.ts`

Validates and prepares the user query:
- Checks for empty or invalid input
- Validates message length (max 2000 characters)
- Sanitizes input to prevent injection attacks

### 2. Intent Classification

**File:** `backend/src/ai/aiService.ts`

Classifies user intent using an LLM:

```typescript
async classifyIntent(question: string): Promise<{
  intent: string;
  confidence: number;
  entities: Record<string, any>;
}>
```

**Supported Intents:**
- `greeting`
- `about_ucc`
- `admission`
- `application_process`
- `application_deadline`
- `application_fee`
- `entry_requirements`
- `programme_information`
- `course_information`
- `course_duration`
- `tuition_fee`
- `other_fees`
- `payment_information`
- `registration`
- `academic_calendar`
- `examination`
- `graduation`
- `accommodation`
- `student_services`
- `ict_support`
- `professional_training`
- `software_services`
- `it_infrastructure`
- `consulting`
- `campus_information`
- `contact_information`
- `news`
- `events`
- `complaint`
- `feedback`
- `technical_problem`
- `human_support`
- `unknown`

**Entities Extracted:**
- `programme`
- `course`
- `course_code`
- `campus`
- `academic_year`
- `intake`
- `fee_type`
- `department`
- `date`

### 3. Knowledge Retrieval

**File:** `backend/src/rag/knowledgeBase.ts`

#### Hybrid Search

Combines semantic and keyword search:

```typescript
async hybridSearch(
  query: string,
  topK: number = 5,
  filters?: Record<string, any>
): Promise<DocumentChunk[]>
```

**Semantic Search:**
- Uses vector embeddings to find conceptually similar content
- Leverages pgvector for efficient similarity search
- Configurable via `TOP_K_RESULTS` environment variable

**Keyword Search:**
- Uses PostgreSQL full-text search capabilities
- Matches exact terms, course codes, fees, dates
- Case-insensitive pattern matching

**Metadata Filtering:**
- Filter by `category`
- Filter by `academic_year`
- Filter by `status` (ACTIVE, ARCHIVED, etc.)

### 4. Reranking

Ranks retrieved results by:
1. **Similarity Score** (60%) - Semantic relevance
2. **Source Authority** (20%) - Official vs. general
3. **Academic Year** (10%) - Prefer current year
4. **Effective Date** (5%) - Prefer recent documents
5. **Category Relevance** (5%) - Match to intent category

### 5. Context Assembly

Formats retrieved chunks for the LLM:

```
UCC Knowledge Base Context:

[1] First relevant chunk text...
[2] Second relevant chunk text...
[3] Third relevant chunk text...

---
Source: [document title] - [category]
```

### 6. Response Generation

**File:** `backend/src/ai/aiService.ts`

Uses the LLM to generate a response based on:
- System prompt (identity, rules, tone)
- Retrieved knowledge base context
- Conversation history (last 10 messages)

**System Prompt Highlights:**
- Identity: UCC Chatbot Assistant
- Tone: Professional, friendly, clear
- Critical rules:
  - Never invent UCC information
  - Cite sources
  - Respect academic year separation
  - Escalate when uncertain

### 7. Validation

Before returning a response, the system checks:
- Is the information supported by retrieved context?
- Is the source authoritative?
- Is it current and applicable?
- Did the AI invent anything?
- Does it need human escalation?

If validation fails, the system returns an escalation message.

### 8. Response Delivery

Final response includes:
- Generated answer
- Source citations
- Intent classification
- Confidence score
- Escalation flag

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `CHUNK_SIZE` | 1000 | Maximum characters per chunk |
| `CHUNK_OVERLAP` | 200 | Overlap between chunks |
| `TOP_K_RESULTS` | 5 | Number of chunks to retrieve |
| `AI_MODEL` | gpt-4o-mini | LLM model for generation |
| `EMBEDDING_MODEL` | text-embedding-3-small | Embedding model |
| `EMBEDDING_API_URL` | https://api.openai.com/v1 | Embedding API endpoint |
| `AI_API_URL` | https://api.openai.com/v1 | LLM API endpoint |

## Performance Optimization

### Indexing Strategy

```sql
-- Vector similarity index (IVFFlat)
CREATE INDEX idx_chunks_embedding ON document_chunks
USING ivfflat (embedding_vector vector_cosine_ops)
WITH (lists = 100);

-- B-tree indexes for filtering
CREATE INDEX idx_chunks_document ON document_chunks(document_id);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_category ON documents(category);
```

### Query Optimization

- Connection pooling (max 20 connections)
- Prepared statements for repeated queries
- Limiting result sets with `TOP_K_RESULTS`
- Caching frequent queries (future enhancement)

## Monitoring and Debugging

### Logs

Enable detailed logging in development:

```env
NODE_ENV=development
```

Logs include:
- Query execution time
- Number of retrieved chunks
- Intent classification results
- LLM response times
- Error details

### Analytics

Track in the admin dashboard:
- Total conversations
- Total messages
- Successful vs. failed responses
- Escalation rate
- Most common intents
- Average response time

## Extending the RAG Pipeline

### Adding New Document Types

1. Add parser in `scripts/ingest-knowledge-base.ts`
2. Update chunking logic if needed
3. Add metadata extraction
4. Test ingestion pipeline

### Improving Search

1. Experiment with different embedding models
2. Adjust chunk size and overlap
3. Add hybrid search weights tuning
4. Implement re-ranking models (e.g., Cohere Rerank)

### Custom Retrieval Strategies

```typescript
// Example: Time-weighted retrieval
async timeWeightedSearch(query: string): Promise<DocumentChunk[]> {
  const chunks = await this.hybridSearch(query, 10);
  
  return chunks
    .map(chunk => ({
      ...chunk,
      metadata: {
        ...chunk.metadata,
        _score: chunk.metadata._score * 
               (1 + (Date.now() - new Date(chunk.metadata.effective_date).getTime()) / 31536000000)
      }
    }))
    .sort((a, b) => b.metadata._score - a.metadata._score)
    .slice(0, 5);
}
```
