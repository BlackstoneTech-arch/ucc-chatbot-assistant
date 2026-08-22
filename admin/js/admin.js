// ============================================
// ADMIN CONFIGURATION - UPDATE THIS FOR PRODUCTION
// ============================================
const ADMIN_CONFIG = {
  API_BASE_URL: window.location.hostname === "localhost"
    ? "http://localhost:8081/api"
    : "https://YOUR-BACKEND-DOMAIN/api"
};
// ============================================

const API_BASE_URL = ADMIN_CONFIG.API_BASE_URL;

let currentTab = 'overview';
let authToken = localStorage.getItem('ucc_token');
let allDocuments = [];
let allConversations = [];
let allPrompts = [];
let allIntegrations = [];
let allFaqs = [];
let allLogs = [];
let editingDocumentId = null;
let editingPromptId = null;
let editingIntegrationId = null;
let editingFaqId = null;

async function apiRequest(endpoint, options = {}) {
  const token = localStorage.getItem('ucc_token');
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  if (response.status === 401) {
    logout();
    throw new Error('Unauthorized');
  }

  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    throw new Error(data.error || `HTTP ${response.status}`);
  }

  return response.json();
}

async function handleLogin(event) {
  event.preventDefault();
  const email = document.getElementById('login-email').value.trim();
  const password = document.getElementById('login-password').value;
  const errorEl = document.getElementById('login-error');
  const btn = document.getElementById('login-btn');

  errorEl.classList.add('hidden');
  btn.querySelector('.btn-text').classList.add('hidden');
  btn.querySelector('.btn-loader').classList.remove('hidden');
  btn.disabled = true;

  try {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });

    if (!response.ok) {
      const data = await response.json().catch(() => ({}));
      throw new Error(data.error || 'Invalid credentials');
    }

    const data = await response.json();
    authToken = data.token;
    localStorage.setItem('ucc_token', authToken);
    localStorage.setItem('ucc_user', JSON.stringify(data));

    document.getElementById('login-screen').classList.add('hidden');
    document.getElementById('dashboard-screen').classList.remove('hidden');
    document.getElementById('user-name').textContent = data.fullName || data.email || 'Admin';

    await loadDashboard();
  } catch (error) {
    errorEl.textContent = error.message;
    errorEl.classList.remove('hidden');
  } finally {
    btn.querySelector('.btn-text').classList.remove('hidden');
    btn.querySelector('.btn-loader').classList.add('hidden');
    btn.disabled = false;
  }
}

function logout() {
  localStorage.removeItem('ucc_token');
  localStorage.removeItem('ucc_user');
  authToken = null;
  document.getElementById('dashboard-screen').classList.add('hidden');
  document.getElementById('login-screen').classList.remove('hidden');
}

function isAuthenticated() {
  return !!localStorage.getItem('ucc_token');
}

function switchTab(tabName) {
  currentTab = tabName;

  document.querySelectorAll('.nav-item').forEach(tab => {
    tab.classList.toggle('active', tab.dataset.tab === tabName);
  });

  document.querySelectorAll('.tab-panel').forEach(panel => {
    panel.classList.toggle('active', panel.id === `tab-${tabName}`);
  });

  if (tabName === 'dashboard') loadDashboard();
  if (tabName === 'knowledge') loadDocuments();
  if (tabName === 'ai-training') loadPrompts();
  if (tabName === 'apis') loadIntegrations();
  if (tabName === 'conversations') loadConversations();
  if (tabName === 'logs') loadLogs();
  if (tabName === 'faqs') loadFaqs();
}

async function loadDashboard() {
  try {
    const data = await apiRequest('/admin/dashboard');
    document.getElementById('stat-conversations').textContent = data.totalConversations || 0;
    document.getElementById('stat-messages').textContent = data.totalMessages || 0;
    document.getElementById('stat-rating').textContent = data.averageRating ? data.averageRating.toFixed(1) : '0';
    document.getElementById('stat-documents').textContent = data.activeDocuments || 0;
    document.getElementById('stat-errors').textContent = data.errorCount || 0;

    const topIntents = document.getElementById('top-intents');
    if (data.topIntents && data.topIntents.length > 0) {
      topIntents.innerHTML = data.topIntents
        .map(intent => `
          <div class="intent-item">
            <span class="intent-name">${escapeHtml(intent.intent || intent.name || 'Unknown')}</span>
            <span class="intent-count">${intent.count || 0}</span>
          </div>
        `)
        .join('');
    } else {
      topIntents.innerHTML = '<p class="text-center text-gray-500">No data available</p>';
    }
  } catch (error) {
    console.error('Failed to load dashboard:', error);
  }
}

