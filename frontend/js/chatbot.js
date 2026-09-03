let sessionId = crypto.randomUUID();
let conversationHistory = [];
let isProcessing = false;
let welcomeLoaded = false;
let detectedLang = 'en';

function formatBotText(text) {
  if (!text) return '';
  const escaped = String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
  return escaped.replace(/\n/g, '<br>');
}

async function loadWelcomeIfNeeded() {
  if (welcomeLoaded) return;
  welcomeLoaded = true;

  const apiBase = (typeof API_BASE_URL !== 'undefined') ? API_BASE_URL : '';
  const hasLiveApi = apiBase && apiBase.length > 0;

  let welcome = null;
  if (hasLiveApi) {
    try {
      const r = await fetch(`${apiBase}/chat/welcome?lang=${encodeURIComponent(detectedLang)}`, { method: 'GET' });
      if (r.ok) welcome = await r.json();
    } catch (_) { welcome = null; }
  }

  if (!welcome && typeof uccFallbackAnswer === 'function') {
    try {
      const fb = uccFallbackAnswer('hello', detectedLang);
      if (fb) {
        welcome = {
          message: fb.answer,
          language: fb.language || detectedLang,
          quickReplies: detectedLang === 'sw'
            ? [
                { label: 'Programu zenu', message: 'Naomba kuona programu zenu' },
                { label: 'Ada ya DCIT', message: 'Ada ya DCIT ni ngapi?' },
                { label: 'Lini maombi yanafunguliwa?', message: 'Lini maombi yanafunguliwa na yanafungwa?' },
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
    } catch (_) { welcome = null; }
  }

  if (!welcome) {
    welcome = {
      message: detectedLang === 'sw'
        ? 'Habari! Karibu katika UCC. Naweza kukusaidia na programu, udahili, ada, na huduma nyingine za UCC.'
        : "Hello! Welcome to UCC. I can help you with programmes, admissions, fees, and other UCC services.",
      language: detectedLang,
      quickReplies: [],
      intakeOpen: '2026-06-01',
      intakeClose: '2026-09-30'
    };
  }

  if (welcome.language) detectedLang = welcome.language;
  showIntakeBanner(welcome);
  addMessage('assistant', welcome.message, [], '', 1.0, false, { quickReplies: welcome.quickReplies || [] });
}

function showIntakeBanner(welcome) {
  if (!welcome) return;
  const banner = document.createElement('div');
  banner.className = 'intake-banner';
  if (detectedLang === 'sw') {
    banner.innerHTML = `📅 <strong>Udaahili 2026/2027:</strong> Maombi yanafunguliwa 1 Juni – 30 Septemba 2026. Intake: Septemba 2026. Ada ya maombi: ${welcome.applicationFee || 'TZS 10,000'}. <a href="https://admission.ucc.co.tz/" target="_blank" rel="noopener">Tuma maombi sasa →</a>`;
  } else {
    banner.innerHTML = `📅 <strong>2026/2027 Admissions:</strong> Applications open 1 June – 30 Sept 2026. Intake: September 2026. Application fee: ${welcome.applicationFee || 'TZS 10,000'}. <a href="https://admission.ucc.co.tz/" target="_blank" rel="noopener">Apply now →</a>`;
  }
  const messagesContainer = document.getElementById('chat-messages');
  if (messagesContainer) messagesContainer.appendChild(banner);
}

function openChat() {
  document.getElementById('chat-widget').classList.remove('hidden');
  document.getElementById('chat-input').disabled = false;
  document.getElementById('send-btn').disabled = false;
  if (!welcomeLoaded) loadWelcomeIfNeeded();
}

function closeChat() {
  document.getElementById('chat-widget').classList.add('hidden');
}

function openChatWith(message) {
  openChat();
  setTimeout(() => sendMessage(message), 300);
}

function sendQuickAction(message) {
  sendMessage(message);
}

function detectLanguage(message) {
  const lower = message.toLowerCase();
  const swahiliIndicators = [
    'habari', 'hujambo', 'kwaheri', 'asante', 'karibu', 'tafadhali', 'ninataka',
    'naomba', 'nimekuwa', 'ninaweza', 'kwanini', 'vipi', 'wapi', 'lini',
    'ndiyo', 'hapana', 'labda', 'pia', 'bado', 'sasa', 'kesho', 'jana',
    'programu', 'kozi', 'ada', 'masomo', 'chuo', 'udahili', 'usajili', 'mitihani',
    'diploma', 'cheti', 'msaada', 'mwalimu', 'mwanafunzi', 'walimu', 'wanafunzi',
    'kuhusu', 'kwa nini', 'kwa hiyo', 'kwa sababu', 'pamoja', 'bila', 'baada ya',
    'kabla ya', 'miongoni', 'mara', 'mara kwa mara', 'hata hivyo', 'ingawa',
    'hata', 'tena', 'tayari', 'bado', 'kisha', 'halafu', 'sasa hivi',
    'mkuu', 'wa dar es salaam', 'uchumi', 'biashara', 'kilimo', 'afya',
    'swahili only', 'lugha', 'maneno', 'sentensi', 'kifungu', 'kitabu'
  ];

  const englishIndicators = [
    'the', 'and', 'with', 'what', 'how', 'when', 'where', 'why', 'which',
    'can', 'could', 'would', 'should', 'will', 'shall', 'may', 'might',
    'about', 'from', 'into', 'onto', 'upon', 'over', 'under', 'between',
    'tuition', 'fees', 'program', 'course', 'diploma', 'certificate',
    'admission', 'application', 'enrollment', 'registration', 'requirements',
    'scholarship', 'curriculum', 'semester', 'academic', 'university',
    'apply', 'contact', 'email', 'phone', 'address', 'location', 'office',
    'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday',
    'january', 'february', 'march', 'april', 'may', 'june', 'july',
    'august', 'september', 'october', 'november', 'december'
  ];

  let swScore = 0;
  let enScore = 0;
  for (const w of swahiliIndicators) {
    if (lower.includes(w)) swScore += 2;
  }
  for (const w of englishIndicators) {
    if (lower.includes(w)) enScore += 1;
  }

  return swScore > enScore ? 'sw' : 'en';
}

function addMessage(role, content, sources = [], intent = '', confidence = 0, escalated = false, opts = {}) {
  const messagesContainer = document.getElementById('chat-messages');

  const welcomeScreen = messagesContainer.querySelector('.welcome-screen');
  if (welcomeScreen) {
    welcomeScreen.remove();
  }

  const messageDiv = document.createElement('div');
  messageDiv.className = `message ${role}`;

  const bubbleDiv = document.createElement('div');
  bubbleDiv.className = 'message-bubble';

  const contentDiv = document.createElement('div');
  contentDiv.className = 'message-text';
  if (role === 'assistant') {
    contentDiv.innerHTML = formatBotText(content);
  } else {
    contentDiv.textContent = content;
  }
  bubbleDiv.appendChild(contentDiv);

  if (sources && sources.length > 0) {
    const sourcesDiv = document.createElement('div');
    sourcesDiv.className = 'message-sources';
    sourcesDiv.innerHTML = '<strong>Sources:</strong>';
    sources.forEach((source, i) => {
      const sourceP = document.createElement('p');
      sourceP.textContent = `${i + 1}. ${source.title || 'UCC Knowledge Base'} (${source.category || 'General'})`;
      sourcesDiv.appendChild(sourceP);
    });
    bubbleDiv.appendChild(sourcesDiv);
  }

  if (escalated) {
    const esc = document.createElement('p');
    esc.className = 'escalation-note';
    esc.textContent = (detectedLang === 'sw')
      ? 'Ikiwa unahitaji msaada wa haraka, wasiliana nasi kwa info@ucc.co.tz au +255 22 2410641/5.'
      : 'For urgent help, please contact us at info@ucc.co.tz or +255 22 2410641/5.';
    bubbleDiv.appendChild(esc);
  }

  if (opts.quickReplies && opts.quickReplies.length) {
    const qr = document.createElement('div');
    qr.className = 'quick-replies';
    opts.quickReplies.forEach((qr0) => {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'quick-reply-chip';
      b.textContent = qr0.label;
      b.addEventListener('click', () => {
        if (qr0.message) sendMessage(qr0.message);
      });
      qr.appendChild(b);
    });
    bubbleDiv.appendChild(qr);
  }

  const timeP = document.createElement('p');
  timeP.className = 'message-time';
  timeP.textContent = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  bubbleDiv.appendChild(timeP);

  messageDiv.appendChild(bubbleDiv);
  messagesContainer.appendChild(messageDiv);
  messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

function showTypingIndicator() {
  const messagesContainer = document.getElementById('chat-messages');
  const typingDiv = document.createElement('div');
  typingDiv.id = 'typing-indicator';
  typingDiv.className = 'message assistant';
  typingDiv.innerHTML = `
    <div class="message-bubble">
      <div class="typing-indicator">
        <span></span>
        <span></span>
        <span></span>
      </div>
    </div>
  `;
  messagesContainer.appendChild(typingDiv);
  messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

function hideTypingIndicator() {
  const indicator = document.getElementById('typing-indicator');
  if (indicator) indicator.remove();
}

async function sendMessage(message) {
  if (!message.trim() || isProcessing) return;

  isProcessing = true;
  const input = document.getElementById('chat-input');
  const sendBtn = document.getElementById('send-btn');

  addMessage('user', message.trim());
  conversationHistory.push({ role: 'user', content: message.trim() });

  input.value = '';
  input.disabled = true;
  sendBtn.disabled = true;
  showTypingIndicator();

  try {
    detectedLang = detectLanguage(message);

    let data;
    const apiBase = (typeof API_BASE_URL !== 'undefined') ? API_BASE_URL : '';
    const hasLiveApi = apiBase && apiBase.length > 0;

    if (hasLiveApi) {
      try {
        const response = await fetch(`${apiBase}/chat`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            message: message.trim(),
            conversationId: sessionId,
            language: detectedLang
          }),
        });
        if (response.ok) {
          data = await response.json();
        } else {
          throw new Error(`HTTP ${response.status}`);
        }
      } catch (networkErr) {
        data = null;
      }
    }

    if (!data) {
      const lang = detectedLang || 'en';
      let fallbackResult = null;
      if (typeof uccFallbackAnswer === 'function') {
        try {
          fallbackResult = uccFallbackAnswer(message, lang);
          if (fallbackResult && typeof fallbackResult === 'object' && fallbackResult.answer) {
            data = {
              answer: fallbackResult.answer,
              language: fallbackResult.language || lang,
              sources: fallbackResult.sources || [],
              confidence: fallbackResult.confidence || 0.7,
              escalationRequired: !!fallbackResult.escalationRequired
            };
            fallbackResult = null;
          } else if (typeof fallbackResult === 'string') {
            data = { answer: fallbackResult, language: lang, sources: [], confidence: 0.7, escalationRequired: false };
            fallbackResult = null;
          }
        } catch (e) {
          fallbackResult = null;
        }
      }
      if (!data) {
        const fallbackText = (detectedLang === 'sw')
          ? 'Samahani, huduma ya chat haipatikani kwa sasa. Tafadhali jaribu tena baadaye au tembelea https://ucc.co.tz/ kwa taarifa zaidi.'
          : 'Sorry, the chat service is currently unavailable. Please try again shortly or visit https://ucc.co.tz/ for more information.';
        data = { answer: fallbackText, language: detectedLang, sources: [], confidence: 0, escalationRequired: true };
      }
    }

    hideTypingIndicator();

    addMessage(
      'assistant',
      data.answer || data.response || 'I couldn\'t generate a response. Please try again.',
      data.sources || [],
      '',
      data.confidence || 0,
      data.escalationRequired || false
    );

    conversationHistory.push({ role: 'assistant', content: data.answer || data.response || '' });
  } catch (error) {
    hideTypingIndicator();
    addMessage(
      'assistant',
      'I\'m temporarily unable to process your request. Please try again shortly or contact UCC directly at https://www.ucc.co.tz/.',
      [],
      'error',
      0,
      true
    );
  } finally {
    isProcessing = false;
    input.disabled = false;
    sendBtn.disabled = false;
    input.focus();
  }
}

function handleSubmit(event) {
  event.preventDefault();
  const input = document.getElementById('chat-input');
  sendMessage(input.value);
}

document.addEventListener('DOMContentLoaded', () => {
  const chatInput = document.getElementById('chat-input');
  if (chatInput) {
    chatInput.addEventListener('keypress', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        handleSubmit(e);
      }
    });
  }

  try {
    const navLang = (navigator.language || 'en').toLowerCase();
    if (navLang.startsWith('sw')) detectedLang = 'sw';
  } catch (_) { detectedLang = 'en'; }

  const htmlLang = (document.documentElement.getAttribute('lang') || '').toLowerCase();
  if (htmlLang.startsWith('sw')) detectedLang = 'sw';

  const widgetVisible = !document.getElementById('chat-widget').classList.contains('hidden');
  if (widgetVisible) loadWelcomeIfNeeded();
});
