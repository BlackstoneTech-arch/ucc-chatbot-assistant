# Deployment Guide

## Production Architecture

```
Internet
   │
   │ HTTPS (443)
   ▼
┌─────────────────┐
│     Nginx       │
│  (Reverse Proxy)│
│  + SSL + Static │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌───────┐ ┌─────────┐
│ Front │ │   Admin │
│  end  │ │   Dash  │
│ :3000 │ │ :3001   │
└────┬──┘ └────┬────┘
     │         │
     └────┬────┘
          │
          ▼
┌─────────────────┐
│   Backend API   │
│     :5000       │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌───────┐ ┌─────────┐
│Postgre│ │   AI    │
│  SQL  │ │   API   │
│+pgvec │ │         │
└───────┘ └─────────┘
```

## Prerequisites

- Linux server (Ubuntu 22.04 LTS recommended)
- Domain name with DNS access
- SSL certificate (Let's Encrypt recommended)
- PostgreSQL 14+ with pgvector extension
- Node.js 18+ and npm 9+
- PM2 or systemd for process management
- Nginx for reverse proxy

## Step 1: Server Preparation

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Node.js
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# Install PostgreSQL
sudo apt install -y postgresql postgresql-contrib

# Install Nginx
sudo apt install -y nginx

# Install PM2
sudo npm install -g pm2

# Install git
sudo apt install -y git
```

## Step 2: Database Setup

```bash
# Log in to PostgreSQL
sudo -u postgres psql

# Create database and user
CREATE DATABASE ucc_chatbot;
CREATE USER ucc_user WITH ENCRYPTED PASSWORD 'strong_password_here';
GRANT ALL PRIVILEGES ON DATABASE ucc_chatbot TO ucc_user;

# Enable pgvector extension
\c ucc_chatbot
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

# Exit
\q
```

## Step 3: Deploy Application

```bash
# Clone repository
sudo mkdir -p /var/www/ucc-chatbot
sudo chown -R $USER:$USER /var/www/ucc-chatbot
git clone <repository-url> /var/www/ucc-chatbot
cd /var/www/ucc-chatbot

# Install dependencies
npm install

# Install frontend dependencies
cd frontend && npm install && cd ..

# Install admin dependencies
cd admin && npm install && cd ..
```

## Step 4: Environment Configuration

Create `.env` file in `/var/www/ucc-chatbot/`:

```env
# Database
DATABASE_URL=postgresql://ucc_user:strong_password_here@localhost:5432/ucc_chatbot

# AI/LLM
AI_API_KEY=your_production_api_key
AI_API_URL=https://api.openai.com/v1
AI_MODEL=gpt-4o-mini
EMBEDDING_API_KEY=your_production_embedding_key
EMBEDDING_API_URL=https://api.openai.com/v1
EMBEDDING_MODEL=text-embedding-3-small

# JWT
JWT_SECRET=very_long_random_string_at_least_32_characters
JWT_EXPIRY=7d

# Server
PORT=5000
NODE_ENV=production
CORS_ORIGIN=https://ucc.co.tz,https://chat.ucc.co.tz

# Admin
ADMIN_EMAIL=admin@ucc.co.tz

# File Upload
MAX_FILE_SIZE=10485760
UPLOAD_DIR=./uploads

# RAG
CHUNK_SIZE=1000
CHUNK_OVERLAP=200
TOP_K_RESULTS=5
```

## Step 5: Build Applications

```bash
cd /var/www/ucc-chatbot

# Build backend
npm run build --workspace=backend

# Build frontend
npm run build --workspace=frontend

# Build admin
npm run build --workspace=admin
```

## Step 6: Run Migrations and Seed

```bash
# Run migrations
npm run db:migrate --workspace=backend

# Seed database
npm run db:seed --workspace=backend

# Ingest knowledge base
npx tsx scripts/ingest-knowledge-base.ts

# Generate embeddings
npx tsx scripts/generate-embeddings.ts
```

## Step 7: Configure PM2

Create `ecosystem.config.js`:

```javascript
module.exports = {
  apps: [
    {
      name: 'ucc-chatbot-backend',
      script: './backend/dist/server.js',
      instances: 1,
      exec_mode: 'cluster',
      env: {
        NODE_ENV: 'production'
      },
      log_file: './logs/backend.log',
      out_file: './logs/backend-out.log',
      error_file: './logs/backend-error.log'
    }
  ]
};
```

Start the application:

```bash
# Create logs directory
mkdir -p /var/www/ucc-chatbot/logs

# Start with PM2
cd /var/www/ucc-chatbot
pm2 start ecosystem.config.js

# Save PM2 configuration
pm2 save

# Setup PM2 to start on boot
pm2 startup
sudo env PATH=$PATH:/usr/bin pm2 startup systemd -u $USER --hp /home/$USER
```

## Step 8: Configure Nginx

Create `/etc/nginx/sites-available/ucc-chatbot`:

```nginx
server {
    listen 80;
    server_name chat.ucc.co.tz admin.ucc.co.tz;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name chat.ucc.co.tz;

    ssl_certificate /etc/letsencrypt/live/chat.ucc.co.tz/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/chat.ucc.co.tz/privkey.pem;

    root /var/www/ucc-chatbot/frontend/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:5000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }
}

