const API_BASE = 'http://localhost:5000/api';

async function apiRequest(endpoint, options = {}) {
  const token = localStorage.getItem('ucc_token');
  const response = await fetch(`${API_BASE}${endpoint}`, {
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

async function loadAnalytics() {
  try {
    const data = await apiRequest('/admin/analytics');

    document.getElementById('stat-conversations').textContent = data.totalConversations || 0;
    document.getElementById('stat-messages').textContent = data.totalMessages || 0;
    document.getElementById('stat-rating').textContent = data.averageRating || '0';
    document.getElementById('stat-documents').textContent = data.activeDocuments || 0;

    // Top intents
    const topIntents = document.getElementById('top-intents');
    if (data.topIntents && data.topIntents.length > 0) {
      topIntents.innerHTML = data.topIntents
        .map(
          (intent) => `
            <div class="intent-item">
              <span class="intent-name">${intent.intent}</span>
              <span class="intent-count">${intent.count}</span>
            </div>
          `
        )
        .join('');
    } else {
      topIntents.innerHTML = '<p class="text-center text-gray-500">No data available</p>';
    }

    // Metrics
    const metricsList = document.getElementById('metrics-list');
    metricsList.innerHTML = `
      <div class="metric-item">
        <span class="metric-label">Total Conversations (30 days)</span>
        <span class="metric-value">${data.totalConversations || 0}</span>
      </div>
      <div class="metric-item">
        <span class="metric-label">Total Messages (30 days)</span>
        <span class="metric-value">${data.totalMessages || 0}</span>
      </div>
      <div class="metric-item">
        <span class="metric-label">Average Rating</span>
        <span class="metric-value">${data.averageRating || '0'}</span>
      </div>
      <div class="metric-item">
        <span class="metric-label">Total Feedback</span>
        <span class="metric-value">${data.totalFeedback || 0}</span>
      </div>
    `;
  } catch (error) {
    console.error('Failed to load analytics:', error);
  }
}

async function loadDocuments() {
  try {
    const data = await apiRequest('/admin/documents?limit=50');
    const tbody = document.getElementById('documents-table-body');

    if (data.data && data.data.length > 0) {
      tbody.innerHTML = data.data
        .map(
          (doc) => `
            <tr>
              <td><strong>${escapeHtml(doc.title)}</strong></td>
              <td>${escapeHtml(doc.category || '-')}</td>
              <td><span class="badge ${doc.status === 'ACTIVE' ? 'badge-success' : 'badge-gray'}">${doc.status || 'ACTIVE'}</span></td>
              <td>${doc.academic_year || '-'}</td>
              <td><span class="badge ${doc.is_indexed ? 'badge-success' : 'badge-warning'}">${doc.is_indexed ? 'Yes' : 'No'}</span></td>
            </tr>
          `
        )
        .join('');
    } else {
      tbody.innerHTML = '<tr><td colspan="5" class="text-center">No documents found</td></tr>';
    }
  } catch (error) {
    console.error('Failed to load documents:', error);
    document.getElementById('documents-table-body').innerHTML = '<tr><td colspan="5" class="text-center">Error loading documents</td></tr>';
  }
}

async function loadConversations() {
  try {
    const data = await apiRequest('/admin/conversations?limit=50');
    const tbody = document.getElementById('conversations-table-body');

    if (data.data && data.data.length > 0) {
      tbody.innerHTML = data.data
        .map(
          (conv) => `
            <tr>
              <td><code>${conv.session_id ? conv.session_id.substring(0, 8) + '...' : '-'}</code></td>
              <td>${escapeHtml(conv.user_email || conv.user_name || 'Anonymous')}</td>
              <td>${conv.message_count || 0}</td>
              <td>${formatDate(conv.started_at)}</td>
              <td><span class="badge ${conv.is_active ? 'badge-success' : 'badge-gray'}">${conv.is_active ? 'Active' : 'Ended'}</span></td>
            </tr>
          `
        )
        .join('');
    } else {
      tbody.innerHTML = '<tr><td colspan="5" class="text-center">No conversations found</td></tr>';
    }
  } catch (error) {
    console.error('Failed to load conversations:', error);
    document.getElementById('conversations-table-body').innerHTML = '<tr><td colspan="5" class="text-center">Error loading conversations</td></tr>';
  }
}

function switchTab(tabName) {
  // Update nav tabs
  document.querySelectorAll('.nav-tab').forEach((tab) => {
    tab.classList.toggle('active', tab.dataset.tab === tabName);
  });

  // Update tab content
  document.querySelectorAll('.tab-content').forEach((content) => {
    content.classList.toggle('active', content.id === `tab-${tabName}`);
  });

  // Load data for specific tabs
  if (tabName === 'dashboard') loadAnalytics();
  if (tabName === 'documents') loadDocuments();
  if (tabName === 'conversations') loadConversations();
  if (tabName === 'analytics') loadAnalytics();
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

function formatDate(dateString) {
  if (!dateString) return '-';
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

// Initialize dashboard
document.addEventListener('DOMContentLoaded', () => {
  if (!isAuthenticated()) {
    window.location.href = 'index.html';
    return;
  }

  loadAnalytics();
});
