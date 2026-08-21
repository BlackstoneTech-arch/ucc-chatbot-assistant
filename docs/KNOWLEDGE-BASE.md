# Knowledge Base Management

## Overview

The knowledge base is the authoritative source of UCC information for the chatbot. It contains official documents, FAQs, programmes, fees, contacts, and other institutional data.

## Directory Structure

```
knowledge-base/
├── about-ucc/
│   └── about-ucc.md
├── admissions/
│   └── admissions.md
├── programmes/
│   └── programmes.md
├── fees/
│   └── fees.md
├── academic/
├── registration/
│   └── registration.md
├── examinations/
│   └── examinations.md
├── accommodation/
│   └── accommodation.md
├── student-services/
│   └── student-services.md
├── ict-support/
│   └── ict-support.md
├── professional-training/
│   └── professional-training.md
├── software-services/
│   └── software-services.md
├── infrastructure/
│   └── infrastructure.md
├── consulting/
│   └── consulting.md
├── campuses/
│   └── campuses.md
├── contacts/
│   └── contacts.md
├── news/
│   └── news.md
├── events/
│   └── events.md
├── regulations/
│   └── regulations.md
├── faqs/
│   └── faqs.md
└── .index/
    └── .gitkeep
```

## Document Format

### Markdown Documents

Each knowledge base document should be a Markdown file with YAML frontmatter:

```markdown
---
title: Document Title
category: category-name
academic_year: 2026/2027
source: Official UCC brochure
status: ACTIVE
---

# Document Title

Content here...
```

### Metadata Fields

| Field | Required | Description |
|-------|----------|-------------|
| `title` | Yes | Document title |
| `category` | Yes | Category folder name |
| `academic_year` | No | Applicable academic year (e.g., "2026/2027") |
| `source` | No | Source of information |
| `status` | No | ACTIVE, ARCHIVED, or DRAFT |
| `effective_date` | No | Date the information became effective |
| `expiry_date` | No | Date the information expires |

## Categories

| Category | Description | Example Content |
|----------|-------------|-----------------|
| `about-ucc` | General UCC information | History, mission, vision |
| `admissions` | Admission procedures | Application process, deadlines |
| `programmes` | Academic programmes | Programme list, requirements |
| `fees` | Fee information | Tuition, payment methods |
| `academic` | Academic information | Calendar, regulations |
| `registration` | Course registration | Procedures, deadlines |
| `examinations` | Exam information | Schedules, rules, results |
| `accommodation` | Student housing | On-campus, off-campus options |
| `student-services` | Student support | Services, welfare |
| `ict-support` | IT support | Help desk, services |
| `professional-training` | Short courses | Professional development |
| `software-services` | Software development | Services offered |
| `infrastructure` | IT infrastructure | Network, data centre |
| `consulting` | Consulting services | Areas of expertise |
| `campuses` | Campus information | Locations, facilities |
| `contacts` | Contact information | Departments, offices |
| `news` | News and announcements | Latest updates |
| `events` | Events | Open days, information sessions |
| `regulations` | Academic regulations | Rules, policies |
| `faqs` | Frequently asked questions | Common questions and answers |

## Adding New Documents

### Step 1: Create Document

Create a new Markdown file in the appropriate category folder:

```bash
# Example: Adding a new programme
knowledge-base/programmes/bsc-data-science.md
```

### Step 2: Add Content and Metadata

```markdown
---
title: Bachelor of Science in Data Science
category: programmes
academic_year: 2026/2027
source: Official UCC brochure
status: ACTIVE
effective_date: 2026-08-01
---

# Bachelor of Science in Data Science

## Overview
The BSc Data Science programme...

## Entry Requirements
- Two advanced level principal passes...
- ...

## Duration
3 years

## Career Prospects
Graduates can work as...
```

### Step 3: Ingest into Database

```bash
npx tsx scripts/ingest-knowledge-base.ts
```

### Step 4: Generate Embeddings

```bash
npx tsx scripts/generate-embeddings.ts
```

## Updating Existing Documents

### Versioning Strategy

When information changes:

1. **Update the document** with new information
2. **Change the status** to ARCHIVED for old versions
3. **Create a new version** with ACTIVE status
4. **Update metadata** with new effective_date

### Example: Updating Fees

