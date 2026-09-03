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
      'maombi', 'ada ya', 'kiasi gani'
    ];
    let swScore = 0, enScore = 0;
    for (const w of swStems) {
      if (lower.includes(w)) swScore += 2;
    }
    if (/\b(the|what|how|when|where|why|which|can|could|would|should|will|may|tuition|fees|program|course|admission|application|apply|contact|email|address|university)\b/.test(lower)) enScore += 1;
    return swScore > enScore ? 'sw' : 'en';
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

  function formatBotText(text) {
    if (!text) return '';
    return String(text)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\n/g, '<br>');
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
                { label: 'Programu zenu', message: 'Naomba kuona programu zenu' },
                { label: 'Ada ya DCIT', message: 'Ada ya DCIT ni ngapi?' },
                { label: 'Lini maombi?', message: 'Lini maombi yanafunguliwa na yanafungwa?' },
                { label: 'DCIT vs DBIT', message: 'DCIT na DBIT, ni ipi bora kwangu?' }
              ]
            : [
                { label: 'Programmes', message: 'What programmes do you offer?' },
                { label: 'DCIT fees', message: 'How much is DCIT?' },
                { label: 'Admission dates', message: 'When do applications open and close?' },
                { label: 'DCIT vs DBIT', message: 'Which is better for me, DCIT or DBIT?' }
              ],
          intakeOpen: '2026-06-01',
          intakeClose: '2026-09-30',
          intakeStart: '2026-09-01',
          applicationFee: 'TZS 10,000'
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
    const fee = welcome.applicationFee || 'TZS 10,000';
    if (state.detectedLang === 'sw') {
      banner.innerHTML = `📅 <strong>Udaahili 2026/2027:</strong> Maombi yanafunguliwa 1 Juni – 30 Septemba 2026. Intake: Septemba 2026. Ada ya maombi: ${fee}. <a href="https://admission.ucc.co.tz/" target="_blank" rel="noopener noreferrer">Tuma maombi sasa →</a>`;
    } else {
      banner.innerHTML = `📅 <strong>2026/2027 Admissions:</strong> Applications open 1 June – 30 Sept 2026. Intake: September 2026. Application fee: ${fee}. <a href="https://admission.ucc.co.tz/" target="_blank" rel="noopener noreferrer">Apply now →</a>`;
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

  // ---------- Render messages ----------
  function addMessage(role, content, sources = [], intent = '', confidence = 0, escalated = false, opts = {}) {
    const messagesContainer = $('chat-messages');
    if (!messagesContainer) return;

    const welcomeScreen = messagesContainer.querySelector('.welcome-screen');
    if (welcomeScreen) welcomeScreen.remove();

    const messageDiv = el('div', { class: `message ${role}`, role: 'article', 'aria-label': role === 'user' ? 'Your message' : 'Assistant response' });
    const bubbleDiv = el('div', { class: 'message-bubble' });
    const contentDiv = el('div', { class: 'message-text' });
    if (role === 'assistant') contentDiv.innerHTML = formatBotText(content);
    else contentDiv.textContent = content;
    bubbleDiv.appendChild(contentDiv);

    if (sources && sources.length > 0) {
      const sourcesDiv = el('div', { class: 'message-sources' });
      const strong = el('strong', { text: 'Sources:' });
      sourcesDiv.appendChild(strong);
      sources.forEach((source, i) => {
        const sourceP = el('p', { text: `${i + 1}. ${source.title || 'UCC Knowledge Base'}${source.url ? ' — ' : ''}` });
        if (source.url) {
          const a = el('a', { href: source.url, target: '_blank', rel: 'noopener noreferrer', text: source.url });
          sourceP.appendChild(a);
        }
        sourcesDiv.appendChild(sourceP);
      });
      bubbleDiv.appendChild(sourcesDiv);
    }

    if (escalated) {
      const esc = el('p', { class: 'escalation-note', role: 'note' });
      esc.textContent = (state.detectedLang === 'sw')
        ? 'Ikiwa unahitaji msaada wa haraka, wasiliana nasi kwa info@ucc.co.tz au +255 22 2410641/5.'
        : 'For urgent help, please contact us at info@ucc.co.tz or +255 22 2410641/5.';
      bubbleDiv.appendChild(esc);
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
      bubbleDiv.appendChild(qr);
    }

    if (role === 'assistant' && !opts.skipFeedback) {
      bubbleDiv.appendChild(buildFeedbackRow(content));
    }

    const timeP = el('p', { class: 'message-time', 'aria-label': 'Sent at ' + new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) });
    timeP.textContent = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    bubbleDiv.appendChild(timeP);

    messageDiv.appendChild(bubbleDiv);
    messagesContainer.appendChild(messageDiv);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
  }

  // ---------- Feedback ----------
  function buildFeedbackRow(answerText) {
    const row = el('div', { class: 'message-feedback', role: 'group', 'aria-label': 'Rate this response' });
    const upBtn = el('button', { type: 'button', class: 'feedback-btn', 'aria-label': 'Helpful' });
    upBtn.innerHTML = '👍';
    const downBtn = el('button', { type: 'button', class: 'feedback-btn', 'aria-label': 'Not helpful' });
    downBtn.innerHTML = '👎';
    const status = el('span', { class: 'feedback-status', role: 'status' });
    const submitFeedback = (rating) => {
      upBtn.disabled = true; downBtn.disabled = true;
      status.textContent = state.detectedLang === 'sw' ? 'Asante kwa maoni yako!' : 'Thanks for your feedback!';
      queueFeedback({ sessionId: state.sessionId, rating, message: answerText, lang: state.detectedLang, ts: Date.now() });
    };
    upBtn.addEventListener('click', () => submitFeedback('up'));
    downBtn.addEventListener('click', () => submitFeedback('down'));
    row.appendChild(upBtn);
    row.appendChild(downBtn);
    row.appendChild(status);
    return row;
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

  // ---------- Typing indicator ----------
  function showTypingIndicator() {
    const messagesContainer = $('chat-messages');
    if (!messagesContainer) return;
    const typingDiv = el('div', { id: 'typing-indicator', class: 'message assistant', 'aria-label': 'Assistant is typing' });
    typingDiv.innerHTML = '<div class="message-bubble"><div class="typing-indicator" role="status" aria-label="Typing"><span></span><span></span><span></span></div></div>';
    messagesContainer.appendChild(typingDiv);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
  }
  function hideTypingIndicator() {
    const indicator = $('typing-indicator');
    if (indicator) indicator.remove();
  }

  // ---------- Send message ----------
  let activeRequest = null;
  async function sendMessage(message) {
    if (!message || !message.trim() || state.isProcessing) return;
    state.isProcessing = true;
    const input = $('chat-input');
    const sendBtn = $('send-btn');

    const userText = message.trim();
    addMessage('user', userText);
    state.history.push({ role: 'user', content: userText, ts: Date.now() });
    saveHistory();

    if (input) input.value = '';
    if (input) input.disabled = true;
    if (sendBtn) sendBtn.disabled = true;
    showTypingIndicator();

    try {
      saveLangPref(detectLanguage(userText));

      let data = null;
      if (hasLiveApi() && navigator.onLine) {
        try {
          activeRequest = new AbortController();
          const t = setTimeout(() => activeRequest && activeRequest.abort(), 15000);
          const response = await fetch(`${apiBase()}/chat`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: userText, conversationId: state.sessionId, language: state.detectedLang }),
            signal: activeRequest.signal
          });
          clearTimeout(t);
          if (response.ok) data = await response.json();
        } catch (networkErr) {
          data = null;
        } finally {
          activeRequest = null;
        }
      }

      if (!data) {
        const lang = state.detectedLang || 'en';
        if (typeof uccFallbackAnswer === 'function') {
          try {
            const fb = uccFallbackAnswer(userText, lang);
            if (fb && typeof fb === 'object' && fb.answer) {
              data = {
                answer: fb.answer,
                language: fb.language || lang,
                sources: fb.sources || [],
                confidence: fb.confidence || 0.7,
                escalationRequired: !!fb.escalationRequired
              };
            } else if (typeof fb === 'string') {
              data = { answer: fb, language: lang, sources: [], confidence: 0.7, escalationRequired: false };
            }
          } catch (_) {}
        }
        if (!data) {
          data = {
            answer: (lang === 'sw')
              ? 'Samahani, huduma ya chat haipatikani kwa sasa. Tafadhali jaribu tena baadaye au tembelea https://ucc.co.tz/ kwa taarifa zaidi.'
              : 'Sorry, the chat service is currently unavailable. Please try again shortly or visit https://ucc.co.tz/ for more information.',
            language: lang,
            sources: [],
            confidence: 0,
            escalationRequired: true
          };
        }
      }

      hideTypingIndicator();
      addMessage('assistant', data.answer || 'I couldn\'t generate a response. Please try again.', data.sources || [], '', data.confidence || 0, data.escalationRequired || false);
      state.history.push({ role: 'assistant', content: data.answer, ts: Date.now() });
      saveHistory();
    } catch (error) {
      hideTypingIndicator();
      const offline = !navigator.onLine;
      const msg = offline
        ? (state.detectedLang === 'sw' ? 'Huna muunganisho wa intaneti. Tafadhali angalia muunganisho wako na ujaribu tena.' : 'You appear to be offline. Please check your connection and try again.')
        : (state.detectedLang === 'sw' ? 'Samahani, kuna hitilafu. Tafadhali jaribu tena.' : "I'm having trouble responding right now. Please try again.");
      addMessage('assistant', msg, [], 'error', 0, true, {
        quickReplies: [{ label: state.detectedLang === 'sw' ? 'Jaribu tena' : 'Try again', message: userText }]
      });
    } finally {
      state.isProcessing = false;
      if (input) input.disabled = false;
      if (sendBtn) sendBtn.disabled = false;
      if (input) input.focus();
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
      input.setAttribute('aria-label', 'Type your question to UCC Assistant');
      input.setAttribute('enterkeyhint', 'send');
      input.setAttribute('autocomplete', 'off');
      input.setAttribute('inputmode', 'text');
      input.addEventListener('keypress', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
          e.preventDefault();
          handleSubmit(e);
        }
      });
    }
    const sendBtn = $('send-btn');
    if (sendBtn) sendBtn.setAttribute('aria-label', 'Send message');

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
    setLanguage: (lang) => { if (lang === 'sw' || lang === 'en') saveLangPref(lang); }
  };
  // Back-compat for existing on-page onclick handlers
  window.openChat = openChat;
  window.closeChat = closeChat;
  window.openChatWith = openChatWith;
  window.sendQuickAction = sendQuickAction;
  window.handleSubmit = handleSubmit;

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
