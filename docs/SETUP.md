# Setup Guide

## Prerequisites

Before setting up the project, ensure you have the following installed:

- **Node.js** >= 18.x ([Download](https://nodejs.org/))
- **npm** >= 9.x (comes with Node.js)
- **PostgreSQL** >= 14.x ([Download](https://www.postgresql.org/download/))
- **Git** ([Download](https://git-scm.com/download/win))
- **OpenAI API Key** or compatible LLM provider key

## Step 1: Clone the Repository

```bash
git clone <repository-url> ucc-chatbot-assistant
cd ucc-chatbot-assistant
```

## Step 2: Install Dependencies

```bash
npm install
```

This installs dependencies for the root workspace, backend, frontend, and admin dashboard.

## Step 3: Set Up PostgreSQL

### Option A: Using psql (Command Line)

```bash
# Log in to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE ucc_chatbot;

# Create user (optional)
CREATE USER ucc_user WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE ucc_chatbot TO ucc_user;
```

### Option B: Using pgAdmin

1. Open pgAdmin
2. Right-click "Databases" → "Create" → "Database"
3. Name: `ucc_chatbot`
4. Click "Save"

## Step 4: Configure Environment Variables

Copy the example environment file and edit it:

```bash
cp .env.example .env
```

Edit `.env` with your settings:

```env
# Database
DATABASE_URL=postgresql://username:password@localhost:5432/ucc_chatbot

# AI/LLM Configuration
AI_API_KEY=your_openai_api_key_here
AI_API_URL=https://api.openai.com/v1
AI_MODEL=gpt-4o-mini
EMBEDDING_API_KEY=your_embedding_api_key_here
EMBEDDING_API_URL=https://api.openai.com/v1
EMBEDDING_MODEL=text-embedding-3-small

# JWT Configuration
JWT_SECRET=your_jwt_secret_key_here_min_32_chars
JWT_EXPIRY=7d
REFRESH_TOKEN_SECRET=your_refresh_token_secret_here
REFRESH_TOKEN_EXPIRY=30d

# Server Configuration
PORT=5000
NODE_ENV=development
CORS_ORIGIN=http://localhost:3000

# Admin Configuration
ADMIN_EMAIL=admin@ucc.co.tz
ADMIN_PASSWORD=change_me_immediately

# File Upload
MAX_FILE_SIZE=10485760
UPLOAD_DIR=./uploads

# RAG Configuration
CHUNK_SIZE=1000
CHUNK_OVERLAP=200
TOP_K_RESULTS=5

# Rate Limiting
RATE_LIMIT_WINDOW_MS=900000
RATE_LIMIT_MAX_REQUESTS=100
```

### Important Notes

- **Never commit `.env` to version control** - it contains secrets
- Use strong, random values for `JWT_SECRET` (at least 32 characters)
- Change the default admin password immediately after first login

## Step 5: Run Database Migrations

```bash
npm run db:migrate
```

This creates all database tables, indexes, and extensions.

**Expected output:**
```
Running database migrations...
Database connection successful
Schema migration completed
```

## Step 6: Seed Initial Data

```bash
npm run db:seed
```

This creates:
- Default admin user (`admin@ucc.co.tz`)
- Default departments (Academic Affairs, Admissions, ICT Support, Finance, Student Services)

**Expected output:**
```
Seeding database...
Database seeded successfully
```

## Step 7: Ingest Knowledge Base

```bash
npx tsx scripts/ingest-knowledge-base.ts
```

This reads all markdown files from the `knowledge-base/` directory, chunks them, and stores them in the database.

**Expected output:**
```
Starting document ingestion...
Database connected
Processing category: about-ucc (1 files)
  Created document: about ucc (5 chunks)
  Indexed 5 chunks for about ucc
...
Document ingestion completed
```

## Step 8: Generate Embeddings

```bash
npx tsx scripts/generate-embeddings.ts
```

This generates vector embeddings for all document chunks using the configured embedding model.

**Note:** This requires `AI_API_KEY` to be set and may take several minutes depending on the size of your knowledge base.

**Expected output:**
```
Generating embeddings for document chunks...
Found 100 chunks without embeddings
Embedded chunk 1
Embedded chunk 2
...
Embedding generation completed
```

## Step 9: Start Development Servers

```bash
npm run dev
```

This starts:
- **Backend API** at `http://localhost:5000`
- **Frontend** at `http://localhost:3000`
- **Admin Dashboard** at `http://localhost:3001`

## Step 10: Verify Installation

1. Open `http://localhost:3000` in your browser
2. You should see the UCC Chatbot Assistant landing page
3. Click "Start Conversation" to open the chat widget
4. Try asking: "What programmes does UCC offer?"
5. Open `http://localhost:3001` for the admin dashboard
6. Log in with `admin@ucc.co.tz` and the password you set

## Troubleshooting

### Database Connection Error

```
Error: connect ECONNREFUSED 127.0.0.1:5432
```

**Solution:** Ensure PostgreSQL is running and the connection string in `.env` is correct.

### Migration Fails

```
Error: relation "users" already exists
```

**Solution:** The tables already exist. Drop the database and recreate it, or use the `IF NOT EXISTS` clause in migrations.

### Embedding Generation Fails

```
Error: 401 Unauthorized
```

**Solution:** Check that `AI_API_KEY` and `EMBEDDING_API_KEY` are correctly set in `.env`.

### Port Already in Use

```
Error: listen EADDRINUSE :::5000
```

**Solution:** Change the `PORT` in `.env` or stop the process using that port.

### Module Not Found

```
Error: Cannot find module 'express'
```

**Solution:** Run `npm install` again in the backend directory.

## Next Steps

1. Read the [Architecture](./ARCHITECTURE.md) documentation
2. Explore the [API Reference](./API.md)
3. Set up the [Admin Dashboard](./ADMIN.md)
4. Configure [Security](./SECURITY.md) settings for production
5. Learn about [RAG Pipeline](./RAG.md) customization
