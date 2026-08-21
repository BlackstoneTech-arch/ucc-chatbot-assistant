import { pool } from '../config/database.ts';

async function migrate() {
  console.log('Running database migrations...');
  try {
    await pool.query('SELECT 1');
    console.log('Database connection successful');

    const schemaPath = new URL('../database/schema.sql', import.meta.url).pathname;
    const schema = await import('fs').then(fs => fs.readFileSync(schemaPath, 'utf-8'));

    await pool.query(schema);
    console.log('Schema migration completed');
  } catch (error) {
    console.error('Migration failed:', error);
    process.exit(1);
  } finally {
    await pool.end();
  }
}

migrate();
