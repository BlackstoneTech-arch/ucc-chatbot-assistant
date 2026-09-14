/* ============================================
   UCC NEWS / UPDATES — fetch layer
   - Tries the live backend /api/news first.
   - Falls back to a local seed file (data/news.json) when the
     backend is unreachable (production Netlify build).
   - Exposes window.UCCNews
   ============================================ */
(function () {
  'use strict';

  const SEED_URL = 'data/news.json';
  const API_URL = '/api/news';

  function apiBase() {
    return (typeof API_BASE_URL !== 'undefined' && API_BASE_URL) ? API_BASE_URL : '';
  }

  function normalize(items) {
    return (items || []).map(n => ({
      id: n.id || 'n-' + Math.random().toString(36).slice(2, 8),
      title: n.title || '',
      summary: n.summary || '',
      content: n.content || '',
      sourceUrl: n.sourceUrl || n.source_url || '',
      publishedAt: n.publishedAt || n.published_at || '',
      isPublished: n.isPublished !== false,
      priority: typeof n.priority === 'number' ? n.priority : 0
    }));
  }

  function sort(items) {
    return items
      .filter(n => n.isPublished)
      .sort((a, b) => (b.priority || 0) - (a.priority || 0));
  }

  async function fetchFromApi() {
    if (!apiBase()) return null;
    try {
      const controller = new AbortController();
      const t = setTimeout(() => controller.abort(), 5000);
      const r = await fetch(`${apiBase()}${API_URL}`, { signal: controller.signal });
      clearTimeout(t);
      if (!r.ok) return null;
      const data = await r.json();
      return sort(normalize(Array.isArray(data) ? data : (data.items || data.news || [])));
    } catch (_) { return null; }
  }

  async function fetchFromSeed() {
    try {
      const r = await fetch(SEED_URL);
      if (!r.ok) return null;
      const data = await r.json();
      return sort(normalize(data.items || data.news || []));
    } catch (_) { return null; }
  }

  let cache = null;

  async function getNews(force) {
    if (cache && !force) return cache;
    let items = await fetchFromApi();
    if (!items) items = await fetchFromSeed();
    if (!items) items = [];
    cache = items;
    return items;
  }

  async function refresh() { return getNews(true); }

  window.UCCNews = {
    get: getNews,
    refresh,
    count: () => cache ? cache.length : 0
  };

  // ---------- Auto-render the marquee on every page that includes this script ----------
  function renderMarquee(items) {
    const track = document.getElementById('news-marquee-track');
    if (!track) return;
    if (!items || !items.length) {
      track.innerHTML = '<span class="news-marquee-empty">No news or updates at this time.</span>';
      return;
    }
    // Duplicate the list so the scroll is seamless (looping marquee).
    const list = items.map(n => `
      <span class="news-marquee-item">
        <span class="news-marquee-tag">Update</span>
        <a class="news-marquee-link" href="${n.sourceUrl || '#'}" target="_blank" rel="noopener noreferrer" title="${(n.title || '').replace(/"/g, '&quot;')}">${(n.title || '').replace(/</g, '&lt;')}</a>
        <span class="news-marquee-dot" aria-hidden="true"></span>
      </span>`).join('');
    track.innerHTML = list + list;
  }

  async function loadMarquee() {
    const items = await getNews();
    renderMarquee(items);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadMarquee);
  } else {
    loadMarquee();
  }
})();