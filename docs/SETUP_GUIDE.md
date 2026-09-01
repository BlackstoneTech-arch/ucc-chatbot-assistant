# UCC Chatbot Assistant — Manual Setup & Operations Guide

> **Audience:** UCC IT staff / DevOps engineer who needs to deploy, configure, and run the UCC AI Assistant end-to-end.
>
> **What you will have at the end:**
> - Backend running on a public URL (`https://api.your-domain.com`) with JWT auth, RAG pipeline, knowledge base
> - Admin dashboard at a public URL (`https://admin.your-domain.com`) with login
> - Public chatbot widget live at `https://uccchatbot.your-domain.com` (or wherever you host the frontend)
> - Database (MySQL 8 or PostgreSQL) with all tables auto-created and seeded
> - An admin user you can log in with immediately
>
> **Estimated time:** 60–120 minutes the first time. 5 minutes for re-deploys.

---

## Table of Contents

1. [System Requirements](#1-system-requirements)
2. [Repository Layout](#2-repository-layout)
3. [Phase 1 — Local First Run (5 minutes)](#3-phase-1--local-first-run-5-minutes)
4. [Phase 2 — Get a Real Database](#4-phase-2--get-a-real-database)
5. [Phase 3 — Configure Secrets & Environment](#5-phase-3--configure-secrets--environment)
6. [Phase 4 — Build the Backend](#6-phase-4--build-the-backend)
7. [Phase 5 — Run the Backend](#7-phase-5--run-the-backend)
8. [Phase 6 — Deploy the Admin Dashboard](#8-phase-6--deploy-the-admin-dashboard)
9. [Phase 7 — Verify End-to-End](#9-phase-7--verify-end-to-end)
10. [Phase 8 — Enable LLM Fallback (Optional)](#10-phase-8--enable-llm-fallback-optional)
11. [Phase 9 — Populate the Knowledge Base](#11-phase-9--populate-the-knowledge-base)
12. [Phase 10 — Production Hardening](#12-phase-10--production-hardening)
13. [Troubleshooting](#13-troubleshooting)
14. [Maintenance Runbook](#14-maintenance-runbook)

---

## 1. System Requirements

### For local development (Windows, macOS, or Linux)

| Tool | Minimum | Notes |
|---|---|---|
| **Java JDK 17+** | 17 | The build scripts auto-download it if missing. Spring Boot 3.3.4 requires Java 17. |
| **Maven 3.8+** | 3.8 | Auto-downloaded by `build-backend.ps1`. Otherwise install via `brew install maven` / `apt install maven` / [download](https://maven.apache.org/download.cgi). |
| **Node.js 18+** | 18 | Only needed to serve the admin dashboard locally via `npx serve`. Install from [nodejs.org](https://nodejs.org). |
| **MySQL 8** (or PostgreSQL 14+) | 8.0 / 14 | Local install *or* a managed DB. See §4. |
| **Git** | 2.30+ | To clone the repo. |
| **PowerShell 5.1+** | 5.1 | Windows only. The build scripts are `.ps1`. On macOS/Linux use the equivalent bash commands shown inline. |
| **curl** | any | For smoke-testing endpoints. |
| **2 GB free disk** | — | JDK + Maven cache + built JAR (~200 MB). |

### For production deployment

- A **Linux VM** (Ubuntu 22.04 LTS recommended) OR a **PaaS** like Render, Railway, Fly.io, Heroku
- A **managed MySQL** or **PostgreSQL** instance (or self-hosted on the same VM)
- A **TLS reverse proxy** (Caddy, nginx, or the PaaS's built-in TLS)
- A **DNS A/CNAME** pointing your domain to the backend host
- A **Netlify** (or Cloudflare Pages / Vercel) account for the admin dashboard

---

## 2. Repository Layout

```
ucc-chatbot-assistant/
├── backend/                    # Spring Boot 3.3.4 / Java 17
│   ├── src/main/java/com/ucc/chatbot/
│   │   ├── config/             # Security, CORS, async, JWT filter
│   │   ├── controller/         # REST controllers (public + /api/admin)
│   │   ├── dto/                # Request/response DTOs
│   │   ├── model/              # JPA entities (User, Conversation, KnowledgeDocument, ...)
│   │   ├── repository/         # Spring Data JPA repositories
│   │   ├── service/            # Business logic (Auth, Chat, Knowledge, AI, WebsiteSync)
│   │   ├── util/               # EncryptionUtil, etc.
│   │   └── UccChatbotApplication.java
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
├── admin/                      # Admin dashboard (vanilla HTML/CSS/JS)
│   ├── dashboard.html
│   ├── index.html
│   ├── js/admin.js, admin-auth.js
│   ├── css/admin.css
│   └── netlify.toml
├── frontend/                   # Public website + chatbot widget
│   ├── index.html, chat.html, courses.html, ...
│   └── js/chatbot.js, app.js, ucc-kb.js
├── database/
│   ├── schema.sql              # MySQL 8 schema (30+ tables)
│   ├── seed.sql                # Default admin + 5 FAQs + 3 contacts + AI settings
│   ├── postgresql_schema.sql   # PostgreSQL equivalent
│   ├── seeds/
│   ├── migrations/
│   └── backups/
├── docs/
│   ├── PROJECT_REPORT.md       # Architecture, ERD, full design
│   └── SETUP_GUIDE.md          # ← this file
├── logs/                       # Auto-created at runtime
├── nginx/                      # Production nginx config
├── knowledge-base/             # Local KB files for sync
├── build-backend.ps1           # Auto-installs JDK + Maven, builds JAR
├── start-backend.ps1           # Runs the backend locally
├── start-admin.ps1             # Serves the admin dashboard locally
├── docker-compose.yml          # MySQL + backend + nginx (one-command dev stack)
├── Dockerfile                  # Container image
├── .env.example                # Copy to .env and fill in
└── README.md
```

---

## 3. Phase 1 — Local First Run (5 minutes)

This proves the toolchain works before you touch any cloud services.

### 3.1 Clone

```bash
git clone https://github.com/<your-org>/ucc-chatbot-assistant.git
cd ucc-chatbot-assistant
```

### 3.2 Build the JAR (Windows)

```powershell
.\build-backend.ps1 -Package
```

Expected output (last lines):
```
[INFO] Replacing main artifact ...ucc-chatbot-1.0.0.jar with repackaged archive
[INFO] BUILD SUCCESS
[INFO] Total time:  01:30 min
```

> The first run downloads ~150 MB of dependencies into `.tools/` and your local Maven cache (`~/.m2`). Subsequent runs take ~10 seconds.

### 3.3 Build on macOS / Linux

```bash
cd backend
mvn clean package -DskipTests
cd ..
```

### 3.4 Start the backend in H2 mode (no DB needed)

For the absolute fastest first run, use H2 (in-memory):

```powershell
$env:DB_URL = "jdbc:h2:mem:uccdb;DB_CLOSE_DELAY=-1"
$env:DB_DRIVER = "org.h2.Driver"
$env:DB_DIALECT = "org.hibernate.dialect.H2Dialect"
$env:JWT_SECRET = "local-dev-secret-please-change-for-prod-32bytes!!"
$env:ADMIN_EMAIL = "admin@ucc.co.tz"
$env:ADMIN_PASSWORD = "Admin@123"
$env:ADMIN_NAME = "UCC Administrator"
java -jar backend/target/ucc-chatbot-1.0.0.jar --server.port=8081
```

> H2 is bundled in the JAR, so this works out of the box. Data is lost when you stop the process.

### 3.5 Verify it started

Open a second terminal:

```bash
curl http://localhost:8081/api/health
```

Expected response:
```json
{"status":"UP","database":"UP","aiService":"UP","timestamp":"...","version":"1.0.0"}
```

If you see that, the backend is alive. **Stop here and celebrate** — you've passed the hardest part.

---

## 4. Phase 2 — Get a Real Database

You need a MySQL 8+ (or PostgreSQL 14+) instance. Pick **one** of the options below.

### Option A — Local MySQL (developer machine)

**Windows:**
1. Download [MySQL Community Server 8.0](https://dev.mysql.com/downloads/mysql/) and install with default options.
2. During install, set root password to something you'll remember.
3. Create the database:
   ```sql
   CREATE DATABASE ucc_chatbot_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'ucc_user'@'localhost' IDENTIFIED BY 'strong_password_here';
   GRANT ALL PRIVILEGES ON ucc_chatbot_db.* TO 'ucc_user'@'localhost';
   FLUSH PRIVILEGES;
   ```
4. (Optional) Load the schema. The app will auto-create tables on first run, so this is only needed for the seed data:
   ```bash
   mysql -u ucc_user -p ucc_chatbot_db < database/schema.sql
   mysql -u ucc_user -p ucc_chatbot_db < database/seed.sql
   ```

**macOS:**
```bash
brew install mysql@8.0
brew services start mysql@8.0
mysql -u root -e "CREATE DATABASE ucc_chatbot_db CHARACTER SET utf8mb4;"
```

**Ubuntu:**
```bash
sudo apt install mysql-server-8.0
sudo mysql_secure_installation
sudo mysql -e "CREATE DATABASE ucc_chatbot_db CHARACTER SET utf8mb4;"
```

### Option B — Managed MySQL (free tiers)

| Provider | Free tier | URL |
|---|---|---|
| **PlanetScale** | 1 DB, 1 GB | https://planetscale.com |
| **Railway MySQL** | Trial $5 credit | https://railway.app |
| **Aiven MySQL** | 1-month trial | https://aiven.io |
| **DigitalOcean** | None ($6/mo) | https://digitalocean.com |

When you create the DB, copy these values for the next step:
- **Host** (e.g. `aws.connect.psdb.cloud`)
- **Port** (usually `3306`)
- **Database name**
- **Username**
- **Password**
- **SSL** — note whether it's required (`?sslmode=REQUIRED` in URL) or optional

### Option C — Netlify PostgreSQL (for the Netlify-first deployment)

If you want to use the Netlify DB that was provisioned earlier:

```
Host:     ep-lucky-mouse-a5nt5hhl.us-east-2.db.netlify.com
Port:     5432
Database: netlifydb
Username: netlifydb_owner
Password: npg_FOjBtW7N3rRA
SSL:      required
```

Connection URL:
```
jdbc:postgresql://ep-lucky-mouse-a5nt5hhl.us-east-2.db.netlify.com:5432/netlifydb?sslmode=require
```

**Important:** Netlify DB is a shared/free PostgreSQL. Tables will be created automatically on first run. For production UCC traffic, get a dedicated managed DB instead.

### Option D — Docker MySQL (fastest on any machine with Docker)

```bash
docker run --name ucc-mysql -d \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -e MYSQL_DATABASE=ucc_chatbot_db \
  -e MYSQL_USER=ucc_user \
  -e MYSQL_PASSWORD=ucc_pass \
  -p 3306:3306 \
  mysql:8.0
```

That's it. Connection URL:
```
jdbc:mysql://localhost:3306/ucc_chatbot_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

---

## 5. Phase 3 — Configure Secrets & Environment

### 5.1 Copy the example env file

```bash
cp .env.example .env
```

### 5.2 Edit `.env` — the only file you need to change

Open `.env` in any editor and fill in:

```env
# === Server ===
PORT=8080

# === Database (pick ONE option below) ===

# Option A: MySQL (local Docker example)
DB_TYPE=mysql
DB_URL=jdbc:mysql://localhost:3306/ucc_chatbot_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=ucc_user
DB_PASSWORD=strong_password_here
DB_DRIVER=com.mysql.cj.jdbc.Driver
DB_DIALECT=org.hibernate.dialect.MySQLDialect

# Option B: PostgreSQL (Netlify example) - just uncomment and use these:
# DB_TYPE=postgres
# DB_URL=jdbc:postgresql://ep-lucky-mouse-a5nt5hhl.us-east-2.db.netlify.com:5432/netlifydb?sslmode=require
# DB_USERNAME=netlifydb_owner
# DB_PASSWORD=npg_FOjBtW7N3rRA
# DB_DRIVER=org.postgresql.Driver
# DB_DIALECT=org.hibernate.dialect.PostgreSQLDialect

# === JWT (REQUIRED) ===
# Generate one with: node -e "console.log(require('crypto').randomBytes(48).toString('base64'))"
JWT_SECRET=PASTE_A_RANDOM_48_BYTE_BASE64_STRING_HERE
JWT_EXPIRATION_MS=604800000

# === Default admin (created on first startup) ===
ADMIN_EMAIL=admin@ucc.co.tz
ADMIN_PASSWORD=ChangeMeImmediately!2026
ADMIN_NAME=UCC Administrator

# === AI (leave blank to use the static KB only) ===
AI_API_KEY=
AI_API_URL=https://api.openai.com/v1
AI_MODEL=gpt-4o-mini
AI_EMBEDDING_MODEL=text-embedding-3-small

# === CORS ===
# Comma-separated list of allowed origins. MUST include every URL the
# admin dashboard and public chatbot will be served from.
CORS_ALLOWED_ORIGINS=http://localhost:5500,http://localhost:3000,http://localhost:3001,http://localhost:8080,http://localhost:8081,https://admin.your-domain.com,https://uccchatbot.your-domain.com

# === Public frontend URL (used in Open Graph / canonical links) ===
FRONTEND_URL=https://uccchatbot.your-domain.com
```

### 5.3 Generate a strong JWT secret

Open PowerShell or any terminal and run:

```powershell
node -e "console.log(require('crypto').randomBytes(48).toString('base64'))"
```

Or in Python:
```bash
python -c "import secrets; print(secrets.token_urlsafe(48))"
```

Copy the output into `JWT_SECRET=`. **Do not reuse a short or guessable string.**

### 5.4 Verify the .env file

```powershell
# Windows
Get-Content .env

# macOS/Linux
cat .env
```

Confirm there are no placeholder values like `your-password` or `change-me` still present.

---

## 6. Phase 4 — Build the Backend

### Option A — Windows (one command)

```powershell
.\build-backend.ps1 -Package
```

This:
1. Detects any installed JDK 17+, or downloads Eclipse Temurin 17 into `.tools/`
2. Detects any installed Maven 3.8+, or downloads it into `.tools/`
3. Compiles all 103 source files
4. Runs the unit tests (skip with `-SkipTests`)
5. Produces `backend/target/ucc-chatbot-1.0.0.jar` (Spring Boot fat JAR, ~60 MB)

Build time: **40 seconds** after the first dependency download.

### Option B — macOS / Linux

```bash
cd backend
mvn clean package -DskipTests
cd ..
```

The JAR lands at `backend/target/ucc-chatbot-1.0.0.jar`.

### Verify the build

```bash
ls -la backend/target/ucc-chatbot-1.0.0.jar
java -jar backend/target/ucc-chatbot-1.0.0.jar --version
```

You should see `ucc-chatbot 1.0.0` and Spring Boot's banner.

---

## 7. Phase 5 — Run the Backend

### 7.1 Windows (uses your .env automatically)

```powershell
.\start-backend.ps1
```

Output will look like:
```
[2026-09-01 ...] Starting UCC Chatbot backend on port 8081...
[2026-09-01 ...] Tomcat started on port 8081
[2026-09-01 ...] Started UccChatbotApplication in 8.3 seconds
```

The `start-backend.ps1` script reads `.env`, sets every variable, and runs the JAR.

### 7.2 macOS / Linux (manual)

```bash
set -a; source .env; set +a
java -jar backend/target/ucc-chatbot-1.0.0.jar --server.port=${PORT:-8080}
```

### 7.3 Docker Compose (the "all-in-one" path)

If you have Docker:

```bash
docker compose up -d
```

This starts MySQL 8, the backend, and nginx in one command. Wait ~30 seconds for MySQL health check, then:

```bash
curl http://localhost:8080/api/health
```

To see logs:
```bash
docker compose logs -f backend
```

To stop:
```bash
docker compose down
```

### 7.4 Verify it's working

In another terminal:

```bash
# 1. Health check (no auth)
curl http://localhost:8080/api/health

# 2. Login with the default admin
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@ucc.co.tz","password":"ChangeMeImmediately!2026"}'
```

You should get back a JSON with `token`, `refreshToken`, and `user` (with role `ADMIN`).

If the login fails with `403` or `Bad credentials`, the admin user wasn't created. Check the startup log for the line `Created default admin: admin@ucc.co.tz`. If missing, see §13 Troubleshooting.

### 7.5 Test the chatbot (no auth needed)

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello, what programmes do you offer?","conversationId":"test-1"}'
```

Expected: a JSON with `answer` describing DCIT, DBIT, CCIT, CBIT.

### 7.6 Test an authenticated admin endpoint

```bash
TOKEN="paste-token-here"
curl http://localhost:8080/api/admin/dashboard/stats \
  -H "Authorization: Bearer $TOKEN"
```

Expected: dashboard stats JSON.

---

## 8. Phase 6 — Deploy the Admin Dashboard

The admin dashboard is a static SPA. It has two files that talk to the backend:
- `admin/index.html` (login page)
- `admin/dashboard.html` (the actual app)

### 8.1 Update the API base URL

Open `admin/js/admin-auth.js` and change line 5-7:

```js
const ADMIN_CONFIG = {
  API_BASE_URL: "https://api.your-domain.com/api"   // <-- your backend URL
};
```

> For local dev, leave it as `http://localhost:8081/api`.

### 8.2 Deploy to Netlify (recommended, free)

1. Push the `admin/` directory to its own Git repo (or use the same repo with `base = admin` in `netlify.toml`).
2. In Netlify, "Add new site" → "Import from Git" → select the repo.
3. **Build command:** `echo "no build"`
4. **Publish directory:** `.` (the `admin/` folder)
5. Click Deploy. Netlify gives you a URL like `https://ucc-admin.netlify.app`.
6. Go to **Site settings → Environment variables** and add (if you want to override):
   - `API_BASE_URL` (we still hardcode in JS for now, but future versions will read this)
7. **Custom domain** (optional): set up `admin.your-domain.com` in Domain settings.

The `admin/netlify.toml` already configures SPA-style redirects and security headers.

### 8.3 Deploy to Cloudflare Pages (alternative)

1. Push to Git.
2. Cloudflare Pages → Create → Direct Upload → drag the `admin/` folder.
3. Custom domain: `admin.your-domain.com`.

### 8.4 Self-host with nginx

Copy `admin/` to `/var/www/ucc-admin/`. Add an nginx site:

```nginx
server {
  listen 443 ssl http2;
  server_name admin.your-domain.com;
  ssl_certificate /etc/letsencrypt/live/admin.your-domain.com/fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/live/admin.your-domain.com/privkey.pem;

  root /var/www/ucc-admin;
  index index.html;

  # SPA fallback
  location / {
    try_files $uri $uri/ /index.html;
  }

  # Security headers
  add_header X-Frame-Options "DENY" always;
  add_header X-Content-Type-Options "nosniff" always;
}
```

### 8.5 Local dev only

```powershell
.\start-admin.ps1
```

This serves the admin dashboard at `http://localhost:3001/` and points it at `http://localhost:8081/api` by default.

---

## 9. Phase 7 — Verify End-to-End

This is the **acceptance test**. Do every step in order.

### 9.1 Open the admin dashboard

Go to `https://admin.your-domain.com` (or `http://localhost:3001` locally). You should see a login page.

### 9.2 Log in

- **Email:** `admin@ucc.co.tz`
- **Password:** the value of `ADMIN_PASSWORD` in your `.env`

If successful, you land on the dashboard. You should see:
- "Total conversations: 0" (no chats yet)
- "Knowledge documents: 0" (no KB yet)
- "Pending knowledge: 0"

### 9.3 Test the public chatbot

Open `https://uccchatbot.your-domain.com` (or `frontend/chat.html` locally). Type:

- `Hello` → should reply with a greeting
- `What programmes do you offer?` → should list DCIT, DBIT, CCIT, CBIT
- `Habari, naomba kujua kuhusu DCIT` (Swahili) → should reply in Swahili with DCIT details

### 9.4 Verify admin → knowledge flow

1. In admin → **Knowledge** tab → click **Add Document**.
2. Fill in:
   - Title: `Library hours`
   - Category: `GENERAL`
   - Content: `The UCC library is open Monday–Friday 8 AM to 9 PM, Saturday 9 AM to 5 PM, closed Sunday.`
3. Click **Save**. The new document appears in the list with status `PENDING`.
4. Click **Approve** on the document.
5. Go to the public chatbot and ask: `What are the library hours?` → it should return the text you just entered.

If that works, the entire pipeline is functional: **admin creates content → it lands in MySQL → it gets indexed → the chatbot retrieves it → the user gets an answer**.

### 9.5 Verify the auth flow

1. Click your name (top right) → **Logout**. You're sent back to the login page.
2. Try `http://admin.your-domain.com/dashboard.html` directly → you should be redirected to login.
3. Log in again → dashboard loads.

### 9.6 Check the logs

```bash
tail -f logs/backend.log
```

You should see:
- `Started UccChatbotApplication in 8.3 seconds`
- `Tomcat started on port 8081`
- Per-request log lines for each API hit

---

## 10. Phase 8 — Enable LLM Fallback (Optional)

Without an LLM, the chatbot can only answer from the static UCC knowledge base that ships with the JAR (programmes, fees, contact info, etc.). With an LLM, it can:

- Paraphrase answers more naturally
- Handle questions the static KB doesn't cover
- Translate more accurately between English and Swahili

The LLM is a *fallback* — the static KB always wins when it has an answer. So enabling it is a strict improvement.

### 10.1 Get an API key

Pick one:
- **OpenAI**: https://platform.openai.com/api-keys (~$0.15 per 1M input tokens for gpt-4o-mini)
- **OpenRouter** (cheaper, many models): https://openrouter.ai/keys
- **Azure OpenAI** (enterprise): contact Microsoft
- **Local Ollama** (free, no internet): see §10.3

### 10.2 Configure

In the admin dashboard → **AI Training** tab → **Settings** sub-tab, you can paste the key and it gets encrypted with AES-256. Or set `AI_API_KEY` in `.env` and restart.

### 10.3 Local Ollama (free, no API key)

```bash
# Install Ollama: https://ollama.com/download
ollama pull llama3.1:8b
ollama serve
```

Then in `.env`:
```env
AI_API_URL=http://localhost:11434/v1
AI_API_KEY=ollama
AI_MODEL=llama3.1:8b
```

### 10.4 Test the LLM path

In the admin dashboard → **AI Training** → **Test**, type:
```
What is the capital of France?
```

Without LLM: `I couldn't find verified information about that.`
With LLM: `The capital of France is Paris.`

### 10.5 Cost guardrails

The system prompt includes a hard cap of `max_tokens=400` per response. On gpt-4o-mini, that's ~$0.00006 per response. 10,000 chats ≈ $0.60.

---

## 11. Phase 9 — Populate the Knowledge Base

You have three options, in order of effort.

### 11.1 Use the admin dashboard (manual, ongoing)

Best for: small numbers of curated documents (FAQs, contact info, official policies).

Steps: see §9.4 above.

### 11.2 Bulk import via file upload

1. Create a text file `ucc-faq.txt`:
   ```
   Q: What is the DCIT programme duration?
   A: DCIT is a 2-year programme (4 semesters).

   Q: How much are the fees?
   A: DCIT total fees for 2026/2027 are TZS 3,020,000.
   ```
2. In admin → Knowledge → **Upload** tab → select the file → category `FAQs` → Upload.
3. The system chunks the file into ~500-character pieces, indexes them, and they're immediately retrievable.

### 11.3 Scrape the live ucc.co.tz website (one-click)

1. In admin → **Knowledge** → **Sync Website** tab.
2. Click **Start scan from https://ucc.co.tz/**.
3. Watch the progress (it's async — runs in the background, ~30 seconds to scan 10 important pages).
4. The system:
   - Downloads each page
   - Strips HTML, keeps the readable text
   - Hashes the content (so unchanged pages don't re-index)
   - Creates one knowledge document per page
   - Auto-approves them so they're immediately retrievable
5. Re-run the scan weekly/monthly to catch updates.

### 11.4 Seed from SQL (one-time bulk)

`database/seed.sql` ships with 5 sample FAQs and 3 contacts. To load:
```bash
mysql -u ucc_user -p ucc_chatbot_db < database/seed.sql
```

The `schema.sql` is optional because Hibernate's `ddl-auto=update` will create tables automatically. Only use `schema.sql` if you want to inspect the schema or seed admin/AI settings from scratch.

---

## 12. Phase 10 — Production Hardening

Before going live with real users:

### 12.1 Checklist

- [ ] **JWT_SECRET** is at least 32 random bytes (not "change-me")
- [ ] **ADMIN_PASSWORD** is at least 12 characters and not the default
- [ ] **DB password** is strong and stored in a secret manager (not in `.env` in version control)
- [ ] **TLS** is enabled on the backend (Caddy/nginx in front, or PaaS-managed)
- [ ] **CORS_ALLOWED_ORIGINS** lists only your real domains (not `*` or `localhost` in production)
- [ ] **Logging level** is `INFO` (not `DEBUG`) in production — change `logging.level.com.ucc.chatbot=DEBUG` → `INFO`
- [ ] **Hibernate SQL** is off: `spring.jpa.show-sql=false` ✓ (default)
- [ ] **Database backups** are configured (managed DBs do this automatically)
- [ ] **Uploads directory** is backed up
- [ ] **Admin dashboard** has its own subdomain with strong password
- [ ] **Rate limiting** is in place (nginx `limit_req` or a CDN rule)
- [ ] **Monitoring** is wired (see §12.2)

### 12.2 Monitoring

The backend exposes a health endpoint:
```bash
curl https://api.your-domain.com/api/health
```

For uptime monitoring, use:
- [UptimeRobot](https://uptimerobot.com) (free, pings every 5 min, alerts on downtime)
- [Better Stack](https://betterstack.com)
- [Pingdom](https://www.pingdom.com)

For error tracking:
- The `/api/admin/logs/errors` endpoint shows recent errors
- Ship logs to a service: [Logtail](https://logtail.com), [Papertrail](https://papertrailapp.com), or just `tail -f logs/backend.log`

### 12.3 Backups

The MySQL database has all your users, knowledge, and chat history. Back it up daily:

```bash
# Cron job
0 2 * * * mysqldump -u backup_user -p'...' ucc_chatbot_db | gzip > /backups/ucc-$(date +%F).sql.gz
```

Or use the managed DB's snapshot feature (PlanetScale, DigitalOcean, etc. all have this).

### 12.4 Scaling

The default setup handles ~100 concurrent users comfortably on a $5/month VPS. To scale beyond:

| Concern | Solution |
|---|---|
| DB CPU | Move to a managed MySQL with more vCPUs |
| Backend CPU | Run 2–4 backend instances behind a load balancer |
| LLM cost | Cache popular answers in Redis, lower `max_tokens` |
| Static assets | Put admin + frontend behind a CDN (Cloudflare, Netlify) |

---

## 13. Troubleshooting

### Build fails: "JAVA_HOME not set"

**Windows:**
```powershell
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17.0.11.9\jdk-17.0.11+9", "User")
# Or run the build script which auto-downloads a JDK
```

**macOS:**
```bash
brew install openjdk@17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

**Linux:**
```bash
sudo apt install openjdk-17-jdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

### Build fails: "package com.ucc.chatbot does not exist"

You ran Maven from the wrong directory. Always:
```bash
cd backend
mvn clean package
```

### App starts but immediately exits: "Failed to determine a suitable driver class"

The `DB_DRIVER` env var is wrong or the URL is malformed. Check:
- MySQL: `DB_DRIVER=com.mysql.cj.jdbc.Driver`, URL starts with `jdbc:mysql://`
- PostgreSQL: `DB_DRIVER=org.postgresql.Driver`, URL starts with `jdbc:postgresql://`

### App starts but login returns 401 "Bad credentials"

The default admin user wasn't created. Two causes:
1. The `ADMIN_EMAIL`/`ADMIN_PASSWORD` env vars aren't set → add them to `.env`
2. The user already exists with a different password → reset it:
   ```sql
   UPDATE users SET password_hash = '<bcrypt-hash>' WHERE email = 'admin@ucc.co.tz';
   ```
   Or delete the user and restart the app to re-seed it.

### CORS error in browser: "Access to fetch at '...' has been blocked by CORS policy"

Your `CORS_ALLOWED_ORIGINS` doesn't include the URL you're accessing from. Add it to `.env` and restart. Example:
```env
CORS_ALLOWED_ORIGINS=https://admin.netlify.app,https://uccchatbot.netlify.app
```

### Chat returns "I'm having trouble processing your request"

1. Check the backend log: `tail -f logs/backend.log`
2. If you see "AI API returned status: 401" — your `AI_API_KEY` is wrong or revoked
3. If you see "Could not extract content from response" — the LLM returned an unexpected format
4. Without an LLM key, the system falls back to the static KB; if the question isn't there, you get a polite "I couldn't find that" answer

### Knowledge upload returns 500

Check `logs/backend.log` for the stack trace. Most common cause: file too large (default limit is 10 MB; bump in `application.properties` if needed).

### Admin dashboard shows "Failed to fetch"

The dashboard can't reach the backend. Open browser DevTools → Network tab → check the request URL. Most common causes:
1. The `API_BASE_URL` in `admin/js/admin-auth.js` is wrong
2. The backend isn't running
3. CORS is blocking the request (see above)

### Database tables are missing

Hibernate's `ddl-auto=update` creates tables on first run, but only for entities in the code. If you want to inspect the full schema: `mysql -u ucc_user -p ucc_chatbot_db < database/schema.sql`. The app is forward-compatible: it ignores extra columns in tables.

### `mvn` is super slow

The Maven repository is downloading for the first time. Be patient (~150 MB). For future runs it's cached in `~/.m2/repository/`.

### Java 25 build warnings

If you're on JDK 25 (newer than the project's target of 17), you may see `WARNING: A restricted method in java.lang.System has been called`. These are harmless — the project compiles to Java 17 bytecode and runs on any JDK 17+.

---

## 14. Maintenance Runbook

### Daily
- Nothing. The system runs itself.

### Weekly
- Check `https://admin.your-domain.com` → **Dashboard** tab for unusual error counts
- Check `/api/admin/logs/errors` for repeated failures
- Optionally re-run the **Website Sync** to catch ucc.co.tz updates

### Monthly
- Review chat logs for unanswered questions → add them to the knowledge base
- Update the default admin password
- `docker system prune` (if using Docker) to reclaim disk space
- Verify database backups are completing

### Quarterly
- Rotate the `JWT_SECRET` (forces all users to log in again — plan a maintenance window)
- Update Java/Maven to latest patch versions
- Update the `ai.model` if a better/cheaper one is available
- Review the AI logs for cost/usage trends

### When you change the model or LLM provider
1. Update `AI_API_KEY` and `AI_MODEL` in `.env`
2. Restart the backend: `.\start-backend.ps1` (or `docker compose restart backend`)
3. Test in admin → **AI Training** → **Test**

### When you upgrade the backend code
```bash
git pull
.\build-backend.ps1 -Package
# Stop the old process (Ctrl+C in its terminal, or `docker compose down`)
# Start the new one
.\start-backend.ps1
```

The database is preserved. Hibernate adds new tables/columns automatically; it never drops or alters existing data.

### When you need to reset the database
```bash
mysql -u root -p -e "DROP DATABASE ucc_chatbot_db; CREATE DATABASE ucc_chatbot_db CHARACTER SET utf8mb4;"
# Restart the app — it will recreate all tables and re-seed the admin user
```

---

## Quick Reference Card

```bash
# === One-time setup ===
cp .env.example .env
# edit .env (DB_*, JWT_SECRET, ADMIN_*)
.\build-backend.ps1 -Package

# === Daily run ===
.\start-backend.ps1                    # backend on :8081
.\start-admin.ps1                      # admin UI on :3001

# === Docker ===
docker compose up -d                   # all-in-one
docker compose logs -f backend         # watch backend logs
docker compose down                    # stop everything

# === Verify ===
curl http://localhost:8081/api/health

# === Update admin URL ===
# Edit admin/js/admin-auth.js line 5-7, then redeploy admin/

# === Reset admin password ===
# SQL: UPDATE users SET password_hash = '<bcrypt-hash>' WHERE email = 'admin@ucc.co.tz';
# Or delete the user and let DataLoader recreate it on next start

# === Rebuild after code change ===
.\build-backend.ps1 -Package
# restart the backend
```

---

## Where to get help

1. **First**, check `docs/PROJECT_REPORT.md` for architecture, ERD, and design intent.
2. **Second**, check this guide's §13 Troubleshooting.
3. **Third**, check `logs/backend.log` for the actual stack trace.
4. **Fourth**, hit `/api/health` and `/api/admin/dashboard/health` to see the system's self-diagnosed status.
5. **Last resort**, contact the project lead.

---

*Last updated: 2026-09-01. See `docs/PROJECT_REPORT.md` for the architecture deep-dive.*