// Knowledge Base
async function loadDocuments() {
  try {
    const data = await apiRequest('/admin/knowledge?limit=100');
    allDocuments = Array.isArray(data) ? data : (data.data || data.content || []);
    renderDocuments(allDocuments);
  } catch (error) {
    console.error('Failed to load documents:', error);
    document.getElementById('documents-table-body').innerHTML = '<tr><td colspan="7" class="text-center">Error loading documents</td></tr>';
  }
}

function renderDocuments(documents) {
  const tbody = document.getElementById('documents-table-body');
  if (documents.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7" class="text-center">No documents found</td></tr>';
    return;
  }

  tbody.innerHTML = documents.map(doc => `
    <tr>
      <td><strong>${escapeHtml(doc.title || 'Untitled')}</strong></td>
      <td>${escapeHtml(doc.category || '-')}</td>
      <td><span class="badge ${getStatusBadgeClass(doc.approvalStatus || doc.status)}">${doc.approvalStatus || doc.status || 'PENDING'}</span></td>
      <td>${doc.academicYear || '-'}</td>
      <td>${escapeHtml(doc.sourceType || '-')}</td>
      <td>${formatDate(doc.updatedAt || doc.updated_at)}</td>
      <td>
        <button class="btn-sm btn-secondary" onclick="editDocument('${doc.id}')">Edit</button>
        <button class="btn-sm btn-secondary" style="color: var(--ucc-error);" onclick="deleteDocument('${doc.id}')">Delete</button>
      </td>
    </tr>
  `).join('');
}

function getStatusBadgeClass(status) {
  if (!status) return 'badge-gray';
  const s = status.toUpperCase();
  if (s === 'APPROVED' || s === 'ACTIVE') return 'badge-success';
  if (s === 'PENDING') return 'badge-warning';
  if (s === 'REJECTED' || s === 'INACTIVE') return 'badge-error';
  return 'badge-gray';
}

function filterDocuments() {
  const search = document.getElementById('kb-search').value.toLowerCase();
  const category = document.getElementById('kb-category-filter').value;
  const status = document.getElementById('kb-status-filter').value;

  const filtered = allDocuments.filter(doc => {
    const matchesSearch = !search || (doc.title || '').toLowerCase().includes(search) || (doc.content || '').toLowerCase().includes(search);
    const matchesCategory = !category || (doc.category || '').toLowerCase() === category.toLowerCase();
    const matchesStatus = !status || (doc.approvalStatus || doc.status || '').toUpperCase() === status.toUpperCase();
    return matchesSearch && matchesCategory && matchesStatus;
  });

  renderDocuments(filtered);
}

function openDocumentModal(id = null) {
  editingDocumentId = id;
  document.getElementById('document-modal-title').textContent = id ? 'Edit Document' : 'New Document';
  document.getElementById('document-form').reset();

  if (id) {
    const doc = allDocuments.find(d => d.id === id);
    if (doc) {
      document.getElementById('doc-title').value = doc.title || '';
      document.getElementById('doc-category').value = doc.category || '';
      document.getElementById('doc-year').value = doc.academicYear || '';
      document.getElementById('doc-content').value = doc.content || '';
      document.getElementById('doc-source-url').value = doc.sourceUrl || '';
      document.getElementById('doc-source-type').value = doc.sourceType || 'MANUAL';
      document.getElementById('doc-status').value = doc.approvalStatus || doc.status || 'PENDING';
    }
  }

  document.getElementById('document-modal').classList.remove('hidden');
}

function closeDocumentModal() {
  document.getElementById('document-modal').classList.add('hidden');
  editingDocumentId = null;
}

