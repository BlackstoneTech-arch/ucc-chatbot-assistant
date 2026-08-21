import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import compression from 'compression';
import rateLimit from 'express-rate-limit';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';
import { corsMiddleware, securityHeaders } from './middleware/security.ts';
import { requestId, errorHandler } from './middleware/errorHandler.ts';
import chatRoutes from './routes/chat.ts';
import authRoutes from './routes/auth.ts';
import adminRoutes from './routes/admin.ts';
import { pool } from './config/database.ts';
import { env } from './config/env.ts';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(__dirname, '../..');

dotenv.config();

const app = express();

app.use(compression());
app.use(corsMiddleware);
app.use(securityHeaders);
app.use(helmet({
  contentSecurityPolicy: false,
  crossOriginEmbedderPolicy: false,
}));
app.use(morgan('combined'));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));
app.use(requestId);

app.use('/api/chat', rateLimit({ windowMs: 60000, max: 30 }), chatRoutes);
app.use('/api/auth', authRoutes);
app.use('/api/admin', adminRoutes);

app.get('/api/health', async (req, res) => {
  try {
    await pool.query('SELECT 1');
    res.json({ status: 'healthy', database: 'connected', timestamp: new Date().toISOString() });
  } catch (error) {
    res.status(500).json({ status: 'unhealthy', database: 'disconnected', timestamp: new Date().toISOString() });
  }
});

app.get('/api/programmes', (req, res) => {
  res.json({ message: 'Use /api/chat/programmes endpoint' });
});

app.use(express.static(path.join(projectRoot, 'frontend')));
app.use('/admin', express.static(path.join(projectRoot, 'admin')));

app.get('/', (req, res) => {
  res.sendFile(path.join(projectRoot, 'frontend', 'index.html'));
});

app.get('/admin', (req, res) => {
  res.sendFile(path.join(projectRoot, 'admin', 'index.html'));
});

app.get('/admin/dashboard', (req, res) => {
  res.sendFile(path.join(projectRoot, 'admin', 'dashboard.html'));
});

app.use((req, res) => {
  res.status(404).json({ error: 'Route not found' });
});

app.use(errorHandler);

const PORT = env.PORT;

app.listen(PORT, () => {
  console.log(`UCC Chatbot Assistant backend running on port ${PORT}`);
  console.log(`Environment: ${env.NODE_ENV}`);
  console.log(`Health check: http://localhost:${PORT}/api/health`);
});

export default app;
