# UCC Chatbot Assistant Documentation

Complete documentation for the UCC Chatbot Assistant project.

## Documentation Index

| Document | Description |
|----------|-------------|
| [README.md](./README.md) | Project overview and quick start |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | System architecture and design decisions |
| [API.md](./API.md) | REST API reference |
| [SETUP.md](./SETUP.md) | Local development setup guide |
| [DEPLOYMENT.md](./DEPLOYMENT.md) | Production deployment guide |
| [CONTRIBUTING.md](./CONTRIBUTING.md) | Contribution guidelines |
| [RAG.md](./RAG.md) | Retrieval-Augmented Generation pipeline |
| [KNOWLEDGE-BASE.md](./KNOWLEDGE-BASE.md) | Knowledge base management |
| [TESTING.md](./TESTING.md) | Testing strategy and execution |
| [SECURITY.md](./SECURITY.md) | Security policies and practices |

## Project Overview

The UCC Chatbot Assistant is an AI-powered customer-care system for the University of Dar es Salaam Computing Centre (UCC). It uses Retrieval-Augmented Generation (RAG) to provide accurate, sourced answers from an official UCC knowledge base.

**Official Sources:**
- Website: https://ucc.co.tz/
- Admission Portal: https://admission.ucc.co.tz/

## Quick Links

- **Frontend:** React + TypeScript + Tailwind CSS
- **Backend:** Node.js + Express + TypeScript
- **Database:** PostgreSQL + pgvector
- **AI:** OpenAI-compatible API
- **Admin:** Separate React dashboard

## Getting Started

```bash
# Install dependencies
npm install

# Configure environment
cp .env.example .env

# Run migrations
npm run db:migrate

# Seed database
npm run db:seed

# Ingest knowledge base
npx tsx scripts/ingest-knowledge-base.ts
npx tsx scripts/generate-embeddings.ts

# Start development
npm run dev
```

## Support

For issues and questions, contact the UCC ICT office or visit https://ucc.co.tz/.
