# REST API Reference

## Base URL

```
http://localhost:5000/api
```

## Authentication

Admin endpoints require JWT authentication via the `Authorization` header:

```
Authorization: Bearer <token>
```

## Rate Limiting

- General API: 100 requests per 15 minutes
- Chat endpoint: 20 messages per minute

## Response Format

### Success Response
```json
{
  "data": { ... },
  "message": "Success message"
}
```

### Error Response
```json
{
  "error": "Error description",
  "requestId": "uuid",
  "timestamp": "2026-08-21T00:00:00.000Z"
}
```

---

## Public Endpoints

### Health Check

```
GET /health
```

Returns API and database health status.

**Response:**
```json
{
  "status": "healthy",
  "database": "connected",
  "timestamp": "2026-08-21T00:00:00.000Z"
}
```

---

### Send Chat Message

```
POST /chat
```

Send a message to the chatbot.

**Request Body:**
```json
{
  "message": "What programmes does UCC offer?",
  "sessionId": "uuid",
  "conversationHistory": [
    { "role": "user", "content": "Hello" },
    { "role": "assistant", "content": "Hi! How can I help?" }
  ]
}
```

**Response:**
```json
{
  "id": "timestamp",
  "message": "What programmes does UCC offer?",
  "response": "UCC offers several programmes...",
  "sources": [
    {
      "documentId": "uuid",
      "title": "Programmes",
      "category": "programmes",
      "similarity": 0.95
    }
  ],
  "intent": "programme_information",
  "confidence": 0.9,
  "escalated": false,
  "timestamp": "2026-08-21T00:00:00.000Z"
}
```

---

### List Programmes

```
GET /chat/programmes
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `level` | string | Filter by programme level (e.g., "undergraduate") |
| `category` | string | Filter by category |
| `academic_year` | string | Filter by academic year (e.g., "2026/2027") |
| `search` | string | Search in title or description |

**Response:**
```json
{
  "data": [
    {
      "id": "uuid",
      "title": "Bachelor of Science in Computer Science",
      "level": "undergraduate",
      "category": "Academic Programme",
      "duration_months": 36,
      "entry_requirements": "...",
      "academic_year": "2026/2027",
      "status": "ACTIVE"
    }
  ],
  "total": 5
}
```

---

### Get Programme by ID

```
GET /chat/programmes/:id
```

**Response:**
```json
{
  "data": {
    "id": "uuid",
    "title": "Bachelor of Science in Computer Science",
    "level": "undergraduate",
    "description": "...",
    "entry_requirements": "...",
    "academic_year": "2026/2027",
    "status": "ACTIVE"
  }
}
```

---

### List FAQs

```
GET /chat/faqs
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `category` | string | Filter by category |
| `search` | string | Search in question or answer |

**Response:**
```json
{
  "data": [
    {
      "id": "uuid",
      "question": "How can I apply to UCC?",
      "answer": "Visit the admission portal...",
      "category": "admissions",
      "priority": 10
    }
  ],
  "total": 20
}
```

---

### List Contacts

