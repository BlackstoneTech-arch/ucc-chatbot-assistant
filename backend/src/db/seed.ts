import { pool } from '../config/database.ts';
import bcrypt from 'bcryptjs';

async function seed() {
  console.log('Seeding database...');
  try {
    await pool.query('SELECT 1');

    const passwordHash = await bcrypt.hash('admin123', 12);

    await pool.query(
      `INSERT INTO users (email, password_hash, full_name, role)
       VALUES ($1, $2, $3, $4)
       ON CONFLICT (email) DO NOTHING`,
      ['admin@ucc.co.tz', passwordHash, 'UCC Admin', 'superadmin']
    );

    const departments = [
      { name: 'Academic Affairs', description: 'Academic programmes and student registration', email: 'academic@ucc.co.tz', phone: '+255 22 2410 001' },
      { name: 'Admissions', description: 'Student admissions and applications', email: 'admissions@ucc.co.tz', phone: '+255 22 2410 002' },
      { name: 'ICT Support', description: 'Technical support and IT services', email: 'ict@ucc.co.tz', phone: '+255 22 2410 003' },
      { name: 'Finance', description: 'Fees, payments, and financial services', email: 'finance@ucc.co.tz', phone: '+255 22 2410 004' },
      { name: 'Student Services', description: 'Student welfare and services', email: 'students@ucc.co.tz', phone: '+255 22 2410 005' },
    ];

    for (const dept of departments) {
      await pool.query(
        `INSERT INTO departments (name, description, email, phone)
         VALUES ($1, $2, $3, $4)
         ON CONFLICT DO NOTHING`,
        [dept.name, dept.description, dept.email, dept.phone]
      );
    }

    console.log('Database seeded successfully');
  } catch (error) {
    console.error('Seeding failed:', error);
    process.exit(1);
  } finally {
    await pool.end();
  }
}

seed();
