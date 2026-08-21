import { pool } from '../config/database.js';
import { env } from '../config/env.js';

async function generateEmbeddings() {
  console.log('Generating embeddings for document chunks...');
  try {
    await pool.query('SELECT 1');

    const result = await pool.query('SELECT id, chunk_text FROM document_chunks WHERE embedding_vector IS NULL LIMIT 1000');
    console.log(`Found ${result.rows.length} chunks without embeddings`);

    for (const row of result.rows) {
      try {
        const response = await fetch(`${env.EMBEDDING_API_URL}/embeddings`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${env.EMBEDDING_API_KEY}`,
          },
          body: JSON.stringify({
            model: env.EMBEDDING_MODEL,
            input: row.chunk_text.slice(0, 8000),
          }),
        });

        if (!response.ok) {
          console.error(`Failed to embed chunk ${row.id}`);
          continue;
        }

        const data = await response.json();
        const embedding = data.data[0].embedding;
        const vectorString = `[${embedding.join(',')}]`;

        await pool.query('UPDATE document_chunks SET embedding_vector = $1::vector WHERE id = $2', [vectorString, row.id]);
        console.log(`Embedded chunk ${row.id}`);
      } catch (error) {
        console.error(`Error embedding chunk ${row.id}:`, error);
      }
    }

    console.log('Embedding generation completed');
  } catch (error) {
    console.error('Embedding generation failed:', error);
    process.exit(1);
  } finally {
    await pool.end();
  }
}

generateEmbeddings();