async function saveDocument(event) {
  event.preventDefault();

  const docData = {
    title: document.getElementById('doc-title').value,
    category: document.getElementById('doc-category').value,
    academicYear: document.getElementById('doc-year').value,
    content: document.getElementById('doc-content').value,
    sourceUrl: document.getElementById('doc-source-url').value,
    sourceType: document.getElementById('doc-source-type').value,
    approvalStatus: document.getElementById('doc-status').value,
  };

  try {
    if (editingDocumentId) {
      await apiRequest(`/api/admin/knowledge/${editingDocumentId}`, {
        method: 'PUT',
        body: JSON.stringify(docData),
      });
    } else {
      await apiRequest('/api/admin/knowledge', {
        method: 'POST',
        body: JSON.stringify(docData),
      });
    }

    closeDocumentModal();
    loadDocuments();
  } catch (error) {
    alert('Failed to save document: ' + error.message);
  }
}

async function editDocument(id) {
  openDocumentModal(id);
}

async function deleteDocument(id) {
  if (!confirm('Are you sure you want to delete this document?')) return;
  try {
    await apiRequest(`/api/admin/knowledge/${id}`, { method: 'DELETE' });
    loadDocuments();
  } catch (error) {
    alert('Failed to delete document: ' + error.message);
  }
}

