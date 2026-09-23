/* ============================================
   API Configuration
   ============================================
  Production uses the same-origin /api proxy by default. A separately
  hosted backend can override this with the __API_BASE_URL__ global.

  To connect to a live backend API, set BASE_URL to the
  backend root, for example:
    - "https://ucc-chatbot-api.example.com/api"
    - "http://localhost:8081/api"  (local dev)
  The frontend will then POST to {BASE_URL}/chat and GET
  {BASE_URL}/chat/welcome and POST feedback to
  {BASE_URL}/chat/feedback.

  CORS must allow the deployed frontend origin.
  ============================================ */
const API_CONFIG = {
  BASE_URL: (typeof __API_BASE_URL__ !== 'undefined' && __API_BASE_URL__)
    ? __API_BASE_URL__
    : (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1")
      ? "http://localhost:8081/api"
      // Deployed on Cloudflare Workers: route chat traffic through the public
      // backend tunnel. Override with __API_BASE_URL__ at build time if you
      // host the backend elsewhere.
      : "https://ucc-chatbot-api.loca.lt/api"
};
const API_BASE_URL = API_CONFIG.BASE_URL;

/**
 * Robust fetch wrapper used by the chat UI.
 *
 * - Respects the browser's online/offline state (navigator.onLine).
 * - Aborts on timeout instead of hanging forever (important for slow /
 *   unreliable networks and older browsers).
 * - Retries once on transient 5xx / network errors before giving up.
 * - Never throws an unhandled rejection: always resolves with a Response
 *   or rejects with a descriptive Error the caller can fall back from.
 */
async function apiRequest(endpoint, options = {}, retries = 1) {
  if (!API_BASE_URL) throw new Error("Backend API not configured for this host");
  const url = `${API_BASE_URL}${endpoint}`;
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };

  let lastError = null;
  for (let attempt = 0; attempt <= retries; attempt++) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 20000);
    try {
      const response = await fetch(url, {
        method: options.method || "GET",
        headers,
        body: options.body,
        signal: controller.signal,
        credentials: options.credentials || "same-origin"
      });
      clearTimeout(timer);
      return response;
    } catch (err) {
      clearTimeout(timer);
      lastError = err;
      // Only retry transient failures (offline / timeout / connection refused).
      const name = err && err.name;
      const transient = name === "AbortError" || name === "TypeError" || !navigator.onLine;
      if (!transient || attempt >= retries) break;
      // Small back-off before the retry.
      await new Promise(r => setTimeout(r, 800));
    }
  }
  throw lastError || new Error(`API request failed: ${url}`);
}

/**
 * Parse a JSON body safely. Some backends (or proxies) return HTML error
 * pages on 5xx responses; this avoids a SyntaxError in those cases.
 */
async function safeJson(response) {
  const text = await response.text();
  if (!text) return {};
  try {
    return JSON.parse(text);
  } catch (_) {
    return {};
  }
}