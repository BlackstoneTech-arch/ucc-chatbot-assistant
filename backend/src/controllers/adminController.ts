import { Response } from 'express';
import { pool } from '../config/database.ts';
import { AuthRequest } from '../middleware/auth.ts';
import bcrypt from 'bcryptjs';

export const getAnalytics = async (req: AuthRequest, res: Response) => {
  try {
    const [conversations, messages, feedback, intents] = await Promise.all([
      pool.query('SELECT COUNT(*) FROM conversations WHERE started_at >= NOW() - INTERVAL \'30 days\''),
      pool.query('SELECT COUNT(*) FROM messages WHERE created_at >= NOW() - INTERVAL \'30 days\''),
      pool.query('SELECT AVG(rating) as avg_rating, COUNT(*) as total FROM feedback WHERE created_at >= NOW() - INTERVAL \'30 days\''),
      pool.query('SELECT intent, COUNT(*) as count FROM intent_logs WHERE created_at >= NOW() - INTERVAL \'30 days\' GROUP BY intent ORDER BY count DESC LIMIT 10'),
    ]);

    res.json({
      totalConversations: parseInt(conversations.rows[0].count),
      totalMessages: parseInt(messages.rows[0].count),
      averageRating: parseFloat(feedback.rows[0].avg_rating || 0).toFixed(2),
      totalFeedback: parseInt(feedback.rows[0].total),
      topIntents: intents.rows,
    });
  } catch (error) {
    console.error('Analytics error:', error);
    res.status(500).json({ error: 'Failed to fetch analytics' });
  }
};

export const getConversations = async (req: AuthRequest, res: Response) => {
  try {
    const { page = 1, limit = 20, isActive } = req.query;
    const offset = (Number(page) - 1) * Number(limit);

    let whereClause = 'WHERE 1=1';
    const params: any[] = [];
    let paramIndex = 1;

    if (isActive !== undefined) {
      whereClause += ` AND c.is_active = $${paramIndex}`;
      params.push(isActive === 'true');
      paramIndex++;
    }

    const result = await pool.query(
      `SELECT c.*, u.email as user_email, u.full_name as user_name,
              (SELECT COUNT(*) FROM messages m WHERE m.conversation_id = c.id) as message_count
       FROM conversations c
       LEFT JOIN users u ON c.user_id = u.id
       ${whereClause}
       ORDER BY c.started_at DESC
       LIMIT $${paramIndex} OFFSET $${paramIndex + 1}`,
      [...params, Number(limit), offset]
    );

    const countResult = await pool.query(`SELECT COUNT(*) FROM conversations c ${whereClause}`, params);

    res.json({
      data: result.rows,
      total: parseInt(countResult.rows[0].count),
      page: Number(page),
      limit: Number(limit),
    });
  } catch (error) {
    console.error('Get conversations error:', error);
    res.status(500).json({ error: 'Failed to fetch conversations' });
  }
};

export const getConversationMessages = async (req: AuthRequest, res: Response) => {
  try {
    const { id } = req.params;
    const result = await pool.query('SELECT * FROM messages WHERE conversation_id = $1 ORDER BY created_at ASC', [id]);
    res.json({ data: result.rows });
  } catch (error) {
    console.error('Get messages error:', error);
    res.status(500).json({ error: 'Failed to fetch messages' });
  }
};

export const updateDocument = async (req: AuthRequest, res: Response) => {
  try {
    const { id } = req.params;
    const updates = req.body;

    const fields: string[] = [];
    const params: any[] = [];
    let paramIndex = 1;

    const allowedFields = ['title', 'description', 'category', 'subcategory', 'status', 'effective_date', 'expiry_date', 'approved_by'];

    for (const field of allowedFields) {
      if (updates[field] !== undefined) {
        fields.push(`${field} = $${paramIndex}`);
        params.push(updates[field]);
        paramIndex++;
      }
    }

    if (fields.length === 0) {
      return res.status(400).json({ error: 'No valid fields to update' });
    }

    fields.push('updated_at = CURRENT_TIMESTAMP');
    params.push(id);

    const result = await pool.query(`UPDATE documents SET ${fields.join(', ')} WHERE id = $${paramIndex} RETURNING *`, params);

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Document not found' });
    }

    res.json({ data: result.rows[0] });
  } catch (error) {
    console.error('Update document error:', error);
    res.status(500).json({ error: 'Failed to update document' });
  }
};

