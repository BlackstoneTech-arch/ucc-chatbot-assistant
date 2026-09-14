/* ============================================
   UCC ADMIN NEWS — CRUD helper
   - Public news: GET /api/news (via UCCNews)
   - Admin news: GET/POST/PUT/DELETE /api/admin/news
   - Falls back to local seed when backend is unreachable.
   - Exposes window.UCCNewsAdmin
   ============================================ */
(function () {
  'use strict';

  const SEED_URL = 'data/news.json';
  const ADMIN_URL = '/api/admin/news';
  const API_URL = '/api/news';

  function apiBase() {
    return (typeof API_BASE_URL !== 'undefined' && API_BASE_URL) ? API_BASE_URL : '';
  }

  function token() {
    return (typeof window !== 'undefined' && window.__UCC_ADMIN_TOKEN) ? window.__UCC_ADMIN_TOKEN : null;
  }

  async function request(endpoint, options = {}) {
    const base = apiBase();
    if (!base) return null;
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    const t = token();
    if (t) headers['Authorization'] = 'Bearer ' + t;
    const controller = new AbortController();
    const tmo = setTimeout(() => controller.abort(), 8000);
    try {
      const r = await fetch(`${base}${endpoint}`, { signal: controller.signal, headers, ...options });
      clearTimeout(tmo);
      if (!r.ok) return null;
      return await r.json();
    } catch (_) { return null; }
  }

  async function listAll() {
    const data = await request(ADMIN_URL);
    if (!data) return null;
    return Array.isArray(data) ? data : (data.items || data.news || []);
  }

  async function create(item) {
    return await request(ADMIN_URL, { method: 'POST', body: JSON.stringify(item) });
  }

  async function update(id, item) {
    return await request(`${ADMIN_URL}/${id}`, { method: 'PUT', body: JSON.stringify(item) });
  }

  async function remove(id) {
    return await request(`${ADMIN_URL}/${id}`, { method: 'DELETE' });
  }

  async function seed() {
    try {
      const r = await fetch(SEED_URL);
      if (!r.ok) return [];
      const data = await r.json();
      return data.items || data.news || [];
    } catch (_) { return []; }
  }

  window.UCCNewsAdmin = {
    listAll,
    create,
    update,
    remove,
    seed,
    hasBackend: () => !!apiBase()
  };
})();