server {
    listen 443 ssl http2;
    server_name admin.ucc.co.tz;

    ssl_certificate /etc/letsencrypt/live/admin.ucc.co.tz/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/admin.ucc.co.tz/privkey.pem;

    root /var/www/ucc-chatbot/admin/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:5000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }
}
```

Enable the site:

```bash
sudo ln -s /etc/nginx/sites-available/ucc-chatbot /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

## Step 9: SSL Certificate

```bash
# Install Certbot
sudo apt install -y certbot python3-certbot-nginx

# Get certificate for chat.ucc.co.tz
sudo certbot --nginx -d chat.ucc.co.tz

# Get certificate for admin.ucc.co.tz
sudo certbot --nginx -d admin.ucc.co.tz

# Test auto-renewal
sudo certbot renew --dry-run
```

## Step 10: Firewall Configuration

```bash
# Allow SSH, HTTP, and HTTPS
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Enable firewall
sudo ufw enable
```

## Step 11: Monitoring

```bash
# Check PM2 status
pm2 status

# View logs
pm2 logs ucc-chatbot-backend

# Monitor resources
pm2 monit
```

## Step 12: Backup Strategy

### Database Backup

Create `/var/www/ucc-chatbot/scripts/backup.sh`:

```bash
#!/bin/bash
BACKUP_DIR="/var/backups/ucc-chatbot"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

# Backup database
pg_dump -U ucc_user ucc_chatbot | gzip > $BACKUP_DIR/db_$TIMESTAMP.sql.gz

# Backup knowledge base files
tar -czf $BACKUP_DIR/kb_$TIMESTAMP.tar.gz /var/www/ucc-chatbot/knowledge-base/

# Keep only last 30 days
find $BACKUP_DIR -type f -mtime +30 -delete
```

Add to crontab:

```bash
sudo crontab -e

# Add line:
0 2 * * * /var/www/ucc-chatbot/scripts/backup.sh
```

## Updating the Application

```bash
cd /var/www/ucc-chatbot

# Pull latest changes
git pull origin main

# Install dependencies
npm install
cd frontend && npm install && cd ..
cd admin && npm install && cd ..

# Rebuild
npm run build --workspace=backend
npm run build --workspace=frontend
npm run build --workspace=admin

# Run migrations if needed
npm run db:migrate --workspace=backend

# Restart PM2
pm2 restart ucc-chatbot-backend

# Reload Nginx
sudo systemctl reload nginx
```

## Scaling Considerations

### Horizontal Scaling

For high traffic, run multiple backend instances:

```javascript
// ecosystem.config.js
module.exports = {
  apps: [
    {
      name: 'ucc-chatbot-backend',
      script: './backend/dist/server.js',
      instances: 'max',
      exec_mode: 'cluster',
      env: { NODE_ENV: 'production' }
    }
  ]
};
```

### Database Scaling

- Use connection pooling (already configured)
- Consider read replicas for high read workloads
- Monitor query performance with `EXPLAIN ANALYZE`

### CDN

For static assets, use a CDN like Cloudflare or AWS CloudFront.
