# UCC Chatbot Assistant

AI-powered university customer-care assistant for the University of Dar es Salaam Computing Centre (UCC).

## Official Information

- **Website:** https://ucc.co.tz/
- **Admission Portal:** https://admission.ucc.co.tz/

## Features

- Natural language Q&A about UCC
- RAG-based retrieval from official UCC knowledge base
- Programme and admission information
- Fee information
- Contact information
- Source attribution
- Human escalation
- Admin knowledge management dashboard
- Analytics and feedback
- Swahili/English support architecture

## Quick Start

### Prerequisites

- Node.js >= 18
- PostgreSQL >= 14
- OpenAI API key (or compatible LLM provider)

### Installation

```bash
# Clone the repository
git clone <repository-url> ucc-chatbot-assistant
cd ucc-chatbot-assistant

# Install dependencies
npm install

# Configure environment
cp .env.example .env
# Edit .env with your database and AI credentials

# Run database migrations
npm run db:migrate

# Seed initial data
npm run db:seed

# Start development
npm run dev
```

## Project Structure

```
ucc-chatbot-assistant/
├── backend/                 # Node.js/Express/TypeScript API
│   ├── src/
│   │   ├── routes/          # API routes
│   │   ├── controllers/     # Request handlers
│   │   ├── services/        # Business logic
│   │   ├── middleware/      # Auth, rate limiting, etc.
│   │   ├── rag/             # RAG pipeline
│   │   ├── ai/              # LLM integration
│   │   ├── config/          # Configuration
│   │   └── db/              # Database scripts
│   └── package.json
├── frontend/                # React/TypeScript/Tailwind
│   ├── src/
│   │   ├── components/      # Reusable UI components
│   │   ├── pages/           # Page components
│   │   ├── services/        # API clients
│   │   ├── types/           # TypeScript types
│   │   └── hooks/           # Custom hooks
│   └── package.json
├── admin/                   # Admin dashboard
├── database/
│   ├── schema.sql           # PostgreSQL schema
│   └── seeds/               # Sample data
├── knowledge-base/          # UCC knowledge base documents
├── tests/                   # Test suites
├── docs/                    # Documentation
└── scripts/                 # Utility scripts
```

## Environment Variables

See `.env.example` for required variables.

Key variables:
- `DATABASE_URL` - PostgreSQL connection string
- `AI_API_KEY` - OpenAI/compatible API key
- `JWT_SECRET` - JWT signing secret
- `PORT` - Backend server port

## API Endpoints

### Public
- `POST /api/chat` - Send a chat message
- `GET /api/chat/programmes` - List programmes
- `GET /api/chat/programmes/:id` - Get programme details
- `GET /api/chat/faqs` - List FAQs
- `GET /api/chat/contacts` - List contacts
- `POST /api/chat/feedback` - Submit feedback
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration

### Admin
- `GET /api/admin/analytics` - Dashboard analytics
- `GET /api/admin/conversations` - List conversations
- `GET /api/admin/documents` - List documents
- `PUT /api/admin/documents/:id` - Update document
- `DELETE /api/admin/documents/:id` - Delete document
- `POST /api/admin/faqs` - Create FAQ
- `GET /api/admin/stats` - Knowledge base stats

## Development

```bash
# Backend
npm run dev:backend

# Frontend
npm run dev:frontend

# Database migration
npm run db:migrate

# Database seed
npm run db:seed

# Run tests
npm run test

# Lint
npm run lint
```

## Security

- JWT authentication
- Role-based access control
- Rate limiting
- Input validation
- SQL injection protection
- XSS protection
- Secure HTTP headers
- Environment variable protection

## License

Proprietary - University of Dar es Salaam Computing Centre
