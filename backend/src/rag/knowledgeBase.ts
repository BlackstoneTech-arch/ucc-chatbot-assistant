import { Pool } from 'pg';
import { env } from '../config/env.ts';

export interface DocumentChunk {
  id: string;
  document_id: string;
  chunk_index: number;
  chunk_text: string;
  metadata: Record<string, any>;
}

export class KnowledgeBase {
  private pool: Pool;

  constructor(pool: Pool) {
    this.pool = pool;
  }

  async search(query: string, topK: number = 5, filters?: Record<string, any>): Promise<DocumentChunk[]> {
    const embedding = await this.generateEmbedding(query);
    if (!embedding) return [];

    const vectorString = `[${embedding.join(',')}]`;

    let whereClause = 'WHERE dc.embedding_vector IS NOT NULL AND d.status = \'ACTIVE\'';
    const params: any[] = [vectorString, topK];
    let paramIndex = 3;

    if (filters?.category) {
      whereClause += ` AND d.category = $${paramIndex}`;
      params.push(filters.category);
      paramIndex++;
    }

    if (filters?.academic_year) {
      whereClause += ` AND d.academic_year = $${paramIndex}`;
      params.push(filters.academic_year);
      paramIndex++;
    }

    const sql = `
      SELECT dc.id, dc.document_id, dc.chunk_index, dc.chunk_text, dc.metadata,
             1 - (dc.embedding_vector <=> $1::vector) AS similarity
      FROM document_chunks dc
      JOIN documents d ON dc.document_id = d.id
      ${whereClause}
      ORDER BY dc.embedding_vector <=> $1::vector
      LIMIT $2
    `;

    try {
      const result = await this.pool.query(sql, params);
      return result.rows.map((row) => ({
        id: row.id,
        document_id: row.document_id,
        chunk_index: row.chunk_index,
        chunk_text: row.chunk_text,
        metadata: { ...row.metadata, similarity: row.similarity },
      }));
    } catch (error) {
      console.error('Vector search error:', error);
      return [];
    }
  }

  async keywordSearch(query: string, topK: number = 5, filters?: Record<string, any>): Promise<DocumentChunk[]> {
    const searchQuery = `%${query.toLowerCase()}%`;
    let whereClause = `WHERE LOWER(dc.chunk_text) LIKE $1 AND d.status = 'ACTIVE'`;
    const params: any[] = [searchQuery];
    let paramIndex = 2;

    if (filters?.category) {
      whereClause += ` AND d.category = $${paramIndex}`;
      params.push(filters.category);
      paramIndex++;
    }

    const sql = `
      SELECT dc.id, dc.document_id, dc.chunk_index, dc.chunk_text, dc.metadata
      FROM document_chunks dc
      JOIN documents d ON dc.document_id = d.id
      ${whereClause}
      LIMIT $${paramIndex}
    `;

    params.push(topK);

    try {
      const result = await this.pool.query(sql, params);
      return result.rows.map((row) => ({
        id: row.id,
        document_id: row.document_id,
        chunk_index: row.chunk_index,
        chunk_text: row.chunk_text,
        metadata: row.metadata,
      }));
    } catch (error) {
      console.error('Keyword search error:', error);
      return [];
    }
  }

  async hybridSearch(query: string, topK: number = 5, filters?: Record<string, any>): Promise<DocumentChunk[]> {
    const semanticResults = await this.search(query, topK * 2, filters);
    const keywordResults = await this.keywordSearch(query, topK * 2, filters);

    const combined = new Map<string, DocumentChunk>();

    for (const chunk of semanticResults) {
      combined.set(chunk.id, { ...chunk, metadata: { ...chunk.metadata, _score: (chunk.metadata?.similarity || 0) * 0.7 } });
    }

    for (const chunk of keywordResults) {
      const existing = combined.get(chunk.id);
      if (existing) {
        combined.set(chunk.id, {
          ...existing,
          metadata: { ...existing.metadata, _score: existing.metadata._score + 0.3 },
        });
      } else {
        combined.set(chunk.id, { ...chunk, metadata: { ...chunk.metadata, _score: 0.3 } });
      }
    }

    return Array.from(combined.values())
      .sort((a, b) => (b.metadata?._score || 0) - (a.metadata?._score || 0))
      .slice(0, topK);
  }

  async getDocumentById(documentId: string) {
    const result = await this.pool.query('SELECT * FROM documents WHERE id = $1', [documentId]);
    return result.rows[0] || null;
  }

