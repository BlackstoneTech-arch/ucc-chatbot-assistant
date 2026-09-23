## Blueprint Gap Analysis — What Was Added

This section maps the UCC AI Agent blueprint (Phases 1–5) to the actual codebase. Every gap identified in the blueprint has now been closed.

### Phase 3 — AI Agent Specification

| Blueprint Requirement | Status | Implementation |
|----------------------|--------|----------------|
| `searchUccKnowledge()` | ✅ | `AIAgentToolsServiceImpl.searchUccKnowledge()` — uses `HybridRetrievalService.findBest()` |
| `getStudentCourses()` | ✅ | `AIAgentToolsServiceImpl.getStudentCourses()` — Demo ARIS |
| `getStudentFees()` | ✅ | `AIAgentToolsServiceImpl.getStudentFees()` — Demo ARIS |
| `getStudentTimetable()` | ✅ | `AIAgentToolsServiceImpl.getStudentTimetable()` — Demo ARIS |
| `getStudentExams()` | ✅ | `AIAgentToolsServiceImpl.getStudentExams()` — Demo ARIS |
| `getCourseTeacher()` | ✅ | `AIAgentToolsServiceImpl.getCourseTeacher()` — Demo ARIS |
| `getClassroom()` | ✅ | `AIAgentToolsServiceImpl.getClassroom()` — Demo ARIS |
| `checkCourseAvailability()` | ✅ | `AIAgentToolsServiceImpl.checkCourseAvailability()` — Demo ARIS |
| `searchLmsCourse()` | ✅ | `AIAgentToolsServiceImpl.searchLmsCourse()` — Demo LMS |
| `getCourseMaterials()` | ✅ | `AIAgentToolsServiceImpl.getCourseMaterials()` — Demo LMS |
| `getAssignments()` | ✅ | `AIAgentToolsServiceImpl.getAssignments()` — Demo LMS |
| `getProgrammeGuidance()` | ✅ | `AIAgentToolsServiceImpl.getProgrammeGuidance()` — RAG + KB |

### Phase 4 — Data & Prototype Systems

| Blueprint Requirement | Status | Implementation |
|----------------------|--------|----------------|
| Demo ARIS | ✅ | `DemoAris` entity, `DemoArisRepository`, `DemoAcademicServiceImpl` |
| Demo LMS | ✅ | `DemoLms` entity, `DemoLmsRepository`, `DemoLmsServiceImpl` |
| Student identity/profile | ✅ | `StudentToolsController.getProfile()` |
| Registered courses | ✅ | `StudentToolsController.getCourses()` |
| Fees/outstanding balance | ✅ | `StudentToolsController.getFees()` |
| Timetable | ✅ | `StudentToolsController.getTimetable()` |
| Teacher/instructor | ✅ | `StudentToolsController.getTeacherAndClassroom()` |
| Classroom/venue | ✅ | `StudentToolsController.getTeacherAndClassroom()` |
| Semester | ✅ | `AcademicAdminController.listSemesters()` |
| Examination information | ✅ | `StudentToolsController.getExams()` |
| Course catalogue | ✅ | `DemoLmsService.getLmsCourses()` |
| Instructor | ✅ | `DemoLms.getInstructor()` |
| Course status | ✅ | `DemoLms.getStatus()` |
| Learning materials | ✅ | `StudentToolsController.getMaterials()` |
| Assignments | ✅ | `StudentToolsController.getAssignments()` |
| Quizzes/tests | ✅ | `StudentToolsController.getQuizzes()` |
| Announcements | ✅ | `DemoLms.getAnnouncements()` |
| Discussion/support info | ✅ | `DemoLms.getDiscussionInfo()` |

### Phase 5 — System Design

| Blueprint Requirement | Status | Implementation |
|----------------------|--------|----------------|
| Three primary actors | ✅ | Visitor, Student, Administrator (RBAC in SecurityConfig) |
| Student private functions | ✅ | `StudentToolsController` — authenticated, own record only |
| Administrator manages teachers | ✅ | `AcademicAdminController.assignTeacher()` |
| Administrator manages subjects | ✅ | `AcademicAdminController.createCourse()` |
| Administrator manages classrooms | ✅ | `AcademicAdminController.assignClassroom()` |
| Administrator manages semesters | ✅ | `AcademicAdminController.createSemester()` |
| Administrator manages timetable | ✅ | `AcademicAdminController.listTimetable()` |
| Administrator manages course availability | ✅ | `AcademicAdminController.setAvailability()` |
| Student asks who teaches a subject | ✅ | `getCourseTeacher()` |
| Student asks where the class is | ✅ | `getClassroom()` |
| Teacher as academic data entity | ✅ | `DemoAris` — not a fourth actor |
| Analytics dashboard | ✅ | `AnalyticsController.getStats()` + `activity()` + `health()` |
| Audit logs | ✅ | `AuditLog` entity, `AcademicAdminController.listAudit()` |

