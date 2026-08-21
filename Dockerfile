FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production

COPY backend/package*.json ./backend/
RUN cd backend && npm ci --only=production

COPY backend/src ./backend/src
COPY backend/tsconfig*.json ./backend/
RUN cd backend && npm run build

FROM node:20-alpine

WORKDIR /app

COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/backend/node_modules ./backend/node_modules
COPY --from=builder /app/backend/dist ./backend/dist
COPY --from=builder /app/backend/package.json ./backend/
COPY frontend ./frontend
COPY admin ./admin
COPY knowledge-base ./knowledge-base

EXPOSE 5000

CMD ["npm", "run", "start", "--workspace=backend"]