  async getDocuments(filters?: Record<string, any>, limit: number = 20, offset: number = 0) {
    let query = 'SELECT * FROM documents WHERE 1=1';
    const params: any[] = [];
    let paramIndex = 1;

    if (filters?.status) {
      query += ` AND status = $${paramIndex}`;
      params.push(filters.status);
      paramIndex++;
    }
    if (filters?.category) {
      query += ` AND category = $${paramIndex}`;
      params.push(filters.category);
      paramIndex++;
    }
    if (filters?.search) {
      query += ` AND (title ILIKE $${paramIndex} OR description ILIKE $${paramIndex})`;
      params.push(`%${filters.search}%`);
      paramIndex++;
    }

    query += ` ORDER BY created_at DESC LIMIT $${paramIndex} OFFSET $${paramIndex + 1}`;
    params.push(limit, offset);

    const result = await this.pool.query(query, params);
    return result.rows;
  }

  async createDocument(data: any) {
    const result = await this.pool.query(
      'INSERT INTO documents (title, description, category, subcategory, department_id, file_path, file_type, file_size, source_url, source_type, academic_year, intake, status, version, effective_date, expiry_date, uploaded_by) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17) RETURNING *',
      [data.title, data.description, data.category, data.subcategory, data.department_id, data.file_path, data.file_type, data.file_size, data.source_url, data.source_type, data.academic_year, data.intake, data.status, data.version, data.effective_date, data.expiry_date, data.uploaded_by]
    );
    return result.rows[0];
  }

  async updateDocument(id: string, data: any) {
    const fields: string[] = [];
    const params: any[] = [];
    let paramIndex = 1;

    const allowedFields = ['title', 'description', 'category', 'subcategory', 'status', 'effective_date', 'expiry_date', 'approved_by'];

    for (const field of allowedFields) {
      if (data[field] !== undefined) {
        fields.push(`${field} = $${paramIndex}`);
        params.push(data[field]);
        paramIndex++;
      }
    }

    if (fields.length === 0) return null;

    fields.push(`updated_at = CURRENT_TIMESTAMP`);
    params.push(id);

    const result = await this.pool.query(`UPDATE documents SET ${fields.join(', ')} WHERE id = $${paramIndex} RETURNING *`, params);
    return result.rows[0] || null;
  }

  async deleteDocument(id: string) {
    const result = await this.pool.query('DELETE FROM documents WHERE id = $1 RETURNING id', [id]);
    return result.rows[0] || null;
  }

  async addChunks(documentId: string, chunks: Array<{ chunk_index: number; chunk_text: string; metadata?: Record<string, any> }>) {
    const client = await this.pool.connect();
    try {
      await client.query('BEGIN');
      for (const chunk of chunks) {
        await client.query(
          'INSERT INTO document_chunks (document_id, chunk_index, chunk_text, metadata) VALUES ($1, $2, $3, $4)',
          [documentId, chunk.chunk_index, chunk.chunk_text, JSON.stringify(chunk.metadata || {})]
        );
      }
      await client.query('UPDATE documents SET is_indexed = true, indexed_at = CURRENT_TIMESTAMP WHERE id = $1', [documentId]);
      await client.query('COMMIT');
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }

  async getFAQ(filters?: { category?: string; keywords?: string[]; limit?: number }): Promise<any[]> {
    let query = 'SELECT * FROM faqs WHERE is_published = true';
    const params: any[] = [];
    let paramIndex = 1;

    if (filters?.category) {
      query += ` AND category = $${paramIndex}`;
      params.push(filters.category);
      paramIndex++;
    }

    if (filters?.keywords && filters.keywords.length > 0) {
      query += ` AND (${filters.keywords.map(() => `keywords @> $${paramIndex}`).join(' OR ')})`;
      filters.keywords.forEach(() => { params.push(['${filters!.keywords![0]}']); paramIndex++; });
    }

    query += ' ORDER BY priority DESC, created_at DESC';
    if (filters?.limit) {
      query += ` LIMIT $${paramIndex}`;
      params.push(filters.limit);
    }

    const result = await this.pool.query(query, params);
    return result.rows;
  }

  private async generateEmbedding(text: string): Promise<number[] | null> {
    try {
      const response = await fetch(env.EMBEDDING_API_URL + '/embeddings', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${env.EMBEDDING_API_KEY}`,
        },
        body: JSON.stringify({
          model: env.EMBEDDING_MODEL,
          input: text,
        }),
      });

      if (!response.ok) return null;

      const data = await response.json();
      return data.data[0].embedding;
    } catch (error) {
      console.error('Embedding generation error:', error);
      return null;
    }
  }
}
