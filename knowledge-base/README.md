# UCC Knowledge Base

This directory contains the verified, human-curated knowledge base for the UCC chatbot.

## Files

Each topic lives in its own Markdown file under `knowledge-base/<topic>/<topic>.md`.

| File | Topic |
|------|-------|
| `about-ucc/about-ucc.md` | UCC overview, vision, mission, values, services, branches, partners |
| `academic/academic.md` | Academic year, semesters, entry qualifications, registration |
| `accommodation/accommodation.md` | On/off-campus housing |
| `admissions/admissions.md` | Current intake, portal, process, programmes, entry qualifications, documents |
| `campuses/campuses.md` | Campus locations and addresses |
| `contacts/contacts.md` | Phone, email, branches, social media, office hours |
| `consulting/consulting.md` | ICT consulting services |
| `events/events.md` | Open days, info sessions, career fairs |
| `examinations/examinations.md` | Exam schedule, rules, results, graduation |
| `faqs/faqs.md` | Frequently asked questions (Q&A format) |
| `fees/fees.md` | Fee structures for DCIT, DBIT, CCIT, CBIT, professional courses |
| `ict-support/ict-support.md` | Student ICT support, lab access, LMS, software |
| `infrastructure/infrastructure.md` | IT infrastructure, hosting, security, managed services |
| `news/news.md` | Latest news and announcements |
| `professional-training/professional-training.md` | 24+ professional courses, Pearson VUE, durations |
| `programmes/programmes.md` | Academic programmes (DCIT, DBIT, CCIT, CBIT) and professional courses |
| `registration/registration.md` | Course registration process |
| `regulations/regulations.md` | Academic and examination regulations |
| `software-services/software-services.md` | 6 software products (ARIS, OLASS, IFMIS, eTac, HMS, MES) |
| `student-services/student-services.md` | Registration, academic calendar, support services |

## How the KB is used

1. **Static fallback** — `frontend/js/ucc-kb.js` embeds the most important facts (programmes, fees, admissions, contacts) as a client-side map. When the backend API is unreachable, the chat widget answers from this map.

2. **Backend injection** — `AIServiceImpl.STATIC_KB_EN` / `STATIC_KB_SW` (Java) mirrors the same verified facts and is injected into the LLM system prompt as "VERIFIED UCC KNOWLEDGE BASE SNIPPET" so the model never hallucinates.

3. **RAG retrieval** — `KnowledgeDocument` entities in MySQL are populated from the admin dashboard or website sync. `HybridRetrievalService` performs BM25 + vector search over them.

## Verification rules

- Every fact must be traceable to `https://ucc.co.tz/` or an official UCC document.
- Do **not** guess fees, dates, or contact details. If a fact is not verified, mark it as "Contact UCC for current information."
- Academic calendar and timetable are **not** published online — only distributed to registered students. Never invent dates for these.
- Application fee: TZS **15,000** local / TZS **30,000** foreign (verified from ucc.co.tz).
- Reporting: **November 2026** (intake label: October 2026/2027).
- Application window: 1 June – 30 September 2026.
- Official contacts: `ucc@udsm.ac.tz`, `info@ucc.co.tz`, `+255 22 2410641/5` (Dar landline), `+255 754782120` (mobile), `+255 0747 626 619` (Dodoma).

## Updating

1. Edit the Markdown file in this directory.
2. Update the matching entry in `frontend/js/ucc-kb.js` and/or `AIServiceImpl.java` (STATIC_KB_EN / STATIC_KB_SW).
3. If the fact comes from a new/changed page on ucc.co.tz, run the **Website Sync** in the admin dashboard to re-index.