import { pool } from '../config/database.js';
import fs from 'fs';
import path from 'path';

const KNOWLEDGE_BASE_DIR = path.join(process.cwd(), 'knowledge-base');
const CATEGORIES = [
  'about-ucc', 'admissions', 'programmes', 'fees', 'academic',
  'registration', 'examinations', 'accommodation', 'student-services',
  'ict-support', 'professional-training', 'software-services', 'infrastructure',
  'consulting', 'campuses', 'contacts', 'news', 'events', 'regulations', 'faqs'
];

function chunkText(text: string, chunkSize: number = 1000, overlap: number = 200): string[] {
  const chunks: string[] = [];
  let start = 0;

  while (start < text.length) {
    let end = start + chunkSize;
    if (end < text.length) {
      const lastNewline = text.lastIndexOf('\n', end);
      if (lastNewline > start) {
        end = lastNewline + 1;
      }
    }
    const chunk = text.slice(start, end).trim();
    if (chunk.length > 0) {
      chunks.push(chunk);
    }
    start = end - overlap;
    if (start >= text.length) break;
  }

  return chunks;
}

async function ingestDocuments() {
  console.log('Starting document ingestion...');
  try {
    await pool.query('SELECT 1');
    console.log('Database connected');

    for (const category of CATEGORIES) {
      const categoryPath = path.join(KNOWLEDGE_BASE_DIR, category);
      if (!fs.existsSync(categoryPath)) continue;

      const files = fs.readdirSync(categoryPath).filter(f => f.endsWith('.md'));
      console.log(`Processing category: ${category} (${files.length} files)`);

      for (const file of files) {
        const filePath = path.join(categoryPath, file);
        const content = fs.readFileSync(filePath, 'utf-8');
        const title = file.replace('.md', '').replace(/-/g, ' ');

        const result = await pool.query(
          `INSERT INTO documents (title, description, category, source_type, status, effective_date)
           VALUES ($1, $2, $3, $4, $5, CURRENT_DATE)
           ON CONFLICT DO NOTHING
           RETURNING id`,
          [title, content.slice(0, 200), category, 'knowledge_base', 'ACTIVE']
        );

        const docId = result.rows[0]?.id;
        if (!docId) {
          console.log(`  Document ${title} already exists, skipping...`);
          continue;
        }

        const chunks = chunkText(content);
        console.log(`  Created document: ${title} (${chunks.length} chunks)`);

        for (let i = 0; i < chunks.length; i++) {
          await pool.query(
            'INSERT INTO document_chunks (document_id, chunk_index, chunk_text, metadata) VALUES ($1, $2, $3, $4)',
            [docId, i, chunks[i], JSON.stringify({ category, title, chunkIndex: i })]
          );
        }

        await pool.query('UPDATE documents SET is_indexed = true, indexed_at = CURRENT_TIMESTAMP WHERE id = $1', [docId]);
        console.log(`  Indexed ${chunks.length} chunks for ${title}`);
      }
    }

    console.log('Document ingestion completed');
  } catch (error) {
    console.error('Ingestion failed:', error);
    process.exit(1);
  } finally {
    await pool.end();
  }
}

ingestDocuments();
