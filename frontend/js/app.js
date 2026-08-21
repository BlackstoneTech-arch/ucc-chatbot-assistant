const API_BASE = 'http://localhost:5000/api';
let sessionId = crypto.randomUUID();
let conversationHistory = [];
let isProcessing = false;

function openChat() {
  document.getElementById('chat-widget').classList.remove('hidden');
  document.getElementById('chat-input').disabled = false;
  document.getElementById('send-btn').disabled = false;
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

function addMessage(role, content, sources = [], intent = '', confidence = 0, escalated = false) {
  const messagesContainer = document.getElementById('chat-messages');

  // Clear welcome screen if present
  const welcomeScreen = messagesContainer.querySelector('.welcome-screen');
  if (welcomeScreen) {
    welcomeScreen.remove();
  }

  const messageDiv = document.createElement('div');
  messageDiv.className = `message ${role}`;

  const bubbleDiv = document.createElement('div');
  bubbleDiv.className = 'message-bubble';

  const contentP = document.createElement('p');
  contentP.textContent = content;
  bubbleDiv.appendChild(contentP);

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
    const response = await fetch(`${API_BASE}/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        message: message.trim(),
        sessionId: sessionId,
        conversationHistory: conversationHistory.slice(-10),
      }),
    });

    hideTypingIndicator();

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const data = await response.json();

    addMessage(
      'assistant',
      data.response || 'I couldn\'t generate a response. Please try again.',
      data.sources || [],
      data.intent || '',
      data.confidence || 0,
      data.escalated || false
    );

    conversationHistory.push({ role: 'assistant', content: data.response || '' });
  } catch (error) {
    hideTypingIndicator();
    addMessage(
      'assistant',
      'I\'m temporarily unable to process your request. Please try again shortly or contact UCC directly at https://ucc.co.tz/.',
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

// Enter key to send
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
});
