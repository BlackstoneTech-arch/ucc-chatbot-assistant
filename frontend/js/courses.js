/* ============================================
   UCC COURSES — retrieval layer
   - Tries backend /api/courses first
   - Falls back to data/courses.json
   - Exposes window.UCCCourses
   ============================================ */
(function () {
  'use strict';

  const SEED_URL = 'data/courses.json';
  const API_URL = '/api/courses';

  function apiBase() {
    return (typeof API_BASE_URL !== 'undefined' && API_BASE_URL) ? API_BASE_URL : '';
  }

  async function fetchFromApi() {
    const base = apiBase();
    if (!base) return null;
    const controller = new AbortController();
    const tmo = setTimeout(() => controller.abort(), 8000);
    try {
      const r = await fetch(`${base}${API_URL}`, { signal: controller.signal });
      clearTimeout(tmo);
      if (!r.ok) return null;
      return await r.json();
    } catch (_) { return null; }
  }

  async function fetchSeed() {
    try {
      const r = await fetch(SEED_URL);
      if (!r.ok) return null;
      return await r.json();
    } catch (_) { return null; }
  }

  async function load() {
    const apiData = await fetchFromApi();
    if (apiData && (apiData.programmes || apiData.courses)) return apiData;
    return await fetchSeed();
  }

  function listProgrammes(data) {
    if (!data) return [];
    return data.programmes || data.courses || [];
  }

  function search(data, query) {
    const programmes = listProgrammes(data);
    if (!query || !query.trim()) return programmes;
    const q = query.toLowerCase().trim();
    return programmes.filter(p => {
      const hay = (p.name + ' ' + p.short + ' ' + (p.level || '') + ' ' + (p.focus || '') + ' ' + (p.bestFor || '')).toLowerCase();
      return hay.includes(q);
    });
  }

  function findById(data, id) {
    const programmes = listProgrammes(data);
    return programmes.find(p => p.id === id || p.short === id) || null;
  }

  function formatProgramme(p, lang) {
    if (!p) return '';
    const sw = lang === 'sw';
    const feeLines = p.fees ? Object.entries(p.fees).map(([k, v]) => `- ${k}: ${v}`).join('\n') : '';
    if (sw) {
      return `${p.name} (${p.short})\n` +
        `Muda: ${p.duration || '—'}\n` +
        `Jumla: ${p.totalFee || '—'}\n` +
        `${feeLines ? feeLines + '\n' : ''}` +
        `Vigezo vya kujiunga: ${p.entryRequirements || '—'}\n` +
        `Lengo: ${p.focus || '—'}\n` +
        `Maeneo: ${p.locations || '—'}\n` +
        `Jiandikishe: ${p.applyUrl || '—'}\n` +
        `Chanzo: ${p.sourceUrl || '—'}`;
    }
    return `${p.name} (${p.short})\n` +
      `Duration: ${p.duration || '—'}\n` +
      `Total: ${p.totalFee || '—'}\n` +
      `${feeLines ? feeLines + '\n' : ''}` +
      `Entry requirements: ${p.entryRequirements || '—'}\n` +
      `Focus: ${p.focus || '—'}\n` +
      `Best for: ${p.bestFor || '—'}\n` +
      `Locations: ${p.locations || '—'}\n` +
      `Apply: ${p.applyUrl || '—'}\n` +
      `Source: ${p.sourceUrl || '—'}`;
  }

  function formatList(data, lang) {
    const programmes = listProgrammes(data);
    if (!programmes.length) return lang === 'sw'
      ? 'Hakuna programu zilizopatikana kwa sasa.'
      : 'No programmes available at this time.';
    const sw = lang === 'sw';
    const intro = sw
      ? 'Programu za UCC 2026/2027:\n\n'
      : 'UCC Programmes for 2026/2027:\n\n';
    return intro + programmes.map(p => `• ${p.name} (${p.short}) — ${p.level || '—'}, ${p.duration || '—'}, Total ${p.totalFee || '—'}`).join('\n') +
      '\n\n' + (sw
        ? 'Kwa maelezo zaidi kuhusu programu fulani, uliza kwa jina lake (kama DCIT, DBIT, CCIT, CBIT).'
        : 'For full details on a specific programme, ask by name (e.g. DCIT, DBIT, CCIT, CBIT).');
  }

  async function searchProgrammes(query, lang) {
    const data = await load();
    if (!data) return null;
    const results = search(data, query);
    return { data, results, lang };
  }

  window.UCCCourses = {
    load,
    listProgrammes,
    search,
    findById,
    formatProgramme,
    formatList,
    searchProgrammes,
    hasBackend: () => !!apiBase()
  };
})();