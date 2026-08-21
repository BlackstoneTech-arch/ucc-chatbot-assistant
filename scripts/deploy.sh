#!/bin/bash
set -e

echo "=========================================="
echo "UCC Chatbot Assistant - Deployment Script"
echo "=========================================="
echo ""

# Configuration
APP_NAME="ucc-chatbot-assistant"
APP_DIR="/var/www/$APP_NAME"
SERVICE_USER="www-data"
NGINX_SITE="ucc-chatbot"

echo "Step 1: Updating system..."
sudo apt update && sudo apt upgrade -y

echo "Step 2: Installing dependencies..."
sudo apt install -y git nodejs npm postgresql postgresql-contrib nginx certbot python3-certbot-nginx

echo "Step 3: Creating application directory..."
sudo mkdir -p $APP_DIR
sudo chown -R $USER:$USER $APP_DIR

echo "Step 4: Cloning repository..."
if [ -d "$APP_DIR/.git" ]; then
    echo "Repository exists, pulling latest changes..."
    cd $APP_DIR
    git pull origin main
else
    echo "Cloning repository..."
    git clone <repository-url> $APP_DIR
    cd $APP_DIR
fi

echo "Step 5: Installing Node.js dependencies..."
npm install
cd backend && npm install && cd ..

echo "Step 6: Setting up environment..."
if [ ! -f .env ]; then
    cp .env.example .env
    echo "Created .env file. Please edit it with your configuration:"
    echo "  nano $APP_DIR/.env"
    exit 1
fi

echo "Step 7: Installing PM2..."
sudo npm install -g pm2

echo "Step 8: Building application..."
npm run build --workspace=backend

echo "Step 9: Running database migrations..."
npm run db:migrate --workspace=backend

echo "Step 10: Seeding database..."
npm run db:seed --workspace=backend

echo "Step 11: Ingesting knowledge base..."
npx tsx scripts/ingest-knowledge-base.ts
npx tsx scripts/generate-embeddings.ts

echo "Step 12: Configuring PM2..."
sudo tee /etc/systemd/system/ucc-chatbot.service > /dev/null <<EOF
[Unit]
Description=UCC Chatbot Assistant
After=network.target postgresql.service

[Service]
Type=simple
User=$SERVICE_USER
WorkingDirectory=$APP_DIR/backend
ExecStart=/usr/bin/node dist/server.js
Restart=always
RestartSec=10
Environment=NODE_ENV=production

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable ucc-chatbot
sudo systemctl start ucc-chatbot

echo "Step 13: Configuring Nginx..."
sudo tee /etc/nginx/sites-available/$NGINX_SITE > /dev/null <<EOF
server {
    listen 80;
    server_name chat.ucc.co.tz admin.ucc.co.tz;
    return 301 https://\$server_name\$request_uri;
}

server {
    listen 443 ssl http2;
    server_name chat.ucc.co.tz;

    ssl_certificate /etc/letsencrypt/live/chat.ucc.co.tz/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/chat.ucc.co.tz/privkey.pem;

    root $APP_DIR/frontend;
    index index.html;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:5000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_cache_bypass \$http_upgrade;
    }
}

server {
    listen 443 ssl http2;
    server_name admin.ucc.co.tz;

    ssl_certificate /etc/letsencrypt/live/admin.ucc.co.tz/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/admin.ucc.co.tz/privkey.pem;

    root $APP_DIR/admin;
    index index.html;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:5000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_cache_bypass \$http_upgrade;
    }
}
EOF

sudo ln -sf /etc/nginx/sites-available/$NGINX_SITE /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx

echo "Step 14: Setting up SSL..."
sudo certbot --nginx -d chat.ucc.co.tz -d admin.ucc.co.tz

echo "Step 15: Setting up firewall..."
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw --force enable

echo "Step 16: Setting up backup..."
sudo tee /etc/cron.daily/ucc-chatbot-backup > /dev/null <<EOF
#!/bin/bash
BACKUP_DIR="$APP_DIR/database/backups"
TIMESTAMP=\$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR
pg_dump -U ucc_user ucc_chatbot | gzip > $BACKUP_DIR/db_\$TIMESTAMP.sql.gz
find $BACKUP_DIR -type f -mtime +30 -delete
EOF
sudo chmod +x /etc/cron.daily/ucc-chatbot-backup

echo ""
echo "=========================================="
echo "Deployment Complete!"
echo "=========================================="
echo "Application URL: https://chat.ucc.co.tz"
echo "Admin URL: https://admin.ucc.co.tz"
echo "Logs: sudo journalctl -u ucc-chatbot -f"
echo "=========================================="