```
GET /chat/contacts
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `department_id` | string | Filter by department |

**Response:**
```json
{
  "data": [
    {
      "id": "uuid",
      "name": "UCC Main Office",
      "email": "info@ucc.co.tz",
      "phone": "+255 22 2410 000",
      "is_primary": true
    }
  ],
  "total": 10
}
```

---

### Submit Feedback

```
POST /chat/feedback
```

**Request Body:**
```json
{
  "messageId": "uuid",
  "conversationId": "uuid",
  "rating": 5,
  "comment": "Very helpful!",
  "feedbackType": "response"
}
```

**Response:**
```json
{
  "data": {
    "id": "uuid",
    "rating": 5,
    "comment": "Very helpful!",
    "created_at": "2026-08-21T00:00:00.000Z"
  }
}
```

---

## Authentication Endpoints

### Register

```
POST /auth/register
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "secure_password",
  "full_name": "John Doe",
  "role": "user"
}
```

**Response:**
```json
{
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "full_name": "John Doe",
    "role": "user"
  },
  "token": "jwt_token_here"
}
```

---

### Login

```
POST /auth/login
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "secure_password"
}
```

**Response:**
```json
{
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "full_name": "John Doe",
    "role": "user"
  },
  "token": "jwt_token_here"
}
```

---

### Get Current User

```
GET /auth/me
```

**Headers:**
```
Authorization: Bearer <token>
```

**Response:**
```json
{
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "full_name": "John Doe",
    "role": "user",
    "last_login": "2026-08-21T00:00:00.000Z"
  }
}
```

---

## Admin Endpoints

All admin endpoints require authentication and admin role.

### Get Analytics

```
GET /admin/analytics
```

**Response:**
```json
{
  "totalConversations": 150,
  "totalMessages": 1200,
  "averageRating": 4.2,
  "totalFeedback": 80,
  "topIntents": [
    { "intent": "programme_information", "count": 45 },
    { "intent": "admission", "count": 30 }
  ]
}
```

---

### List Conversations

```
GET /admin/conversations
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | number | Page number (default: 1) |
| `limit` | number | Items per page (default: 20) |
| `isActive` | boolean | Filter by active status |

**Response:**
```json
{
  "data": [
    {
      "id": "uuid",
      "sessionId": "uuid",
      "userEmail": "user@example.com",
      "messageCount": 10,
      "startedAt": "2026-08-21T00:00:00.000Z",
      "isActive": true
    }
  ],
  "total": 50,
  "page": 1,
  "limit": 20
}
```

---

### Get Conversation Messages

```
GET /admin/conversations/:id/messages
```

**Response:**
```json
{
  "data": [
    {
      "id": "uuid",
      "role": "user",
      "content": "Hello",
      "intent": "greeting",
      "createdAt": "2026-08-21T00:00:00.000Z"
    }
  ]
}
```

---

### List Documents

```
GET /admin/documents
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `status` | string | Filter by status (ACTIVE, ARCHIVED, DRAFT) |
| `category` | string | Filter by category |
| `search` | string | Search in title or description |
| `page` | number | Page number |
| `limit` | number | Items per page |

**Response:**
```json
{
  "data": [
    {
      "id": "uuid",
      "title": "Programmes 2026/2027",
      "category": "programmes",
      "status": "ACTIVE",
      "academic_year": "2026/2027",
      "is_indexed": true,
      "created_at": "2026-08-21T00:00:00.000Z"
    }
  ],
  "total": 25,
  "page": 1,
  "limit": 20
}
```

---

### Update Document

```
PUT /admin/documents/:id
```

**Request Body:**
```json
{
  "title": "Updated Title",
  "status": "ACTIVE",
  "effective_date": "2026-08-21"
}
```

**Response:**
```json
{
  "data": {
    "id": "uuid",
    "title": "Updated Title",
    "status": "ACTIVE",
    "updated_at": "2026-08-21T00:00:00.000Z"
  }
}
```

---

### Delete Document

```
DELETE /admin/documents/:id
```

**Response:**
```json
{
  "message": "Document deleted successfully"
}
```

---

### Create FAQ

```
POST /admin/faqs
```

**Request Body:**
```json
{
  "question": "How much are tuition fees?",
  "answer": "Tuition fees vary by programme...",
  "category": "fees",
  "keywords": ["fees", "tuition", "payment"],
  "priority": 5
}
```

**Response:**
```json
{
  "data": {
    "id": "uuid",
    "question": "How much are tuition fees?",
    "answer": "Tuition fees vary by programme...",
    "category": "fees",
    "is_published": true
  }
}
```

---

### Get Knowledge Base Stats

```
GET /admin/stats
```

**Response:**
```json
{
  "totalDocuments": 50,
  "totalChunks": 500,
  "totalFAQs": 100,
  "activeDocuments": 45
}
```

---

## Error Codes

| Status Code | Description |
|-------------|-------------|
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Missing or invalid token |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource not found |
| 429 | Too Many Requests - Rate limit exceeded |
| 500 | Internal Server Error - Server error |
