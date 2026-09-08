/* ============================================
   UCC Customer Care Assistant — Frontend Controller
   - Welcome + quick-reply chips
   - EN / SW language detection
   - Live backend API OR local KB fallback
   - localStorage history (7-day retention)
   - Feedback widget (thumbs up/down + comment)
   - Accessibility: ARIA roles, live region, focus, reduced motion
   - Online/offline detection + retry
   - No external dependencies
   ============================================ */

(function () {
  'use strict';

  // ---------- Constants ----------
  const STORAGE_KEYS = {
    SESSION: 'ucc_chat_session',
    HISTORY: 'ucc_chat_history',
    LANG: 'ucc_chat_lang',
    PENDING_FEEDBACK: 'ucc_chat_pending_feedback'
  };
  const HISTORY_TTL_MS = 7 * 24 * 60 * 60 * 1000;
  const LANG_TTL_MS = 365 * 24 * 60 * 60 * 1000;
  const MAX_HISTORY_MESSAGES = 200;
  const FEEDBACK_BATCH_SIZE = 10;

  // ---------- State ----------
  const state = {
    sessionId: getOrCreateSessionId(),
    history: loadHistory(),
    isProcessing: false,
    welcomeLoaded: false,
    detectedLang: loadLangPref(),
    conversationHistory: []
  };

  // ---------- Storage helpers ----------
  function getOrCreateSessionId() {
    try {
      const existing = localStorage.getItem(STORAGE_KEYS.SESSION);
      if (existing && isFresh(existing)) return JSON.parse(existing).id;
    } catch (_) {}
    const id = (crypto && crypto.randomUUID) ? crypto.randomUUID() : ('sess-' + Date.now() + '-' + Math.random().toString(36).slice(2, 10));
    saveWithTimestamp(STORAGE_KEYS.SESSION, { id }, HISTORY_TTL_MS);
    return id;
  }

  function saveWithTimestamp(key, value, ttlMs) {
    try {
      localStorage.setItem(key, JSON.stringify({ value, ts: Date.now(), ttl: ttlMs }));
    } catch (_) {}
  }

  function loadWithTimestamp(key) {
    try {
      const raw = localStorage.getItem(key);
      if (!raw) return null;
      const parsed = JSON.parse(raw);
      if (!isFresh(parsed)) {
        localStorage.removeItem(key);
        return null;
      }
      return parsed.value;
    } catch (_) { return null; }
  }

  function isFresh(stored) {
    if (!stored || typeof stored !== 'object') return false;
    if (!stored.ts || !stored.ttl) return true;
    return (Date.now() - stored.ts) < stored.ttl;
  }

  function loadHistory() {
    const h = loadWithTimestamp(STORAGE_KEYS.HISTORY);
    return Array.isArray(h) ? h.slice(-MAX_HISTORY_MESSAGES) : [];
  }

  function saveHistory() {
    saveWithTimestamp(STORAGE_KEYS.HISTORY, state.history.slice(-MAX_HISTORY_MESSAGES), HISTORY_TTL_MS);
  }

  function loadLangPref() {
    return loadWithTimestamp(STORAGE_KEYS.LANG) || 'en';
  }

  function saveLangPref(lang) {
    state.detectedLang = lang;
    saveWithTimestamp(STORAGE_KEYS.LANG, lang, LANG_TTL_MS);
  }

  function clearAllStoredData() {
    Object.values(STORAGE_KEYS).forEach(k => {
      try { localStorage.removeItem(k); } catch (_) {}
    });
    state.sessionId = getOrCreateSessionId();
    state.history = [];
  }

  // ---------- Language detection ----------
  function detectLanguage(message) {
    const lower = (message || '').toLowerCase();
    const swStems = [
      'habari', 'hujambo', 'kwaheri', 'asante', 'karibu', 'tafadhali', 'naomba',
      'ninataka', 'nimekuwa', 'ninaweza', 'kwanini', 'vipi', 'wapi', 'lini',
      'ndiyo', 'hapana', 'labda', 'pia', 'bado', 'sasa', 'kesho', 'jana',
      'programu', 'kozi', 'ada', 'masomo', 'chuo', 'udahili', 'usajili', 'mitihani',
      'diploma', 'cheti', 'msaada', 'mwalimu', 'mwanafunzi',
      'kuhusu', 'kwa nini', 'kwa hiyo', 'kwa sababu', 'pamoja', 'bila', 'baada ya',
      'kabla ya', 'tena', 'tayari', 'kisha', 'halafu', 'sasa hivi',
      'mkuu', 'uchumi', 'biashara', 'afya', 'lugha', 'maneno', 'kitabu',
      'nataka', 'natafuta', 'kujiunga', 'nimehitaji', 'ningependa',
      'maombi', 'ada ya', 'kiasi gani', 'pakua', 'hati', 'karatasi', 'kalenda', 'ratiba', 'muhtasari', 'brochure', 'brochure', 'wasiliana nasi'
    ];
    let swScore = 0, enScore = 0;
    for (const w of swStems) {
      if (lower.includes(w)) swScore += 2;
    }
    if (/\b(the|what|how|when|where|why|which|can|could|would|should|will|may|tuition|fees|program|course|admission|application|apply|contact|email|address|university|download|brochure|prospectus|timetable|calendar)\b/.test(lower)) enScore += 1;
    return swScore > enScore ? 'sw' : 'en';
  }

  // ---------- Document / IT-help intent detection ----------
  async function tryDocumentResponse(message) {
    if (!message) return null;
    const lower = message.toLowerCase();
    const D = window.UCCDocuments;
    if (!D) return null;
    await D.render && (D._loaded || await D.render().then(() => { D._loaded = true; }).catch(() => {}));

    const all = D.getAllDocs ? D.getAllDocs() : [];
    if (!all.length) return null;

    const lang = state.detectedLang || 'en';
    const isSw = lang === 'sw';

    // 1) Application pack
    const packTriggers = [
      'application pack', 'admission pack', 'apply pack', 'intake pack', 'app pack',
      'all documents', 'all admission documents', 'documents for application',
      'pakua', 'hati zote', 'hati za maombi', 'maombi pack', 'pakua zote', 'hati zangu'
    ];
    if (packTriggers.some(t => lower.includes(t)) || (lower.includes('documents') && lower.includes('apply'))) {
      const pack = D.getIntakePack && D.getIntakePack();
      if (pack && pack.documents && pack.documents.length) {
        return {
          answer: isSw
            ? `Hapa kuna **Pak ya Maombi ya Oktoba 2026/2027** — ${pack.documents.length} hati rasmi za UCC. Bofya kila kiungo kupakua moja kwa moja kwenye ucc.co.tz.\n\n💡 Unaweza pia kuomba **msaada wa moja kwa moja wa maombi** — naomba tuambie kama unataka kuomba DCIT, DBIT, CCIT, au CBIT.`
            : `Here is the **October 2026/2027 Application Pack** — ${pack.documents.length} official UCC documents. Click each link to download directly from ucc.co.tz.\n\n💡 I can also walk you through the application step-by-step — just tell me which programme you want to apply to (DCIT, DBIT, CCIT, or CBIT).`,
          downloads: pack.documents,
          quickReplies: isSw
            ? [
                { label: 'Naomba DCIT', message: 'Naomba kusaidiwa kuomba DCIT' },
                { label: 'Ada ya maombi', message: 'Ada ya maombi ni kiasi gani?' },
                { label: 'Lini maombi?', message: 'Lini maombi yanafunguliwa na yanafungwa?' }
              ]
            : [
                { label: 'Apply for DCIT', message: 'Help me apply for DCIT' },
                { label: 'Application fee', message: 'How much is the application fee?' },
                { label: 'When do I apply?', message: 'When do applications open and close?' }
              ],
          confidence: 0.95
        };
      }
    }

    // 2) Calendar / timetable
    const calendarTriggers = ['calendar', 'academic calendar', 'semester dates', 'term dates', 'school calendar', 'kalenda', 'kalenda ya masomo', 'tarehe za semesta'];
    const timetableTriggers = ['timetable', 'class schedule', 'class timetable', 'my class', 'schedule of classes', 'ratiba', 'ratiba ya masomo', 'ratiba ya darasa'];
    if (timetableTriggers.some(t => lower.includes(t))) {
      return {
        answer: isSw
          ? `**Ratiba za madarasa** huchapishwa kwa kila semesta na kitivo. Kwa sasa ratiba rasmi za DCIT/DBIT/CCIT/CBIT haziko kwenye tovuti ya UCC — zinapostiwa kwenye mabango ya kampasi na kwenye portal ya wanafunzi mwanzoni mwa kila semesta.\n\n**Ili kupata ratiba yako:**\n1. Tembelea kampasi yako (UCC HQ Mlimani au Dodoma Branch) mwanzo wa semesta\n2. Angalia mabango ya matangazo ya idara\n3. Wasiliana nasi kwa barua pepe: **ucc@udsm.ac.tz**\n4. Piga simu: **+255 22 2410641/5** (Dar) / **+255 754782120** (simu) / **+255 0747 626 619** (Dodoma)\n\n💡 Kwa sasa unaweza kupakua **brochure ya programu yako** hapa chini kwa maelezo ya jumla ya kozi.`
          : `**Class timetables** are published per semester by the academic office. The official timetables for DCIT, DBIT, CCIT, and CBIT are not currently hosted online — they are posted on the campus notice boards and the student portal at the start of each semester.\n\n**To get your timetable:**\n1. Visit your campus (UCC HQ Mlimani or Dodoma Branch) at the start of the semester\n2. Check the departmental notice boards\n3. Email us at: **ucc@udsm.ac.tz**\n4. Call the help desk: **+255 22 2410641/5** (Dar) / **+255 754782120** (mobile) / **+255 0747 626 619** (Dodoma)\n\n💡 In the meantime, you can download your **programme brochure** below for the full curriculum overview.`,
        downloads: all.filter(d => d.category === 'prospectus'),
        quickReplies: isSw
          ? [
              { label: 'Brochure ya DCIT', message: 'Naomba brochure ya DCIT' },
              { label: 'Brochure ya DBIT', message: 'Naomba brochure ya DBIT' },
              { label: 'Mratibu', message: 'Mratibu wa programu ni nani?' }
            ]
          : [
              { label: 'DCIT brochure', message: 'Send me the DCIT brochure' },
              { label: 'DBIT brochure', message: 'Send me the DBIT brochure' },
              { label: 'Coordinator', message: 'Who is the programme coordinator?' }
            ],
        confidence: 0.85
      };
    }
    if (calendarTriggers.some(t => lower.includes(t))) {
      return {
        answer: isSw
          ? `**Kalenda rasmi ya kitaaluma ya UCC** haijasambazwa kwenye tovuti kwa umma. Inachapishwa kwa kila mwaka wa masomo na husambazwa kwa wanafunzi waliosajiliwa kupitia portal.\n\n**Tarehe muhimu za 2026/2027:**\n• Maombi YANAFUNGULIWA: 1 Juni 2026\n• Maombi YANAFUNGWA: 30 Septemba 2026\n• Kuripoti chuoni: Novemba 2026 (jina la intake: Oktoba 2026/2027)\n\n**Ili kupata kalenda kamili:**\n• Email: **ucc@udsm.ac.tz**\n• Simu: **+255 22 2410641/5** (Dar) / **+255 754782120** (simu) / **+255 0747 626 619** (Dodoma)`
          : `The **official UCC academic calendar** is not currently published on the public website. It is released each academic year and distributed to registered students through the student portal.\n\n**Key dates for 2026/2027:**\n• Applications OPEN: 1 June 2026\n• Applications CLOSE: 30 September 2026\n• Reporting: November 2026 (intake label: October 2026/2027)\n\n**To get the full calendar:**\n• Email: **ucc@udsm.ac.tz**\n• Phone: **+255 22 2410641/5** (Dar) / **+255 754782120** (mobile) / **+255 0747 626 619** (Dodoma)`,
        quickReplies: isSw
          ? [
              { label: 'Pakua brochure', message: 'Naomba kupakua brochure za programu' }
            ]
          : [
              { label: 'Download brochures', message: 'Send me the programme brochures' }
            ],
        confidence: 0.85
      };
    }

    // 3) Programme-specific brochure (or 'all brochures')
    const brochureTriggers = ['brochure', 'prospectus', 'flyer', 'flier', 'muhtasari', 'brochure', 'taarifa zaidi kuhusu'];
    const wantsBrochure = brochureTriggers.some(t => lower.includes(t)) || lower.includes('download');
    const wantAll = /all|every|all programmes|all the|programs|courses/.test(lower) && wantsBrochure;
    if (wantsBrochure) {
      const wantedProgs = [];
      if (/\bdcit\b/.test(lower)) wantedProgs.push('DCIT');
      if (/\bdbit\b/.test(lower)) wantedProgs.push('DBIT');
      if (/\bccit\b/.test(lower)) wantedProgs.push('CCIT');
      if (/\bcbit\b/.test(lower)) wantedProgs.push('CBIT');

      let docs = [];
      if (wantAll) {
        docs = all.filter(d => d.category === 'prospectus');
      } else if (wantedProgs.length) {
        docs = all.filter(d => d.category === 'prospectus' && wantedProgs.includes(d.programme));
      } else {
        // No specific programme: return the most relevant single one or all
        docs = all.filter(d => d.category === 'prospectus' && d.programme === 'DCIT');
        if (!docs.length) docs = all.filter(d => d.category === 'prospectus').slice(0, 1);
      }

      if (docs.length) {
        const intro = docs.length === 1
          ? (isSw ? `Hapa kuna **brochure rasmi ya ${docs[0].programme}** — bofya kupakua kutoka ucc.co.tz.` : `Here is the **official ${docs[0].programme} brochure** — click to download from ucc.co.tz.`)
          : (isSw ? `Hapa kuna **${docs.length} brochure rasmi za UCC** — bofya kila kiungo kupakua kutoka ucc.co.tz.` : `Here are the **${docs.length} official UCC brochures** — click each link to download from ucc.co.tz.`);
        return {
          answer: intro,
          downloads: docs,
          quickReplies: isSw
            ? [
                { label: 'Ada ya DCIT', message: 'Ada ya DCIT ni ngapi?' },
                { label: 'Lini maombi?', message: 'Lini maombi yanafunguliwa na yanafungwa?' },
                { label: 'DCIT vs DBIT', message: 'DCIT na DBIT, ni ipi bora kwangu?' }
              ]
            : [
                { label: 'DCIT fees', message: 'How much is DCIT?' },
                { label: 'Admission dates', message: 'When do applications open and close?' },
                { label: 'DCIT vs DBIT', message: 'Which is better for me, DCIT or DBIT?' }
              ],
          confidence: 0.95
        };
      }
    }

    // 4) Generic "download" or "documents" with no specific target
    if (lower.includes('download') || lower.includes('pakua') || (lower.includes('document') && !lower.includes('documented'))) {
      const pack = D.getIntakePack && D.getIntakePack();
      const docs = pack ? pack.documents : all.filter(d => d.category === 'prospectus');
      return {
        answer: isSw
          ? `Hapa kuna **hati rasmi za UCC** — bofya kila kiungo kupakua kutoka ucc.co.tz. Ukihitaji hati nyingine, niambie tu.`
          : `Here are the **official UCC documents** — click each link to download from ucc.co.tz. If you need a specific document, just tell me.`,
        downloads: docs.slice(0, 6),
        quickReplies: isSw
          ? [
              { label: 'Brochure ya DCIT', message: 'Naomba brochure ya DCIT' },
              { label: 'Kozi za kitaalamu', message: 'Kozi za kitaalamu ni zipi?' }
            ]
          : [
              { label: 'DCIT brochure', message: 'Send me the DCIT brochure' },
              { label: 'Professional courses', message: 'What professional courses do you offer?' }
            ],
        confidence: 0.85
      };
    }

    return null;
  }

  async function tryITHelpResponse(message) {
    if (!message) return null;
    const lower = message.toLowerCase();
    const isSw = (state.detectedLang || 'en') === 'sw';

    // IT education / tech notes / study help
    const itTriggers = [
      'computer help', 'tech support', 'it help', 'ict help', 'technology question', 'programming help',
      'coding help', 'study help', 'tech note', 'tech notes', 'computer notes', 'ict notes',
      'computer science', 'information technology', 'software help', 'network help', 'database help',
      'how to code', 'learn programming', 'learn coding', 'learn networking',
      'msaada wa kompyuta', 'msaada wa teknolojia', 'msaada wa it', 'usaidizi wa kiufundi', 'mambo ya teknolojia',
      'coding', 'programming', 'python', 'java', 'javascript', 'html', 'css', 'networking', 'database',
      'computer notes', 'it notes', 'teach me', 'explain', 'what is', 'how does', 'tutorial'
    ];
    if (!itTriggers.some(t => lower.includes(t))) return null;

    // Be honest about scope
    return {
      answer: isSw
        ? `Asante kwa swali lako la kiufundi! Naweza kukupa **majibu ya haraka ya jumla** kuhusu mada nyingi za IT, lakini kwa **maelezo kamili ya kitaaluma** (kama vile kozi maalum, mitihani, au nyenzo za kusoma) ninapendekeza hivi:\n\n📚 **Nyenzo zinazopendekezwa:**\n• **UCC inatoa kozi za kitaalamu** — CCNA, CCNP, PMP, CISA, CISM, ITIL, COBIT, Ethical Hacking, Mobile App Dev. Pakua brochure hapa chini.\n• Kwa maswali ya jumla ya IT, jaribu kuuliza kwa maneno mahususi (mfano: "What is a database?", "How does TCP/IP work?") — nitajibu kwa ufupi.\n• Kwa msaada wa kiufundi wa UCC (akaunti ya email, mtandao, LMS), wasiliana na **ict@ucc.co.tz** au piga **+255 22 2410641/5**.\n\n⚠️ Kumbuka: Sio mtaalam wa IT — kwa maswali ya kina, thibitisha na chanzo rasmi au mwalimu.`
        : `Thanks for your IT question! I can give **quick general answers** on many IT topics, but for **full academic depth** (specific courses, exams, or study material) I recommend:\n\n📚 **Recommended resources:**\n• **UCC offers professional IT courses** — CCNA, CCNP, PMP, CISA, CISM, ITIL, COBIT, Ethical Hacking, Mobile App Dev. Download the brochures below.\n• For general IT questions, try asking in specific terms (e.g. "What is a database?", "How does TCP/IP work?") and I'll give a short answer.\n• For UCC IT support (email account, network, LMS), contact **ict@ucc.co.tz** or call **+255 22 2410641/5**.\n\n⚠️ I'm not a substitute for an IT instructor — for in-depth questions, verify with an authoritative source or your teacher.`,
      downloads: (window.UCCDocuments && window.UCCDocuments.getAllDocs && window.UCCDocuments.getAllDocs().filter(d => d.category === 'prospectus' && d.programme === null)) || [],
      quickReplies: isSw
        ? [
            { label: 'CCNA', message: 'Niambie kuhusu CCNA' },
            { label: 'PMP', message: 'Niambie kuhusu PMP' },
            { label: 'IT support', message: 'Ninahitaji msaada wa IT' }
          ]
        : [
            { label: 'CCNA', message: 'Tell me about CCNA' },
            { label: 'PMP', message: 'Tell me about PMP' },
            { label: 'IT support', message: 'I need IT support' }
          ],
      confidence: 0.6
    };
  }

  async function buildAssistantResponse(userText) {
    // 1) Document / download intent (pre-empts API)
    const docResp = await tryDocumentResponse(userText);
    if (docResp) return docResp;
    // 2) IT help intent
    const itResp = await tryITHelpResponse(userText);
    if (itResp) return itResp;
    return null;
  }

  // ---------- API ----------
  function apiBase() {
    return (typeof API_BASE_URL !== 'undefined' && API_BASE_URL) ? API_BASE_URL : '';
  }
  function hasLiveApi() { return apiBase().length > 0; }

  // ---------- DOM helpers ----------
  function $(id) { return document.getElementById(id); }
  function el(tag, props, ...children) {
    const e = document.createElement(tag);
    if (props) {
      for (const k in props) {
        if (k === 'class') e.className = props[k];
        else if (k === 'text') e.textContent = props[k];
        else if (k.startsWith('on') && typeof props[k] === 'function') e.addEventListener(k.slice(2).toLowerCase(), props[k]);
        else if (k === 'html') e.innerHTML = props[k];
        else e.setAttribute(k, props[k]);
      }
    }
    for (const c of children) if (c) e.appendChild(typeof c === 'string' ? document.createTextNode(c) : c);
    return e;
  }

  // ---------- Welcome ----------
  async function loadWelcomeIfNeeded() {
    if (state.welcomeLoaded) return;
    state.welcomeLoaded = true;

    let welcome = null;
    if (hasLiveApi()) {
      try {
        const controller = new AbortController();
        const t = setTimeout(() => controller.abort(), 5000);
        const r = await fetch(`${apiBase()}/chat/welcome?lang=${encodeURIComponent(state.detectedLang)}`, { method: 'GET', signal: controller.signal });
        clearTimeout(t);
        if (r.ok) welcome = await r.json();
      } catch (_) { welcome = null; }
    }

    if (!welcome && typeof uccFallbackAnswer === 'function') {
      const fb = uccFallbackAnswer('hello', state.detectedLang);
      if (fb && fb.answer) {
        welcome = {
          message: fb.answer,
          language: fb.language || state.detectedLang,
          quickReplies: state.detectedLang === 'sw'
            ? [
                { label: '📥 Pakua Hati', message: 'Naomba pakua hati zote za kuomba' },
                { label: 'Programu zenu', message: 'Naomba kuona programu zenu' },
                { label: 'Ada ya DCIT', message: 'Ada ya DCIT ni ngapi?' },
                { label: 'Lini maombi?', message: 'Lini maombi yanafunguliwa na yanafungwa?' },
                { label: 'DCIT vs DBIT', message: 'DCIT na DBIT, ni ipi bora kwangu?' }
              ]
            : [
                { label: '📥 Download Documents', message: 'I need the application pack' },
                { label: 'Programmes', message: 'What programmes do you offer?' },
                { label: 'DCIT fees', message: 'How much is DCIT?' },
                { label: 'Admission dates', message: 'When do applications open and close?' },
                { label: 'DCIT vs DBIT', message: 'Which is better for me, DCIT or DBIT?' }
              ],
          intakeOpen: '2026-06-01',
          intakeClose: '2026-09-30',
          intakeStart: '2026-11-01',
          applicationFee: 'TZS 15,000 (local) / TZS 30,000 (foreign)'
        };
      }
    }

    if (!welcome) {
      welcome = {
        message: state.detectedLang === 'sw'
          ? 'Habari! Karibu katika UCC. Naweza kukusaidia na programu, udahili, ada, na huduma nyingine za UCC.'
          : "Hello! Welcome to UCC. I can help you with programmes, admissions, fees, and other UCC services.",
        language: state.detectedLang,
        quickReplies: [],
        intakeOpen: '2026-06-01',
        intakeClose: '2026-09-30'
      };
    }

    if (welcome.language) saveLangPref(welcome.language);
    showIntakeBanner(welcome);
    addMessage('assistant', welcome.message, [], '', 1.0, false, { quickReplies: welcome.quickReplies || [] });
    state.history.push({ role: 'assistant', content: welcome.message, ts: Date.now() });
    saveHistory();
  }

  function showIntakeBanner(welcome) {
    if (!welcome) return;
    const banner = el('div', { class: 'intake-banner', role: 'status' });
    const fee = welcome.applicationFee || 'TZS 15,000 (local) / TZS 30,000 (foreign)';
    if (state.detectedLang === 'sw') {
      banner.innerHTML = `📅 <strong>Udaahili Oktoba 2026/2027:</strong> Maombi yanafunguliwa 1 Juni – 30 Septemba 2026. Kuripoti: Novemba 2026. Ada ya maombi: ${fee}. <a href="https://admission.ucc.co.tz/" target="_blank" rel="noopener noreferrer">Tuma maombi sasa →</a>`;
    } else {
      banner.innerHTML = `📅 <strong>October 2026/2027 Admissions:</strong> Applications open 1 June – 30 Sept 2026. Reporting: November 2026. Application fee: ${fee}. <a href="https://admission.ucc.co.tz/" target="_blank" rel="noopener noreferrer">Apply now →</a>`;
    }
    const messagesContainer = $('chat-messages');
    if (messagesContainer) messagesContainer.appendChild(banner);
  }

  // ---------- Widget open/close ----------
  function openChat() {
    const widget = $('chat-widget');
    if (widget) {
      widget.classList.remove('hidden');
      widget.setAttribute('aria-hidden', 'false');
    }
    const input = $('chat-input');
    if (input) {
      input.disabled = false;
      input.focus();
    }
    const sendBtn = $('send-btn');
    if (sendBtn) sendBtn.disabled = false;
    if (!state.welcomeLoaded) loadWelcomeIfNeeded();
  }

  function closeChat() {
    const widget = $('chat-widget');
    if (widget) {
      widget.classList.add('hidden');
      widget.setAttribute('aria-hidden', 'true');
    }
  }

  function openChatWith(message) {
    openChat();
    setTimeout(() => sendMessage(message), 250);
  }

  function sendQuickAction(message) { sendMessage(message); }

  // ---------- Render messages (ChatGPT-style rows) ----------
  const AVATAR_LABEL = { assistant: 'UCC', user: 'You' };

  function makeAvatar(role) {
    const a = el('div', { class: 'chat-avatar ' + (role === 'user' ? 'user-avatar' : 'bot-avatar'), 'aria-hidden': 'true' });
    if (role === 'user') {
      a.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>';
    } else {
      a.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/></svg>';
    }
    return a;
  }

  function renderMarkdownToHtml(text) {
    if (!text) return '';
    // Minimal markdown: escape first, then handle **bold**, *italic*, `code`, line breaks, links
    const esc = (s) => String(s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
    let html = esc(text);
    // links [text](url)
    html = html.replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');
    // bare URLs
    html = html.replace(/(?<!["'>])(https?:\/\/[^\s<]+)/g, '<a href="$1" target="_blank" rel="noopener noreferrer">$1</a>');
    // bold + italic
    html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/(^|[^*])\*([^*\n]+)\*/g, '$1<em>$2</em>');
    // inline code
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
    // line breaks (preserve paragraphs)
    const paragraphs = html.split(/\n{2,}/).map(p => '<p>' + p.replace(/\n/g, '<br>') + '</p>');
    return paragraphs.join('');
  }

  function addMessage(role, content, sources = [], intent = '', confidence = 0, escalated = false, opts = {}) {
    const messagesContainer = $('chat-messages');
    if (!messagesContainer) return;

    const welcomeScreen = messagesContainer.querySelector('.welcome-screen');
    if (welcomeScreen) welcomeScreen.remove();

    const row = el('div', { class: `chat-row ${role}`, role: 'article', 'aria-label': role === 'user' ? 'Your message' : 'Assistant response' });
    row.appendChild(makeAvatar(role));

    const contentCol = el('div', { class: 'chat-content' });
    const inner = el('div', { class: 'chat-content-inner' });
    contentCol.appendChild(inner);

    const label = el('div', { class: 'chat-role-label' });
    label.textContent = AVATAR_LABEL[role] || role;
    inner.appendChild(label);

    const text = el('div', { class: 'chat-text' });
    if (role === 'user') {
      text.textContent = content;
    } else {
      text.innerHTML = renderMarkdownToHtml(content);
    }
    inner.appendChild(text);

    if (sources && sources.length > 0) {
      const sourcesDiv = el('div', { class: 'message-sources' });
      const strong = el('strong', { text: 'Sources' });
      sourcesDiv.appendChild(strong);
      sources.forEach((source, i) => {
        const sourceP = el('p');
        if (source.url) {
          const a = el('a', { href: source.url, target: '_blank', rel: 'noopener noreferrer' });
          a.textContent = `${i + 1}. ${source.title || 'UCC Knowledge Base'}`;
          sourceP.appendChild(a);
        } else {
          sourceP.textContent = `${i + 1}. ${source.title || 'UCC Knowledge Base'}`;
        }
        sourcesDiv.appendChild(sourceP);
      });
      inner.appendChild(sourcesDiv);
    }

    if (escalated) {
      const esc2 = el('div', { class: 'escalation-note', role: 'note' });
      esc2.textContent = (state.detectedLang === 'sw')
        ? 'Ikiwa unahitaji msaada wa haraka, wasiliana nasi kwa info@ucc.co.tz au +255 22 2410641/5.'
        : 'For urgent help, please contact us at info@ucc.co.tz or +255 22 2410641/5.';
      inner.appendChild(esc2);
    }

    if (opts.quickReplies && opts.quickReplies.length) {
      const qr = el('div', { class: 'quick-replies', role: 'group', 'aria-label': 'Suggested questions' });
      opts.quickReplies.forEach((qr0) => {
        const b = el('button', { type: 'button', class: 'quick-reply-chip' });
        b.textContent = qr0.label;
        b.setAttribute('aria-label', `Ask: ${qr0.label}`);
        b.addEventListener('click', () => { if (qr0.message) sendMessage(qr0.message); });
        qr.appendChild(b);
      });
      inner.appendChild(qr);
    }

    if (opts.downloads && opts.downloads.length) {
      inner.appendChild(buildDownloadsCard(opts.downloads));
    }

    if (role === 'assistant' && !opts.skipFeedback) {
      const meta = el('div', { class: 'chat-meta' });
      meta.appendChild(buildFeedbackRow(content));
      const copyBtn = el('button', { type: 'button', 'aria-label': 'Copy response' });
      copyBtn.textContent = '📋 Copy';
      copyBtn.addEventListener('click', () => copyToClipboard(content, copyBtn));
      meta.appendChild(copyBtn);
      inner.appendChild(meta);
    }

    row.appendChild(contentCol);
    messagesContainer.appendChild(row);
    scrollChatToBottom();
  }

  function scrollChatToBottom() {
    const mc = $('chat-messages');
    if (!mc) return;
    window.requestAnimationFrame(() => {
      // Use document scroll because the full-page chat is just a document flow
      const rect = mc.getBoundingClientRect();
      const composer = document.querySelector('.chat-composer');
      const composerH = composer ? composer.offsetHeight : 0;
      const target = window.scrollY + rect.bottom - window.innerHeight + composerH + 80;
      window.scrollTo({ top: Math.max(0, target), behavior: 'smooth' });
    });
  }

  function copyToClipboard(text, btn) {
    if (!navigator.clipboard || !text) return;
    navigator.clipboard.writeText(text).then(() => {
      const prev = btn.textContent;
      btn.textContent = '✓ Copied';
      setTimeout(() => { btn.textContent = prev; }, 1500);
    }).catch(() => {});
  }

  // ---------- Streaming reveal (word-by-word) ----------
  // Creates a chat-row in 'streaming' mode and reveals `text` one word at a time.
  // Returns an object { finish(), abort() } so callers can short-circuit.
  function startStreamingReply() {
    const messagesContainer = $('chat-messages');
    if (!messagesContainer) return null;

    const welcomeScreen = messagesContainer.querySelector('.welcome-screen');
    if (welcomeScreen) welcomeScreen.remove();

    const row = el('div', { class: 'chat-row assistant streaming', role: 'article', 'aria-label': 'Assistant response' });
    row.appendChild(makeAvatar('assistant'));
    const contentCol = el('div', { class: 'chat-content' });
    const inner = el('div', { class: 'chat-content-inner' });
    const label = el('div', { class: 'chat-role-label', text: AVATAR_LABEL.assistant });
    inner.appendChild(label);
    const textDiv = el('div', { class: 'chat-text streaming' });
    inner.appendChild(textDiv);
    contentCol.appendChild(inner);
    row.appendChild(contentCol);
    messagesContainer.appendChild(row);

    let cancelled = false;
    let finished = false;
    let timer = null;
    let onDone = null;

    const reveal = (fullText) => {
      if (cancelled) return;
      textDiv.innerHTML = renderMarkdownToHtml(fullText);
      scrollChatToBottom();
    };

    const streamWords = (fullText) => {
      if (cancelled) return;
      const tokens = fullText.split(/(\s+)/);
      let idx = 0;
      let acc = '';
      const tick = () => {
        if (cancelled) { finished = true; if (onDone) onDone(acc, true); return; }
        // Reveal 2-3 tokens per frame for speed
        const step = 2;
        for (let i = 0; i < step && idx < tokens.length; i++) {
          acc += tokens[idx++];
        }
        reveal(acc);
        if (idx < tokens.length) {
          timer = setTimeout(tick, 18);
        } else {
          finished = true;
          textDiv.classList.remove('streaming');
          if (onDone) onDone(acc, false);
        }
      };
      tick();
    };

    return {
      element: row,
      textDiv: textDiv,
      // Show a fully-formatted preview immediately (no animation) — used for fast / static responses
      setText(t) { cancelled = true; if (timer) clearTimeout(timer); reveal(t); textDiv.classList.remove('streaming'); finished = true; if (onDone) onDone(t, false); },
      // Reveal word-by-word
      stream(t) { streamWords(t); },
      onDone(cb) { onDone = cb; if (finished && onDone) onDone(textDiv.textContent, cancelled); },
      abort() { cancelled = true; if (timer) clearTimeout(timer); textDiv.classList.remove('streaming'); }
    };
  }

  // ---------- Feedback ----------
  function buildFeedbackRow(answerText) {
    const row = el('div', { class: 'message-feedback', role: 'group', 'aria-label': 'Rate this response' });
    const upBtn = el('button', { type: 'button', class: 'fb-btn', 'aria-label': 'Helpful' });
    upBtn.innerHTML = '👍';
    const downBtn = el('button', { type: 'button', class: 'fb-btn', 'aria-label': 'Not helpful' });
    downBtn.innerHTML = '👎';
    const submitFeedback = (rating) => {
      upBtn.disabled = true; downBtn.disabled = true;
      upBtn.classList.add('active');
      queueFeedback({ sessionId: state.sessionId, rating, message: answerText, lang: state.detectedLang, ts: Date.now() });
    };
    upBtn.addEventListener('click', () => submitFeedback('up'));
    downBtn.addEventListener('click', () => submitFeedback('down'));
    row.appendChild(upBtn);
    row.appendChild(downBtn);
    return row;
  }

  // ---------- Downloads card ----------
  function buildDownloadsCard(downloads) {
    const wrap = el('div', { class: 'message-downloads', role: 'group', 'aria-label': 'Downloadable documents' });
    const heading = el('p', { class: 'downloads-heading' });
    heading.textContent = state.detectedLang === 'sw' ? '📎 Hati za kupakua:' : '📎 Documents to download:';
    wrap.appendChild(heading);
    const list = el('ul', { class: 'downloads-list' });
    downloads.forEach(d => {
      const li = el('li', { class: 'downloads-item' });
      const a = el('a', { href: d.downloadUrl, target: '_blank', rel: 'noopener noreferrer', class: 'downloads-link', 'data-id': d.id || '' });
      a.setAttribute('download', '');
      a.innerHTML = `<span class="downloads-icon" aria-hidden="true">⬇</span><span class="downloads-title">${escapeHtml(d.title)}</span><span class="downloads-meta">${escapeHtml(d.fileType || 'PDF')}</span>`;
      li.appendChild(a);
      list.appendChild(li);
    });
    wrap.appendChild(list);
    return wrap;
  }

  function escapeHtml(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function queueFeedback(item) {
    let pending = [];
    try {
      const raw = localStorage.getItem(STORAGE_KEYS.PENDING_FEEDBACK);
      if (raw) pending = JSON.parse(raw);
    } catch (_) {}
    pending.push(item);
    if (pending.length > 500) pending = pending.slice(-500);
    try { localStorage.setItem(STORAGE_KEYS.PENDING_FEEDBACK, JSON.stringify(pending)); } catch (_) {}
    flushPendingFeedback();
  }

  function flushPendingFeedback() {
    if (!hasLiveApi()) return;
    let pending = [];
    try {
      const raw = localStorage.getItem(STORAGE_KEYS.PENDING_FEEDBACK);
      if (raw) pending = JSON.parse(raw);
    } catch (_) { return; }
    if (!pending.length) return;
    const batch = pending.splice(0, FEEDBACK_BATCH_SIZE);
    Promise.allSettled(batch.map(item => fetch(`${apiBase()}/chat/feedback`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId: item.sessionId, rating: item.rating, comment: item.message ? item.message.substring(0, 500) : null })
    }).then(r => { if (!r.ok) throw new Error('HTTP ' + r.status); }))).then(results => {
      const failed = results.filter(r => r.status === 'rejected').length;
      if (failed > 0) {
        const requeue = batch.slice(-failed).concat(pending);
        try { localStorage.setItem(STORAGE_KEYS.PENDING_FEEDBACK, JSON.stringify(requeue)); } catch (_) {}
      } else {
        try { localStorage.setItem(STORAGE_KEYS.PENDING_FEEDBACK, JSON.stringify(pending)); } catch (_) {}
      }
    });
  }

  // ---------- Typing indicator (ChatGPT-style bouncing dots) ----------
  function showTypingIndicator() {
    const messagesContainer = $('chat-messages');
    if (!messagesContainer) return;
    const welcomeScreen = messagesContainer.querySelector('.welcome-screen');
    if (welcomeScreen) welcomeScreen.remove();
    const row = el('div', { id: 'typing-indicator', class: 'chat-typing', 'aria-label': 'Assistant is typing' });
    row.appendChild(makeAvatar('assistant'));
    const bubble = el('div', { class: 'chat-typing-bubble', role: 'status', 'aria-label': 'Typing' });
    bubble.innerHTML = '<span></span><span></span><span></span>';
    const wrapper = el('div', { class: 'chat-content' });
    const inner = el('div', { class: 'chat-content-inner' });
    inner.appendChild(bubble);
    wrapper.appendChild(inner);
    row.appendChild(wrapper);
    messagesContainer.appendChild(row);
    scrollChatToBottom();
  }
  function hideTypingIndicator() {
    const indicator = $('typing-indicator');
    if (indicator) indicator.remove();
  }

  // ---------- Send message (with streaming) ----------
  let activeRequest = null;
  let activeStream = null;
  async function sendMessage(message) {
    if (!message || !message.trim() || state.isProcessing) return;
    state.isProcessing = true;
    const input = $('chat-input');
    const sendBtn = $('send-btn');
    setSendButtonStop(true);

    const userText = message.trim();
    addMessage('user', userText);
    state.history.push({ role: 'user', content: userText, ts: Date.now() });
    saveHistory();

    if (input) { input.value = ''; autoResizeInput(input); }

    showTypingIndicator();

    try {
      saveLangPref(detectLanguage(userText));

      // 0) Try the local document / IT-help intent pre-check first
      const localResp = await buildAssistantResponse(userText);
      hideTypingIndicator();
      if (localResp) {
        addMessage(
          'assistant',
          localResp.answer,
          localResp.sources || [],
          '',
          localResp.confidence || 0.85,
          !!localResp.escalationRequired,
          { quickReplies: localResp.quickReplies, downloads: localResp.downloads }
        );
        state.history.push({ role: 'assistant', content: localResp.answer, ts: Date.now() });
        saveHistory();
        return;
      }

      // 1) Start the streaming reply shell up-front so the user sees the assistant typing
      hideTypingIndicator();
      const stream = startStreamingReply();
      activeStream = stream;

      let finalAnswer = null;
      let finalSources = [];
      let finalQuickReplies = null;
      let finalDownloads = null;
      let finalEscalation = false;
      let finalConfidence = 0.85;

      // 2) Fetch from API (or KB fallback) and stream the answer in
      const onResolve = (data) => {
        finalAnswer = data.answer || '';
        finalSources = data.sources || [];
        finalQuickReplies = data.quickReplies || null;
        finalDownloads = data.downloads || null;
        finalEscalation = !!data.escalationRequired;
        finalConfidence = data.confidence || 0.85;
        if (stream) {
          if (STREAMING_ENABLED && finalAnswer) {
            stream.stream(finalAnswer);
            stream.onDone((txt, wasAborted) => {
              attachExtrasToStreamRow(stream, finalSources, finalQuickReplies, finalDownloads, finalEscalation, finalAnswer, wasAborted);
            });
          } else {
            stream.setText(finalAnswer);
            attachExtrasToStreamRow(stream, finalSources, finalQuickReplies, finalDownloads, finalEscalation, finalAnswer, false);
          }
        }
      };

      if (hasLiveApi() && navigator.onLine) {
        try {
          activeRequest = new AbortController();
          const t = setTimeout(() => activeRequest && activeRequest.abort(), 20000);
          const token = (typeof localStorage !== 'undefined') ? localStorage.getItem('ucc_auth_token') : null;
          const userRaw = (typeof localStorage !== 'undefined') ? localStorage.getItem('ucc_auth_user') : null;
          const user = userRaw ? JSON.parse(userRaw) : null;
          const response = await fetch(`${apiBase()}/chat`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', ...(token ? { 'Authorization': 'Bearer ' + token } : {}) },
            body: JSON.stringify({ message: userText, conversationId: state.sessionId, language: state.detectedLang, user: user || null }),
            signal: activeRequest.signal
          });
          clearTimeout(t);
          if (response.ok) {
            const data = await response.json();
            const answerText = (data && data.answer) ? String(data.answer) : '';
            const isEscalation = /couldn't find verified information|please contact ucc|visit https:\/\/ucc\.co\.tz\//i.test(answerText);
            if (isEscalation && typeof uccFallbackAnswer === 'function') {
              try {
                const fb = uccFallbackAnswer(userText, state.detectedLang || 'en');
                if (fb && fb.answer) {
                  onResolve({ answer: fb.answer, language: fb.language || (state.detectedLang || 'en'), sources: fb.sources || [], confidence: fb.confidence || 0.75, escalationRequired: !!fb.escalationRequired });
                } else {
                  onResolve(data);
                }
              } catch (_) {
                onResolve(data);
              }
            } else {
              onResolve(data);
            }
          } else {
            throw new Error('HTTP ' + response.status);
          }
        } catch (networkErr) {
          if (stream) { stream.abort(); stream.element.remove(); }
          // Try local KB
          const lang = state.detectedLang || 'en';
          let fb = null;
          if (typeof uccFallbackAnswer === 'function') {
            try { fb = uccFallbackAnswer(userText, lang); } catch (_) {}
          }
          if (fb && typeof fb === 'object' && fb.answer) {
            onResolve({ answer: fb.answer, language: fb.language || lang, sources: fb.sources || [], confidence: fb.confidence || 0.7, escalationRequired: !!fb.escalationRequired });
          } else if (typeof fb === 'string') {
            onResolve({ answer: fb, language: lang, sources: [], confidence: 0.7, escalationRequired: false });
          } else {
            onResolve({
              answer: (lang === 'sw')
                ? 'Samahani, huduma ya chat haipatikani kwa sasa. Tafadhali jaribu tena baadaye au tembelea https://ucc.co.tz/ kwa taarifa zaidi.'
                : 'Sorry, the chat service is currently unavailable. Please try again shortly or visit https://ucc.co.tz/ for more information.',
              language: lang,
              sources: [],
              confidence: 0,
              escalationRequired: true
            });
          }
        } finally {
          activeRequest = null;
        }
      } else {
        if (stream) { stream.abort(); stream.element.remove(); }
        const lang = state.detectedLang || 'en';
        let fb = null;
        if (typeof uccFallbackAnswer === 'function') {
          try { fb = uccFallbackAnswer(userText, lang); } catch (_) {}
        }
        if (fb && typeof fb === 'object' && fb.answer) {
          addMessage('assistant', fb.answer, fb.sources || [], '', fb.confidence || 0.7, !!fb.escalationRequired, { quickReplies: fb.quickReplies, downloads: fb.downloads });
        } else if (typeof fb === 'string') {
          addMessage('assistant', fb, [], '', 0.7, false);
        } else {
          addMessage('assistant', (lang === 'sw')
            ? 'Samahani, huduma ya chat haipatikani kwa sasa. Tafadhali jaribu tena baadaye au tembelea https://ucc.co.tz/ kwa taarifa zaidi.'
            : 'Sorry, the chat service is currently unavailable. Please try again shortly or visit https://ucc.co.tz/ for more information.', [], '', 0, true);
        }
        state.history.push({ role: 'assistant', content: finalAnswer || (fb && (fb.answer || fb)) || '', ts: Date.now() });
        saveHistory();
      }

      // Push final assistant text into history
      if (finalAnswer) {
        state.history.push({ role: 'assistant', content: finalAnswer, ts: Date.now() });
        saveHistory();
      }
    } catch (error) {
      hideTypingIndicator();
      if (activeStream) { try { activeStream.abort(); activeStream.element.remove(); } catch (_) {} activeStream = null; }
      const offline = !navigator.onLine;
      const msg = offline
        ? (state.detectedLang === 'sw' ? 'Huna muunganisho wa intaneti. Tafadhali angalia muunganisho wako na ujaribu tena.' : 'You appear to be offline. Please check your connection and try again.')
        : (state.detectedLang === 'sw' ? 'Samahani, kuna hitilafu. Tafadhali jaribu tena.' : "I'm having trouble responding right now. Please try again.");
      addMessage('assistant', msg, [], 'error', 0, true, {
        quickReplies: [{ label: state.detectedLang === 'sw' ? 'Jaribu tena' : 'Try again', message: userText }]
      });
    } finally {
      state.isProcessing = false;
      activeStream = null;
      if (input) input.disabled = false;
      setSendButtonStop(false);
      if (input) input.focus();
    }
  }

  const STREAMING_ENABLED = true; // word-by-word reveal

  function setSendButtonStop(isStop) {
    const sendBtn = $('send-btn');
    if (!sendBtn) return;
    if (isStop) {
      sendBtn.classList.add('is-stop');
      sendBtn.setAttribute('aria-label', 'Stop generating');
      sendBtn.setAttribute('title', 'Stop');
      sendBtn.disabled = false;
      sendBtn.onclick = (e) => { e.preventDefault(); stopGenerating(); };
    } else {
      sendBtn.classList.remove('is-stop');
      sendBtn.removeAttribute('onclick');
      sendBtn.setAttribute('aria-label', 'Send message');
      sendBtn.setAttribute('title', 'Send (Enter)');
    }
  }

  function stopGenerating() {
    if (activeRequest) { try { activeRequest.abort(); } catch (_) {} }
    if (activeStream) { try { activeStream.abort(); } catch (_) {} }
  }

  // Append sources / quick-replies / downloads / feedback to the streamed row's
  // .chat-content-inner (instead of bubbleDiv, which no longer exists).
  function attachExtrasToStreamRow(stream, sources, quickReplies, downloads, escalated, rawAnswer, wasAborted) {
    if (!stream || !stream.element) return;
    const inner = stream.element.querySelector('.chat-content-inner');
    if (!inner) return;
    if (sources && sources.length) {
      const sourcesDiv = el('div', { class: 'message-sources' });
      const strong = el('strong', { text: 'Sources' });
      sourcesDiv.appendChild(strong);
      sources.forEach((source, i) => {
        const sourceP = el('p');
        if (source.url) {
          const a = el('a', { href: source.url, target: '_blank', rel: 'noopener noreferrer' });
          a.textContent = `${i + 1}. ${source.title || 'UCC Knowledge Base'}`;
          sourceP.appendChild(a);
        } else {
          sourceP.textContent = `${i + 1}. ${source.title || 'UCC Knowledge Base'}`;
        }
        sourcesDiv.appendChild(sourceP);
      });
      inner.appendChild(sourcesDiv);
    }
    if (escalated) {
      const esc2 = el('div', { class: 'escalation-note', role: 'note' });
      esc2.textContent = (state.detectedLang === 'sw')
        ? 'Ikiwa unahitaji msaada wa haraka, wasiliana nasi kwa info@ucc.co.tz au +255 22 2410641/5.'
        : 'For urgent help, please contact us at info@ucc.co.tz or +255 22 2410641/5.';
      inner.appendChild(esc2);
    }
    if (quickReplies && quickReplies.length) {
      const qr = el('div', { class: 'quick-replies', role: 'group', 'aria-label': 'Suggested questions' });
      quickReplies.forEach((qr0) => {
        const b = el('button', { type: 'button', class: 'quick-reply-chip' });
        b.textContent = qr0.label;
        b.setAttribute('aria-label', `Ask: ${qr0.label}`);
        b.addEventListener('click', () => { if (qr0.message) sendMessage(qr0.message); });
        qr.appendChild(b);
      });
      inner.appendChild(qr);
    }
    if (downloads && downloads.length) {
      inner.appendChild(buildDownloadsCard(downloads));
    }
    // feedback + copy
    const meta = el('div', { class: 'chat-meta' });
    meta.appendChild(buildFeedbackRow(rawAnswer || ''));
    const copyBtn = el('button', { type: 'button', 'aria-label': 'Copy response' });
    copyBtn.textContent = '📋 Copy';
    copyBtn.addEventListener('click', () => copyToClipboard(rawAnswer || '', copyBtn));
    meta.appendChild(copyBtn);
    inner.appendChild(meta);

    if (wasAborted) {
      const note = el('div', { class: 'escalation-note', role: 'note' });
      note.textContent = (state.detectedLang === 'sw') ? '⏹ Imesimamishwa.' : '⏹ Stopped.';
      inner.appendChild(note);
    }
  }

  // ---------- Restore history ----------
  function restoreHistory() {
    if (!state.history || !state.history.length) return;
    const intro = el('div', { class: 'history-restore', role: 'status' });
    intro.innerHTML = '<em>Welcome back. Here is your recent conversation. <button id="clear-history-btn" type="button" class="link-btn">Start new conversation</button></em>';
    $('chat-messages').appendChild(intro);
    state.history.forEach(m => addMessage(m.role, m.content, [], '', 0, false, { skipFeedback: true }));
    const clr = $('clear-history-btn');
    if (clr) clr.addEventListener('click', clearHistoryAndRestart);
  }

  function clearHistoryAndRestart() {
    clearAllStoredData();
    const messagesContainer = $('chat-messages');
    if (messagesContainer) messagesContainer.innerHTML = '';
    state.welcomeLoaded = false;
    loadWelcomeIfNeeded();
  }

  // ---------- Submit ----------
  function handleSubmit(event) {
    event.preventDefault();
    const input = $('chat-input');
    if (input) sendMessage(input.value);
  }

  // ---------- Init ----------
  function init() {
    // Accessibility: announce chat region to screen readers
    const chatMessages = $('chat-messages');
    if (chatMessages) {
      chatMessages.setAttribute('role', 'log');
      chatMessages.setAttribute('aria-live', 'polite');
      chatMessages.setAttribute('aria-relevant', 'additions text');
    }
    const input = $('chat-input');
    if (input) {
      input.setAttribute('aria-label', 'Message UCC Assistant');
      input.setAttribute('enterkeyhint', 'send');
      input.setAttribute('autocomplete', 'off');
      input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
          e.preventDefault();
          handleSubmit(e);
        }
      });
      input.addEventListener('input', () => autoResizeInput(input));
      autoResizeInput(input);
    }
    const sendBtn = $('send-btn');
    if (sendBtn) sendBtn.setAttribute('aria-label', 'Send message');

    // Wire up topbar buttons
    const newChatBtn = $('new-chat-btn');
    if (newChatBtn) newChatBtn.addEventListener('click', startNewConversation);
    const themeBtn = $('theme-toggle');
    if (themeBtn) themeBtn.addEventListener('click', toggleTheme);
    const authBtn = $('auth-btn');
    if (authBtn) {
      const hasToken = !!localStorage.getItem('ucc_auth_token');
      authBtn.setAttribute('aria-label', hasToken ? 'Student menu' : 'Student login');
      authBtn.addEventListener('click', () => {
        if (hasToken) {
          if (confirm('Logout from student account?')) {
            localStorage.removeItem('ucc_auth_token');
            localStorage.removeItem('ucc_auth_role');
            localStorage.removeItem('ucc_auth_user');
            location.reload();
          }
        } else {
          location.href = 'login.html';
        }
      });
    }
    const widgetAuthBtn = $('widget-auth-btn');
    if (widgetAuthBtn) {
      const hasToken = !!localStorage.getItem('ucc_auth_token');
      widgetAuthBtn.setAttribute('aria-label', hasToken ? 'Student menu' : 'Student login');
      widgetAuthBtn.addEventListener('click', () => {
        if (hasToken) {
          if (confirm('Logout from student account?')) {
            localStorage.removeItem('ucc_auth_token');
            localStorage.removeItem('ucc_auth_role');
            localStorage.removeItem('ucc_auth_user');
            location.reload();
          }
        } else {
          location.href = 'login.html';
        }
      });
    }

    // Wire up welcome prompt cards
    document.querySelectorAll('.prompt-card[data-prompt]').forEach(card => {
      card.addEventListener('click', () => {
        const p = card.getAttribute('data-prompt');
        if (p) sendMessage(p);
      });
    });

    // Initialise theme from storage or system pref
    initTheme();

    // Detect language from browser / html
    try {
      const navLang = (navigator.language || 'en').toLowerCase();
      if (navLang.startsWith('sw')) saveLangPref('sw');
    } catch (_) {}
    const htmlLang = (document.documentElement.getAttribute('lang') || '').toLowerCase();
    if (htmlLang.startsWith('sw')) saveLangPref('sw');

    // Restore history if any
    restoreHistory();

    // Load welcome if widget is visible
    const widget = $('chat-widget');
    if (widget && !widget.classList.contains('hidden')) loadWelcomeIfNeeded();

    // Online/offline indicators
    window.addEventListener('online', () => { setOnlineStatus(true); flushPendingFeedback(); });
    window.addEventListener('offline', () => setOnlineStatus(false));
    setOnlineStatus(navigator.onLine);
  }

  function autoResizeInput(input) {
    if (!input) return;
    input.style.height = 'auto';
    input.style.height = Math.min(input.scrollHeight, 200) + 'px';
  }

  function startNewConversation() {
    // Abort any in-flight request
    if (activeRequest) { try { activeRequest.abort(); } catch (_) {} }
    if (activeStream) { try { activeStream.abort(); } catch (_) {} }
    state.isProcessing = false;
    setSendButtonStop(false);

    // Clear UI
    const messagesContainer = $('chat-messages');
    if (messagesContainer) messagesContainer.innerHTML = '';
    // Re-add welcome screen
    rebuildWelcomeScreen();

    // Reset history
    try { localStorage.removeItem(STORAGE_KEYS.HISTORY); } catch (_) {}
    state.history = [];
    state.welcomeLoaded = false;
    loadWelcomeIfNeeded();

    // Focus input
    const input = $('chat-input');
    if (input) input.focus();
  }

  function rebuildWelcomeScreen() {
    const messagesContainer = $('chat-messages');
    if (!messagesContainer) return;
    const welcome = el('div', { class: 'welcome-screen welcome-screen--full' });
    welcome.innerHTML = `
      <div class="welcome-orb" aria-hidden="true">
        <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
          <path d="M12 2a4 4 0 014 4v1.5a4 4 0 11-8 0V6a4 4 0 014-4zM5 11a7 7 0 1014 0M12 18v4M8 22h8"/>
        </svg>
      </div>
      <h2 class="welcome-title">How can I help you today?</h2>
      <p class="welcome-sub">I'm the UCC Customer Care Assistant. Ask me about programmes, admissions, fees, registration, ICT support, or download official documents.</p>
      <div class="suggested-prompts" role="group" aria-label="Suggested questions">
        <button class="prompt-card" type="button" data-prompt="I need the application pack for 2026/2027">
          <span class="prompt-icon" aria-hidden="true">📥</span>
          <span class="prompt-text">Get the 2026/2027 application pack</span>
        </button>
        <button class="prompt-card" type="button" data-prompt="How do I apply to UCC?">
          <span class="prompt-icon" aria-hidden="true">🎓</span>
          <span class="prompt-text">How do I apply to UCC?</span>
        </button>
        <button class="prompt-card" type="button" data-prompt="What programmes does UCC offer?">
          <span class="prompt-icon" aria-hidden="true">📚</span>
          <span class="prompt-text">What programmes does UCC offer?</span>
        </button>
        <button class="prompt-card" type="button" data-prompt="What are the tuition fees?">
          <span class="prompt-icon" aria-hidden="true">💰</span>
          <span class="prompt-text">What are the tuition fees?</span>
        </button>
        <button class="prompt-card" type="button" data-prompt="Compare DCIT and DBIT">
          <span class="prompt-icon" aria-hidden="true">⚖️</span>
          <span class="prompt-text">Compare DCIT and DBIT</span>
        </button>
        <button class="prompt-card" type="button" data-prompt="How can I contact UCC?">
          <span class="prompt-icon" aria-hidden="true">📞</span>
          <span class="prompt-text">How can I contact UCC?</span>
        </button>
      </div>`;
    messagesContainer.appendChild(welcome);
    // Re-wire prompt cards
    welcome.querySelectorAll('.prompt-card[data-prompt]').forEach(card => {
      card.addEventListener('click', () => {
        const p = card.getAttribute('data-prompt');
        if (p) sendMessage(p);
      });
    });
  }

  // ---------- Theme (light / dark) ----------
  const THEME_KEY = 'ucc_chat_theme';
  function initTheme() {
    let stored = null;
    try { stored = localStorage.getItem(THEME_KEY); } catch (_) {}
    if (stored === 'light' || stored === 'dark') {
      document.documentElement.setAttribute('data-theme', stored);
    } else {
      // No explicit choice — respect prefers-color-scheme via CSS @media
      const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
      if (prefersDark) document.documentElement.setAttribute('data-theme', 'dark');
    }
  }
  function toggleTheme() {
    const current = document.documentElement.getAttribute('data-theme')
      || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
    const next = current === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', next);
    try { localStorage.setItem(THEME_KEY, next); } catch (_) {}
  }

  function setOnlineStatus(online) {
    const indicators = document.querySelectorAll('.status-indicator');
    const texts = document.querySelectorAll('.status-text');
    indicators.forEach(i => i.classList.toggle('offline', !online));
    texts.forEach(t => t.textContent = online
      ? (state.detectedLang === 'sw' ? 'Mtandaoni' : 'Online')
      : (state.detectedLang === 'sw' ? 'Nje ya mtandao' : 'Offline'));
  }

  // ---------- Expose ----------
  window.UCCChatbot = {
    open: openChat,
    close: closeChat,
    openWith: openChatWith,
    send: sendMessage,
    sendQuickAction,
    handleSubmit,
    clearHistory: clearHistoryAndRestart,
    newChat: startNewConversation,
    toggleTheme,
    setLanguage: (lang) => { if (lang === 'sw' || lang === 'en') saveLangPref(lang); }
  };
  // Back-compat for existing on-page onclick handlers
  window.openChat = openChat;
  window.closeChat = closeChat;
  window.openChatWith = openChatWith;
  window.sendQuickAction = sendQuickAction;
  window.handleSubmit = handleSubmit;
  window.startNewConversation = startNewConversation;

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