export const deleteDocument = async (req: AuthRequest, res: Response) => {
  try {
    const { id } = req.params;
    const result = await pool.query('DELETE FROM documents WHERE id = $1 RETURNING id', [id]);

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Document not found' });
    }

    res.json({ message: 'Document deleted successfully' });
  } catch (error) {
    console.error('Delete document error:', error);
    res.status(500).json({ error: 'Failed to delete document' });
  }
};

export const getDocuments = async (req: AuthRequest, res: Response) => {
  try {
    const { status, category, search, page = 1, limit = 20 } = req.query;
    const offset = (Number(page) - 1) * Number(limit);

    let query = 'SELECT * FROM documents WHERE 1=1';
    const params: any[] = [];
    let paramIndex = 1;

    if (status) { query += ` AND status = $${paramIndex}`; params.push(status); paramIndex++; }
    if (category) { query += ` AND category = $${paramIndex}`; params.push(category); paramIndex++; }
    if (search) { query += ` AND (title ILIKE $${paramIndex} OR description ILIKE $${paramIndex})`; params.push(`%${search}%`, `%${search}%`); paramIndex += 2; }

    query += ` ORDER BY created_at DESC LIMIT $${paramIndex} OFFSET $${paramIndex + 1}`;
    params.push(Number(limit), offset);

    const result = await pool.query(query, params);
    const countResult = await pool.query(`SELECT COUNT(*) FROM documents WHERE 1=1 ${status ? 'AND status = $1' : ''}`, status ? [status] : []);

    res.json({
      data: result.rows,
      total: parseInt(countResult.rows[0].count),
      page: Number(page),
      limit: Number(limit),
    });
  } catch (error) {
    console.error('Get documents error:', error);
    res.status(500).json({ error: 'Failed to fetch documents' });
  }
};

export const createFAQ = async (req: AuthRequest, res: Response) => {
  try {
    const { question, answer, category, subcategory, keywords, priority, programme_id, department_id } = req.body;
    const result = await pool.query(
      'INSERT INTO faqs (question, answer, category, subcategory, keywords, priority, programme_id, department_id, created_by) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9) RETURNING *',
      [question, answer, category, subcategory, keywords, priority || 0, programme_id, department_id, req.user!.id]
    );
    res.status(201).json({ data: result.rows[0] });
  } catch (error) {
    console.error('Create FAQ error:', error);
    res.status(500).json({ error: 'Failed to create FAQ' });
  }
};

export const getKnowledgeBaseStats = async (req: AuthRequest, res: Response) => {
  try {
    const [docCount, chunkCount, faqCount, activeDocCount] = await Promise.all([
      pool.query('SELECT COUNT(*) FROM documents'),
      pool.query('SELECT COUNT(*) FROM document_chunks'),
      pool.query('SELECT COUNT(*) FROM faqs WHERE is_published = true'),
      pool.query('SELECT COUNT(*) FROM documents WHERE status = \'ACTIVE\''),
    ]);

    res.json({
      totalDocuments: parseInt(docCount.rows[0].count),
      totalChunks: parseInt(chunkCount.rows[0].count),
      totalFAQs: parseInt(faqCount.rows[0].count),
      activeDocuments: parseInt(activeDocCount.rows[0].count),
    });
  } catch (error) {
    console.error('Stats error:', error);
    res.status(500).json({ error: 'Failed to fetch stats' });
  }
};
