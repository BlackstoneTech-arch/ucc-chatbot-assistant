# UCC Chatbot Backend Deployment Guide

## Prerequisites
- Docker installed locally (optional, for testing)
- A MySQL 8+ database (local or managed)
- A hosting platform account (Render, Railway, Fly.io, or similar)

## Option A: Docker (local or any Docker host)

```bash
docker build -t ucc-chatbot-backend ./backend
docker run -d \
  -p 8080:8080 \
  -e DB_URL="jdbc:mysql://<host>:3306/ucc_chatbot_db?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true" \
  -e DB_USERNAME="<db-user>" \
  -e DB_PASSWORD="<db-password>" \
  -e DB_DRIVER="com.mysql.cj.jdbc.Driver" \
  -e DB_DIALECT="org.hibernate.dialect.MySQL8Dialect" \
  -e JWT_SECRET="<strong-random-secret>" \
  -e AI_API_KEY="<optional-llm-key>" \
  -e CORS_ALLOWED_ORIGINS="https://ucc-chatbot.blackstone-tech02.workers.dev,http://localhost:3000,http://localhost:8080" \
  -e FRONTEND_URL="https://ucc-chatbot.blackstone-tech02.workers.dev" \
  ucc-chatbot-backend
```

## Option B: Render / Railway / Fly.io

Use the provided Dockerfile and set the same environment variables.

## Required Environment Variables

| Variable | Required | Example |
|----------|----------|---------|
| `DB_URL` | Yes | `jdbc:mysql://host:3306/ucc_chatbot_db?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true` |
| `DB_USERNAME` | Yes | `ucc_user` |
| `DB_PASSWORD` | Yes | `your-password` |
| `DB_DRIVER` | Yes | `com.mysql.cj.jdbc.Driver` |
| `DB_DIALECT` | Yes | `org.hibernate.dialect.MySQL8Dialect` |
| `JWT_SECRET` | Yes | 32+ byte random string |
| `AI_API_KEY` | No | `sk-...` |
| `AI_API_URL` | No | `https://api.openai.com/v1` |
| `AI_MODEL` | No | `gpt-4o-mini` |
| `CORS_ALLOWED_ORIGINS` | Yes | Comma-separated allowed origins |
| `FRONTEND_URL` | Yes | `https://ucc-chatbot.blackstone-tech02.workers.dev` |

## After Deployment

1. Copy the deployed backend URL (e.g. `https://ucc-chatbot-api.onrender.com`)
2. Set the Cloudflare Worker secret:
   ```bash
   npx wrangler secret put BACKEND_URL
   ```
3. Redeploy the Worker:
   ```bash
   npx wrangler deploy
   ```
4. Verify:
   ```bash
   node scripts/health-check.js https://<your-backend-host>
   ```

## Notes
- Do NOT expose `JWT_SECRET`, `AI_API_KEY`, or `DB_PASSWORD` to the frontend.
- The backend auto-creates tables on first run via Hibernate (`spring.jpa.hibernate.ddl-auto=update`).
- Default admin: `admin@ucc.co.tz` / password from `ADMIN_PASSWORD` env or `.env`.
