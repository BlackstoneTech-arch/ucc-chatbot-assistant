import { pool } from '../config/database.ts';
import { Response } from 'express';
import { ChatService } from '../services/chatService.ts';

const chatService = new ChatService(pool);

export const chatController = async (req: any, res: Response) => {
  try {
    const { message, sessionId, conversationHistory = [] } = req.body;

    if (!message || typeof message !== 'string' || message.trim().length === 0) {
      return res.status(400).json({ error: 'Message is required' });
    }

    if (message.length > 2000) {
      return res.status(400).json({ error: 'Message too long. Please keep it under 2000 characters.' });
    }

    const result = await chatService.processMessage(message, conversationHistory);

    const response = {
      id: Date.now().toString(),
      message,
      response: result.response,
      sources: result.sources,
      intent: result.intent,
      confidence: result.confidence,
      escalated: result.escalated,
      timestamp: new Date().toISOString(),
    };

    res.json(response);
  } catch (error) {
    console.error('Chat error:', error);
    res.status(500).json({
      error: 'I\'m temporarily unable to process your request. Please try again shortly or contact UCC directly at https://ucc.co.tz/.',
      escalated: true,
    });
  }
};

export const getProgrammes = async (req: any, res: Response) => {
  try {
    const { level, category, academic_year, search } = req.query;
    let query = 'SELECT * FROM programmes WHERE status = \'ACTIVE\'';
    const params: any[] = [];
    let paramIndex = 1;

    if (level) { query += ` AND level ILIKE $${paramIndex}`; params.push(`%${level}%`); paramIndex++; }
    if (category) { query += ` AND category = $${paramIndex}`; params.push(category); paramIndex++; }
    if (academic_year) { query += ` AND academic_year = $${paramIndex}`; params.push(academic_year); paramIndex++; }
    if (search) { query += ` AND (title ILIKE $${paramIndex} OR description ILIKE $${paramIndex})`; params.push(`%${search}%`, `%${search}%`); paramIndex += 2; }

    query += ' ORDER BY title ASC';
    const result = await pool.query(query, params);
    res.json({ data: result.rows, total: result.rowCount });
  } catch (error) {
    console.error('Get programmes error:', error);
    res.status(500).json({ error: 'Failed to fetch programmes' });
  }
};

export const getProgrammeById = async (req: any, res: Response) => {
  try {
    const { id } = req.params;
    const result = await pool.query('SELECT * FROM programmes WHERE id = $1', [id]);
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Programme not found' });
    }
    res.json({ data: result.rows[0] });
  } catch (error) {
    console.error('Get programme error:', error);
    res.status(500).json({ error: 'Failed to fetch programme' });
  }
};

export const getFAQs = async (req: any, res: Response) => {
  try {
    const { category, search } = req.query;
    let query = 'SELECT * FROM faqs WHERE is_published = true';
    const params: any[] = [];
    let paramIndex = 1;

    if (category) { query += ` AND category = $${paramIndex}`; params.push(category); paramIndex++; }
    if (search) { query += ` AND (question ILIKE $${paramIndex} OR answer ILIKE $${paramIndex})`; params.push(`%${search}%`, `%${search}%`); paramIndex += 2; }

    query += ' ORDER BY priority DESC, created_at DESC';
    const result = await pool.query(query, params);
    res.json({ data: result.rows, total: result.rowCount });
  } catch (error) {
    console.error('Get FAQs error:', error);
    res.status(500).json({ error: 'Failed to fetch FAQs' });
  }
};

export const getContacts = async (req: any, res: Response) => {
  try {
    const { department_id } = req.query;
    let query = 'SELECT * FROM contacts WHERE status = \'ACTIVE\'';
    const params: any[] = [];
    let paramIndex = 1;

    if (department_id) { query += ` AND department_id = $${paramIndex}`; params.push(department_id); paramIndex++; }

    query += ' ORDER BY is_primary DESC, name ASC';
    const result = await pool.query(query, params);
    res.json({ data: result.rows, total: result.rowCount });
  } catch (error) {
    console.error('Get contacts error:', error);
    res.status(500).json({ error: 'Failed to fetch contacts' });
  }
};

export const submitFeedback = async (req: any, res: Response) => {
  try {
    const { messageId, conversationId, rating, comment, feedbackType } = req.body;
    const userId = req.user?.id;

    const result = await pool.query(
      'INSERT INTO feedback (message_id, conversation_id, user_id, rating, comment, feedback_type) VALUES ($1, $2, $3, $4, $5, $6) RETURNING *',
      [messageId, conversationId, userId, rating, comment, feedbackType || 'response']
    );

    res.status(201).json({ data: result.rows[0] });
  } catch (error) {
    console.error('Submit feedback error:', error);
    res.status(500).json({ error: 'Failed to submit feedback' });
  }
};