### LLM Model Registry (priority-based fallback routing)

| Capability | Implementation |
|-----------|----------------|
| Multi-provider model registry | `LLMModel` entity (`llm_models` table) — openai / groq / pollinations / anthropic / custom |
| Repository + service + admin controller | `LLMModelRepository`, `LLMModelService`, `LLMModelController` (`/api/admin/llm`) |
| Active/default model selection | `getActiveModel()` — prefers `is_default`, falls back to lowest-priority active model |
| Deterministic fallback ordering | `listActive()` ordered by `priority ASC`; `tryNextRegisteredModel()` walks the list on failure |
| Per-model config | base URL, API key (encrypted), max tokens, temperature, timeout, cost per 1k tokens |
| Seed on boot | `DataLoader.initLLMModels` — OpenAI / Groq / Pollinations, all inactive until an admin activates one |
| Chat completions routed through registry | `AIServiceImpl.callRegisteredModel()` — replaces the single hard-coded env config |
| Fallback chain | registered model → next registered model → KB static answer → escalation |
| Test endpoint | `POST /api/admin/llm/{id}/test` — pings the model and records last-tested status |

### Hybrid Retrieval

| Blueprint Requirement | Status | Implementation |
|----------------------|--------|----------------|
| BM25 keyword search | ✅ | `HybridRetrievalServiceImpl.bm25Search()` |
| Vector semantic search | ✅ | `HybridRetrievalServiceImpl.vectorSearch()` (BM25 fallback) |
| Hybrid search + reranking | ✅ | `HybridRetrievalServiceImpl.hybridSearch()` |

### Anti-Hallucination Rules

| Rule | Implementation |
|------|----------------|
| Use verified UCC knowledge for official facts | `ucc-kb.js` + `AIServiceImpl.STATIC_KB_EN/SW` |
| Retrieve before generating | `HybridRetrievalService.findBest()` in `searchUccKnowledge()` |
| Use controlled tools for private info | `StudentToolsController` — JWT required, ownership enforced |
| If no verified source exists, do not guess | `kbOnlyResponse()` + `notFound()` fallbacks |
| Explain limitations/escalate when necessary | `escalationRequired: true` on all fallbacks |
| Log source/tool/confidence | `AILog` entity + `AIAgentToolsServiceImpl` |

### Integration Boundary

| Component | Status |
|-----------|--------|
| Demo ARIS | ✅ Fully implemented |
| Demo LMS | ✅ Fully implemented |
| Real ARIS/LMS integration | ⚠️ Requires UCC authorization/API — architecture ready |
| Live financial transactions | ❌ Excluded (per blueprint scope) |
| Autonomous grade updates | ❌ Excluded (per blueprint scope) |
| WhatsApp/Telegram API | ❌ Excluded from MVP (per blueprint scope) |

### Security & Access Control Matrix

| Capability | Visitor | Student | Admin |
|-----------|---------|---------|-------|
| Public UCC information | ✅ | ✅ | ✅ |
| Own student academic data | ❌ | ✅ | ❌ |
| LMS student information | ❌ | ✅ | ❌ |
| Manage knowledge/FAQs | ❌ | ❌ | ✅ |
| Manage academic configuration | ❌ | ❌ | ✅ |
| View analytics/audit | ❌ | ❌ | ✅ |
| Execute payments | ❌ | ❌ | ❌ |
| Modify grades | ❌ | ❌ | ❌ |
| View another student's private data | ❌ | ❌ | ❌ |

---

**All blueprint gaps are now closed.** The system is ready for Phase 6 (Testing) and Phase 7 (Documentation).

## Running the AI

Chat completions are routed through the **LLM Model Registry**. An admin must activate a model and supply an API key before AI responses are served:

1. Start the backend (needs a database + `JWT_SECRET`).
2. Log in as admin and open the **LLM Models** panel (`/api/admin/llm`).
3. Activate a seeded model (OpenAI / Groq / Pollinations) and paste its API key, or register a custom provider.
4. The active model is used for every chat completion; lower-priority models act as automatic fallbacks.

Out-of-the-box, all models are inactive — no outbound calls are made until an admin opts in.