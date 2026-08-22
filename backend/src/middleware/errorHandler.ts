import { Request, Response, NextFunction } from 'express';
import { v4 as uuidv4 } from 'uuid';
import { env } from '../config/env.ts';

export const requestId = (req: Request, res: Response, next: NextFunction) => {
  (req as any).requestId = uuidv4();
  res.setHeader('X-Request-ID', (req as any).requestId);
  next();
};

export const errorHandler = (err: any, req: any, res: Response, next: any) => {
  const requestId = (req as any).requestId || 'unknown';
  console.error(`[${requestId}] Error:`, err);

  const statusCode = err.statusCode || 500;
  const message = err.message || 'Internal server error';

  res.status(statusCode).json({
    error: message,
    requestId,
    ...(env.NODE_ENV === 'development' && { stack: err.stack }),
  });
};