```markdown
---
title: Tuition Fees 2025/2026
category: fees
academic_year: 2025/2026
source: UCC Finance Office
status: ARCHIVED
effective_date: 2025-08-01
expiry_date: 2026-07-31
---
```

Create new version:

```markdown
---
title: Tuition Fees 2026/2027
category: fees
academic_year: 2026/2027
source: UCC Finance Office
status: ACTIVE
effective_date: 2026-08-01
---
```

## Document Guidelines

### Content Rules

1. **Accuracy First** - Only include verified, official UCC information
2. **Source Attribution** - Always cite the source
3. **Academic Year Separation** - Keep different years separate
4. **No Guessing** - If information is unavailable, say so
5. **Professional Tone** - Use clear, formal language

### Formatting

- Use Markdown headings for structure
- Use bullet points for lists
- Use tables for comparisons
- Include contact information where relevant
- Link to official UCC pages when possible

### Example Document

```markdown
---
title: Diploma in Computing and Information Technology
category: programmes
academic_year: 2026/2027
source: Official UCC brochure
status: ACTIVE
---

# Diploma in Computing and Information Technology (DCIT)

## Programme Overview
The DCIT is a two-year diploma programme...

## Entry Requirements
- Certificate level or equivalent
- Relevant ICT/Computer Studies background
- Minimum GPA of 2.5

## Duration
- 2 years (4 semesters)

## Programme Structure

### Year 1
- Introduction to Programming
- Computer Systems
- Mathematics for Computing
- Communication Skills

### Year 2
- Database Systems
- Web Development
- Software Engineering
- Project

## Fees
For current tuition fees, contact the Finance office or visit https://ucc.co.tz/.

## Application
Apply through the official admission portal: https://admission.ucc.co.tz/

## Contact
- Email: admissions@ucc.co.tz
- Phone: +255 22 2410 002

## Source
Official UCC Programme Guide 2026/2027
```

## Ingestion Process

### Automated Ingestion

The `ingest-knowledge-base.ts` script:

1. Reads all Markdown files from category folders
2. Extracts metadata from frontmatter
3. Chunks text into segments (default: 1000 chars with 200 overlap)
4. Creates document records in database
5. Stores chunks with metadata
6. Marks documents as indexed

### Chunking Strategy

```typescript
function chunkText(text: string, chunkSize: number = 1000, overlap: number = 200): string[] {
  const chunks: string[] = [];
  let start = 0;

  while (start < text.length) {
    let end = start + chunkSize;
    // Try to break at newlines
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
  }

  return chunks;
}
```

## Database Schema

### Documents Table

```sql
documents:
  - id (UUID, PK)
  - title (VARCHAR)
  - description (TEXT)
  - category (VARCHAR)
  - subcategory (VARCHAR)
  - file_path (TEXT)
  - file_type (VARCHAR)
  - source_url (TEXT)
  - source_type (VARCHAR)
  - academic_year (VARCHAR)
  - intake (VARCHAR)
  - status (VARCHAR) - ACTIVE, ARCHIVED, DRAFT
  - version (INTEGER)
  - effective_date (DATE)
  - expiry_date (DATE)
  - is_indexed (BOOLEAN)
  - indexed_at (TIMESTAMP)
  - created_at (TIMESTAMP)
  - updated_at (TIMESTAMP)
```

### Document Chunks Table

```sql
document_chunks:
  - id (UUID, PK)
  - document_id (UUID, FK)
  - chunk_index (INTEGER)
  - chunk_text (TEXT)
  - embedding_vector (VECTOR[1536])
  - metadata (JSONB)
  - created_at (TIMESTAMP)
```

## Admin Management

### Via Admin Dashboard

Administrators can:
- Upload new documents (PDF, DOCX, TXT, HTML)
- View and edit existing documents
- Archive or activate documents
- Re-index documents after changes
- View indexing status

### Via API

```bash
# List documents
GET /api/admin/documents?status=ACTIVE&category=programmes

# Update document
PUT /api/admin/documents/:id
{
  "title": "Updated Title",
  "status": "ACTIVE"
}

# Delete document
DELETE /api/admin/documents/:id
```

## Best Practices

1. **Regular Updates** - Review and update knowledge base regularly
2. **Version Control** - Maintain document versions for audit trails
3. **Source Verification** - Always verify information from official sources
4. **Academic Year Awareness** - Never mix information from different years
5. **Feedback Integration** - Use user feedback to improve content
6. **Accessibility** - Write clear, accessible content