function exportKnowledge() {
  const dataStr = JSON.stringify(allDocuments, null, 2);
  const blob = new Blob([dataStr], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `knowledge-base-export-${new Date().toISOString().split('T')[0]}.json`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

// File Upload
const uploadZone = document.getElementById('upload-zone');
const fileInput = document.getElementById('file-input');

if (uploadZone && fileInput) {
  uploadZone.addEventListener('click', () => fileInput.click());
  uploadZone.addEventListener('dragover', (e) => {
    e.preventDefault();
    uploadZone.classList.add('dragover');
  });
  uploadZone.addEventListener('dragleave', () => {
    uploadZone.classList.remove('dragover');
  });
  uploadZone.addEventListener('drop', (e) => {
    e.preventDefault();
    uploadZone.classList.remove('dragover');
    handleFiles(e.dataTransfer.files);
  });
  fileInput.addEventListener('change', (e) => {
    handleFiles(e.target.files);
  });
}

async function handleFiles(files) {
  for (const file of files) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('category', 'imported');
    formData.append('sourceType', 'FILE');

    try {
      const response = await fetch(`${API_BASE_URL}/admin/knowledge/upload`, {
        method: 'POST',
        headers: {
          ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
        },
        body: formData,
      });

      if (!response.ok) throw new Error('Upload failed');
      await loadDocuments();
      alert(`File "${file.name}" uploaded successfully.`);
    } catch (error) {
      alert(`Failed to upload "${file.name}": ${error.message}`);
    }
  }
}

// AI Training
async function loadPrompts() {
  try {
    const data = await apiRequest('/admin/ai/prompts');
    allPrompts = Array.isArray(data) ? data : [];
    renderPrompts(allPrompts);
  } catch (error) {
    console.error('Failed to load prompts:', error);
    document.getElementById('prompts-list').innerHTML = '<p class="text-center text-gray-500">Error loading prompts</p>';
  }
}

function renderPrompts(prompts) {
  const container = document.getElementById('prompts-list');
  if (prompts.length === 0) {
    container.innerHTML = '<p class="text-center text-gray-500">No prompt templates found</p>';
    return;
  }

  container.innerHTML = prompts.map(prompt => `
    <div class="prompt-item">
      <div class="prompt-item-header">
        <span class="prompt-name">${escapeHtml(prompt.name || 'Unnamed')}</span>
        <span class="prompt-type">${prompt.type || 'SYSTEM_PROMPT'}</span>
      </div>
      <p class="prompt-content">${escapeHtml(prompt.content || '')}</p>
      <div class="faq-actions">
        <button class="btn-sm btn-secondary" onclick="editPrompt('${prompt.id}')">Edit</button>
        <button class="btn-sm btn-secondary" style="color: var(--ucc-error);" onclick="deletePrompt('${prompt.id}')">Delete</button>
      </div>
    </div>
  `).join('');
}

function openPromptModal(id = null) {
  editingPromptId = id;
  document.getElementById('prompt-modal-title').textContent = id ? 'Edit Prompt Template' : 'New Prompt Template';
  document.getElementById('prompt-form').reset();

  if (id) {
    const prompt = allPrompts.find(p => p.id === id);
    if (prompt) {
      document.getElementById('prompt-name').value = prompt.name || '';
      document.getElementById('prompt-type').value = prompt.type || 'SYSTEM_PROMPT';
      document.getElementById('prompt-content').value = prompt.content || '';
      document.getElementById('prompt-variables').value = (prompt.variables || []).join(', ');
    }
  }

  document.getElementById('prompt-modal').classList.remove('hidden');
}

function closePromptModal() {
  document.getElementById('prompt-modal').classList.add('hidden');
  editingPromptId = null;
}

async function savePrompt(event) {
  event.preventDefault();
  const promptData = {
    name: document.getElementById('prompt-name').value,
    type: document.getElementById('prompt-type').value,
    content: document.getElementById('prompt-content').value,
    variables: document.getElementById('prompt-variables').value.split(',').map(v => v.trim()).filter(Boolean),
  };

  try {
    if (editingPromptId) {
      await apiRequest(`/api/admin/ai/prompts/${editingPromptId}`, {
        method: 'PUT',
        body: JSON.stringify(promptData),
      });
    } else {
      await apiRequest('/api/admin/ai/prompts', {
        method: 'POST',
        body: JSON.stringify(promptData),
      });
    }

    closePromptModal();
    loadPrompts();
  } catch (error) {
    alert('Failed to save prompt: ' + error.message);
  }
}

async function editPrompt(id) {
  openPromptModal(id);
}

async function deletePrompt(id) {
  if (!confirm('Are you sure you want to delete this prompt template?')) return;
  try {
    await apiRequest(`/api/admin/ai/prompts/${id}`, { method: 'DELETE' });
    loadPrompts();
  } catch (error) {
    alert('Failed to delete prompt: ' + error.message);
  }
}

async function runAITest() {
  const query = document.getElementById('test-query').value.trim();
  const promptId = document.getElementById('test-prompt').value;
  const resultEl = document.getElementById('test-result');
  const outputEl = document.getElementById('test-output');

  if (!query) {
    alert('Please enter a test query');
    return;
  }

  resultEl.classList.remove('hidden');
  outputEl.textContent = 'Running test...';

  try {
    const body = { message: query, conversationId: 'test-' + Date.now() };
    if (promptId) body.promptTemplateId = promptId;

    const response = await fetch(`${API_BASE_URL}/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) throw new Error('Test failed');
    const data = await response.json();
    outputEl.textContent = JSON.stringify(data, null, 2);
  } catch (error) {
    outputEl.textContent = 'Error: ' + error.message;
  }
}

// API Integrations
async function loadIntegrations() {
  try {
    const data = await apiRequest('/admin/integrations');
    allIntegrations = Array.isArray(data) ? data : (data.data || data.content || []);
    renderIntegrations(allIntegrations);
  } catch (error) {
    console.error('Failed to load integrations:', error);
  }
}

function renderIntegrations(integrations) {
  const grid = document.getElementById('integrations-grid');
  if (!grid) return;

  const cards = integrations.map(int => `
    <div class="integration-card">
      <div class="integration-header">
        <div class="integration-icon ${getIntegrationIconClass(int.type)}">
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
        </div>
        <div class="integration-info">
          <h4>${escapeHtml(int.name || 'Unnamed Integration')}</h4>
          <p>${escapeHtml(int.type || 'REST_API')}</p>
        </div>
        <span class="badge ${int.status === 'ACTIVE' ? 'badge-success' : 'badge-gray'}">${int.status || 'Active'}</span>
      </div>
      <div class="integration-body">
        <div class="integration-metric">
          <span class="metric-label">URL</span>
          <span class="metric-value">${escapeHtml(int.baseUrl || int.url || '-')}</span>
        </div>
        <div class="integration-metric">
          <span class="metric-label">Timeout</span>
          <span class="metric-value">${int.timeout || 5000}ms</span>
        </div>
        <div class="integration-metric">
          <span class="metric-label">Retries</span>
          <span class="metric-value">${int.retryCount || 3}</span>
        </div>
      </div>
      <div class="integration-actions">
        <button class="btn-sm btn-secondary" onclick="testIntegration('${int.id}')">Test</button>
        <button class="btn-sm btn-secondary" onclick="editIntegration('${int.id}')">Edit</button>
        <button class="btn-sm btn-secondary" style="color: var(--ucc-error);" onclick="deleteIntegration('${int.id}')">Delete</button>
      </div>
    </div>
  `).join('');

  grid.innerHTML = cards + `
    <div class="integration-card add-new">
      <button class="add-integration-btn" onclick="openIntegrationModal()">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path></svg>
        <span>Add New Integration</span>
      </button>
    </div>
  `;
}

function getIntegrationIconClass(type) {
  switch (type) {
    case 'OPENAI':
    case 'LLM':
      return 'openai';
    case 'DATABASE':
      return 'database';
    case 'WEBHOOK':
      return 'webhook';
    case 'SSO':
      return 'sso';
    case 'FILE_STORAGE':
      return 'file';
    default:
      return 'openai';
  }
}

function openIntegrationModal(id = null) {
  editingIntegrationId = id;
  document.getElementById('integration-modal-title').textContent = id ? 'Edit Integration' : 'Add Integration';
  document.getElementById('integration-form').reset();

  if (id) {
    const int = allIntegrations.find(i => i.id === id);
    if (int) {
      document.getElementById('int-name').value = int.name || '';
      document.getElementById('int-type').value = int.type || 'REST_API';
      document.getElementById('int-url').value = int.baseUrl || int.url || '';
      document.getElementById('int-key').value = int.apiKey || '';
      document.getElementById('int-timeout').value = int.timeout || 5000;
      document.getElementById('int-retry').value = int.retryCount || 3;
    }
  }

  document.getElementById('integration-modal').classList.remove('hidden');
}

function closeIntegrationModal() {
  document.getElementById('integration-modal').classList.add('hidden');
  editingIntegrationId = null;
}

async function saveIntegration(event) {
  event.preventDefault();
  const intData = {
    name: document.getElementById('int-name').value,
    type: document.getElementById('int-type').value,
    baseUrl: document.getElementById('int-url').value,
    apiKey: document.getElementById('int-key').value,
    timeout: parseInt(document.getElementById('int-timeout').value) || 5000,
    retryCount: parseInt(document.getElementById('int-retry').value) || 3,
  };

  try {
    if (editingIntegrationId) {
      await apiRequest(`/api/admin/integrations/${editingIntegrationId}`, {
        method: 'PUT',
        body: JSON.stringify(intData),
      });
    } else {
      await apiRequest('/api/admin/integrations', {
        method: 'POST',
        body: JSON.stringify(intData),
      });
    }

    closeIntegrationModal();
    loadIntegrations();
  } catch (error) {
    alert('Failed to save integration: ' + error.message);
  }
}

async function testIntegration(id) {
  try {
    const response = await apiRequest(`/api/admin/integrations/${id}/test`, { method: 'POST' });
    alert('Connection test result: ' + (response.success ? 'Success' : 'Failed') + '\n' + (response.message || ''));
  } catch (error) {
    alert('Connection test failed: ' + error.message);
  }
}

async function testIntegrationFromModal() {
  const url = document.getElementById('int-url').value;
  if (!url) {
    alert('Please enter a URL first');
    return;
  }
  alert('Testing connection to: ' + url + '\n\nThis would test the integration connection.');
}

function editIntegration(id) {
  openIntegrationModal(id);
}

async function deleteIntegration(id) {
  if (!confirm('Are you sure you want to delete this integration?')) return;
  try {
    await apiRequest(`/api/admin/integrations/${id}`, { method: 'DELETE' });
    loadIntegrations();
  } catch (error) {
    alert('Failed to delete integration: ' + error.message);
  }
}

// Conversations
async function loadConversations() {
  try {
    const data = await apiRequest('/admin/conversations?limit=50');
    allConversations = Array.isArray(data) ? data : (data.data || data.content || []);
    renderConversations(allConversations);
  } catch (error) {
    console.error('Failed to load conversations:', error);
    document.getElementById('conversations-table-body').innerHTML = '<tr><td colspan="7" class="text-center">Error loading conversations</td></tr>';
  }
}

function renderConversations(conversations) {
  const tbody = document.getElementById('conversations-table-body');
  if (conversations.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7" class="text-center">No conversations found</td></tr>';
    return;
  }

  tbody.innerHTML = conversations.map(conv => `
    <tr>
      <td><code>${conv.sessionId ? conv.sessionId.substring(0, 8) + '...' : '-'}</code></td>
      <td>${escapeHtml(conv.userEmail || conv.userName || 'Anonymous')}</td>
      <td>${conv.messageCount || conv.messages?.length || 0}</td>
      <td>${formatDate(conv.startedAt || conv.started_at)}</td>
      <td>${formatDate(conv.updatedAt || conv.updated_at)}</td>
      <td><span class="badge ${conv.isActive ? 'badge-success' : 'badge-gray'}">${conv.isActive ? 'Active' : 'Ended'}</span></td>
      <td>
        <button class="btn-sm btn-secondary" onclick="viewConversation('${conv.id}')">View</button>
      </td>
    </tr>
  `).join('');
}

function filterConversations() {
  const search = document.getElementById('conv-search').value.toLowerCase();
  const status = document.getElementById('conv-status-filter').value;

  const filtered = allConversations.filter(conv => {
    const matchesSearch = !search || (conv.sessionId || '').toLowerCase().includes(search) || (conv.userEmail || '').toLowerCase().includes(search);
    const matchesStatus = !status || (status === 'active' ? conv.isActive : !conv.isActive);
    return matchesSearch && matchesStatus;
  });

  renderConversations(filtered);
}

function viewConversation(id) {
  const conv = allConversations.find(c => c.id === id);
  if (!conv) return;
  alert(`Conversation Details:\n\nSession: ${conv.sessionId}\nUser: ${conv.userEmail || conv.userName || 'Anonymous'}\nMessages: ${conv.messageCount || conv.messages?.length || 0}\nStatus: ${conv.isActive ? 'Active' : 'Ended'}\nStarted: ${formatDate(conv.startedAt || conv.started_at)}`);
}

// System Logs
async function loadLogs() {
  try {
    const data = await apiRequest('/admin/logs?limit=100');
    allLogs = Array.isArray(data) ? data : (data.data || data.content || []);
    renderLogs(allLogs);
    updateLogStats(allLogs);
  } catch (error) {
    console.error('Failed to load logs:', error);
    document.getElementById('log-content').innerHTML = '<div class="log-line error"><span class="log-time">' + formatTimestamp(new Date()) + '</span><span class="log-component">System</span> Error loading logs</div>';
  }
}

function renderLogs(logs) {
  const container = document.getElementById('log-content');
  if (logs.length === 0) {
    container.innerHTML = '<div class="log-line info"><span class="log-time">' + formatTimestamp(new Date()) + '</span><span class="log-component">System</span> No logs available</div>';
    return;
  }

  container.innerHTML = logs.map(log => `
    <div class="log-line ${log.level?.toLowerCase() || 'info'}">
      <span class="log-time">${formatTimestamp(new Date(log.timestamp || log.createdAt || Date.now()))}</span>
      <span class="log-level">${log.level || 'INFO'}</span>
      <span class="log-component">${log.component || 'System'}</span>
      <span class="log-message">${escapeHtml(log.message || log.msg || '')}</span>
    </div>
  `).join('');
}

function updateLogStats(logs) {
  const errors = logs.filter(l => (l.level || '').toUpperCase() === 'ERROR').length;
  const warnings = logs.filter(l => (l.level || '').toUpperCase() === 'WARN').length;
  const infos = logs.filter(l => (l.level || '').toUpperCase() === 'INFO').length;

  document.getElementById('error-count').textContent = errors;
  document.getElementById('warning-count').textContent = warnings;
  document.getElementById('info-count').textContent = infos;

  const badge = document.getElementById('error-badge');
  if (errors > 0) {
    badge.textContent = errors;
    badge.classList.remove('hidden');
  } else {
    badge.classList.add('hidden');
  }
}

function filterLogs() {
  const level = document.getElementById('log-level-filter').value;
  const component = document.getElementById('log-component-filter').value;
  const date = document.getElementById('log-date-filter').value;

  const filtered = allLogs.filter(log => {
    const matchesLevel = !level || (log.level || '').toUpperCase() === level.toUpperCase();
    const matchesComponent = !component || (log.component || '').toLowerCase().includes(component.toLowerCase());
    const matchesDate = !date || (log.timestamp || log.createdAt || '').startsWith(date);
    return matchesLevel && matchesComponent && matchesDate;
  });

  renderLogs(filtered);
}

function clearLogs() {
  document.getElementById('log-content').innerHTML = '';
}

function exportLogs() {
  const dataStr = JSON.stringify(allLogs, null, 2);
  const blob = new Blob([dataStr], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `system-logs-${new Date().toISOString().split('T')[0]}.json`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

// FAQs
async function loadFaqs() {
  try {
    const data = await apiRequest('/api/admin/faqs');
    allFaqs = Array.isArray(data) ? data : (data.data || data.content || []);
    renderFaqs(allFaqs);
  } catch (error) {
    console.error('Failed to load FAQs:', error);
    document.getElementById('faqs-list').innerHTML = '<p class="text-center text-gray-500">Error loading FAQs</p>';
  }
}

function renderFaqs(faqs) {
  const container = document.getElementById('faqs-list');
  if (faqs.length === 0) {
    container.innerHTML = '<p class="text-center text-gray-500">No FAQs found</p>';
    return;
  }

  container.innerHTML = faqs.map(faq => `
    <div class="faq-item">
      <div class="faq-item-header">
        <div class="faq-question">${escapeHtml(faq.question || 'Untitled Question')}</div>
        <span class="badge ${faq.isActive !== false ? 'badge-success' : 'badge-gray'}">${faq.isActive !== false ? 'Active' : 'Inactive'}</span>
      </div>
      <p class="faq-answer">${escapeHtml(faq.answer || '')}</p>
      <div class="faq-actions">
        <button class="btn-sm btn-secondary" onclick="editFaq('${faq.id}')">Edit</button>
        <button class="btn-sm btn-secondary" style="color: var(--ucc-error);" onclick="deleteFaq('${faq.id}')">Delete</button>
      </div>
    </div>
  `).join('');
}

function openFaqModal(id = null) {
  editingFaqId = id;
  document.getElementById('faq-modal-title').textContent = id ? 'Edit FAQ' : 'New FAQ';
  document.getElementById('faq-form').reset();

  if (id) {
    const faq = allFaqs.find(f => f.id === id);
    if (faq) {
      document.getElementById('faq-question').value = faq.question || '';
      document.getElementById('faq-answer').value = faq.answer || '';
      document.getElementById('faq-category').value = faq.category || '';
    }
  }

  document.getElementById('faq-modal').classList.remove('hidden');
}

function closeFaqModal() {
  document.getElementById('faq-modal').classList.add('hidden');
  editingFaqId = null;
}

async function saveFaq(event) {
  event.preventDefault();
  const faqData = {
    question: document.getElementById('faq-question').value,
    answer: document.getElementById('faq-answer').value,
    category: document.getElementById('faq-category').value,
    isActive: true,
  };

  try {
    if (editingFaqId) {
      await apiRequest(`/api/admin/faqs/${editingFaqId}`, {
        method: 'PUT',
        body: JSON.stringify(faqData),
      });
    } else {
      await apiRequest('/api/admin/faqs', {
        method: 'POST',
        body: JSON.stringify(faqData),
      });
    }

    closeFaqModal();
    loadFaqs();
  } catch (error) {
    alert('Failed to save FAQ: ' + error.message);
  }
}

function editFaq(id) {
  openFaqModal(id);
}

async function deleteFaq(id) {
  if (!confirm('Are you sure you want to delete this FAQ?')) return;
  try {
    await apiRequest(`/api/admin/faqs/${id}`, { method: 'DELETE' });
    loadFaqs();
  } catch (error) {
    alert('Failed to delete FAQ: ' + error.message);
  }
}

// Utility Functions
function escapeHtml(text) {
  if (!text) return '';
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

function formatDate(dateString) {
  if (!dateString) return '-';
  const date = new Date(dateString);
  if (isNaN(date.getTime())) return '-';
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function formatTimestamp(date) {
  if (!date) return new Date().toISOString();
  return date.toISOString().replace('T', ' ').substring(0, 19);
}

// Initialize
document.addEventListener('DOMContentLoaded', () => {
  if (!isAuthenticated()) {
    document.getElementById('login-screen').classList.remove('hidden');
    document.getElementById('dashboard-screen').classList.add('hidden');
  } else {
    document.getElementById('login-screen').classList.add('hidden');
    document.getElementById('dashboard-screen').classList.remove('hidden');
    const user = JSON.parse(localStorage.getItem('ucc_user') || '{}');
    document.getElementById('user-name').textContent = user.fullName || user.email || 'Admin';
    loadDashboard();
  }
});
