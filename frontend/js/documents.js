/* ============================================
   UCC Documents — Downloads page renderer
   Reads data/documents.json, renders:
   - The application-pack grid
   - The category filter chips
   - The full document grid
   Also exposes:
   - findDocById(id)
   - searchDocs(query)
   - getIntakePack()
   ============================================ */
(function () {
  'use strict';
  let DATA = null;

  async function loadData() {
    if (DATA) return DATA;
    try {
      const r = await fetch('data/documents.json', { cache: 'no-cache' });
      if (!r.ok) throw new Error('HTTP ' + r.status);
      DATA = await r.json();
    } catch (e) {
      DATA = { version: 'unknown', lastUpdated: '', categories: [], documents: [], intakePack: { documentIds: [] }, intake: {} };
    }
    return DATA;
  }

  function escapeHtml(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function cardHtml(doc) {
    return `
      <article class="doc-card" data-cat="${escapeHtml(doc.category)}" data-id="${escapeHtml(doc.id)}">
        <div class="doc-icon" aria-hidden="true">${getCategoryIcon(doc.category)}</div>
        <div class="doc-body">
          <h3>${escapeHtml(doc.title)}</h3>
          <p>${escapeHtml(doc.description || '')}</p>
          <p class="doc-meta">
            <span class="badge">${escapeHtml(doc.fileType || 'PDF')}</span>
            ${doc.programme ? `<span class="badge badge-blue">${escapeHtml(doc.programme)}</span>` : ''}
            <span class="muted">Source: ${escapeHtml(doc.source || 'ucc.co.tz')}</span>
          </p>
          <a class="btn btn-primary doc-download" href="${escapeHtml(doc.downloadUrl)}" target="_blank" rel="noopener noreferrer" data-id="${escapeHtml(doc.id)}" download>
            ⬇ Download
          </a>
        </div>
      </article>`;
  }

  function getCategoryIcon(cat) {
    const map = { prospectus: '📘', admission: '🎓', calendar: '📅', timetable: '🕒', company: '🏢' };
    return map[cat] || '📄';
  }

  function renderIntakePack() {
    const grid = document.getElementById('intake-pack-grid');
    if (!grid) return;
    const pack = DATA.intakePack;
    const docs = (pack.documentIds || []).map(id => DATA.documents.find(d => d.id === id)).filter(Boolean);
    if (!docs.length) {
      grid.innerHTML = '<p class="muted">No documents available right now. Please check back soon.</p>';
      return;
    }
    grid.innerHTML = docs.map(cardHtml).join('');
  }

  function renderCategoryFilters() {
    const wrap = document.querySelector('.category-filters');
    if (!wrap) return;
    const cats = DATA.categories || [];
    const html = ['<button class="cat-chip active" type="button" data-cat="all" role="tab" aria-selected="true">All</button>']
      .concat(cats.map(c => `<button class="cat-chip" type="button" data-cat="${escapeHtml(c.id)}" role="tab" aria-selected="false">${escapeHtml(c.icon || '')} ${escapeHtml(c.name)}</button>`));
    wrap.innerHTML = html.join('');
    wrap.querySelectorAll('.cat-chip').forEach(btn => {
      btn.addEventListener('click', () => {
        wrap.querySelectorAll('.cat-chip').forEach(b => { b.classList.remove('active'); b.setAttribute('aria-selected', 'false'); });
        btn.classList.add('active');
        btn.setAttribute('aria-selected', 'true');
        filterGrid(btn.dataset.cat);
      });
    });
  }

  function filterGrid(cat) {
    const grid = document.getElementById('document-grid');
    if (!grid) return;
    const cards = grid.querySelectorAll('.doc-card');
    cards.forEach(c => {
      const ok = (cat === 'all') || (c.dataset.cat === cat);
      c.style.display = ok ? '' : 'none';
    });
  }

  function renderGrid() {
    const grid = document.getElementById('document-grid');
    if (!grid) return;
    const docs = DATA.documents || [];
    if (!docs.length) {
      grid.innerHTML = '<p class="muted">No documents available right now.</p>';
      return;
    }
    grid.innerHTML = docs.map(cardHtml).join('');
  }

  async function render() {
    await loadData();
    renderIntakePack();
    renderCategoryFilters();
    renderGrid();
  }

  function findDocById(id) {
    if (!DATA) return null;
    return (DATA.documents || []).find(d => d.id === id) || null;
  }

  function searchDocs(query) {
    if (!DATA) return [];
    const q = (query || '').toLowerCase().trim();
    if (!q) return [];
    return (DATA.documents || []).filter(d => {
      const hay = [d.title, d.description, d.category, d.programme, ...(d.tags || [])].filter(Boolean).join(' ').toLowerCase();
      return hay.includes(q);
    });
  }

  function getIntakePack() {
    if (!DATA || !DATA.intakePack) return null;
    const pack = DATA.intakePack;
    return {
      ...pack,
      documents: (pack.documentIds || []).map(id => findDocById(id)).filter(Boolean)
    };
  }

  function getIntakeInfo() { return DATA ? DATA.intake : null; }

  function getAllDocs() { return DATA ? (DATA.documents || []) : []; }

  window.UCCDocuments = { render, findDocById, searchDocs, getIntakePack, getIntakeInfo, getAllDocs };
})();
