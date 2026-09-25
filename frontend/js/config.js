/* ============================================
   API Configuration
   ============================================
   Production uses the same-origin /api proxy by default.
   Auto-resolution order:
     1. window.__API_BASE_URL__
     2. localStorage/sessionStorage API_BASE_URL
     3. Same-origin /api when not localhost
     4. Local backend when frontend is on localhost/127.0.0.1
   ============================================ */
function resolveApiBaseUrl() {
  const host = window.location.hostname;
  const isLocalhost = host === "localhost" || host === "127.0.0.1" || host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.16.");

  if (typeof __API_BASE_URL__ !== "undefined" && __API_BASE_URL__) return __API_BASE_URL__;
  try {
    const stored = localStorage.getItem("API_BASE_URL");
    if (stored) return stored;
  } catch (_) {}
  try {
    const stored = sessionStorage.getItem("API_BASE_URL");
    if (stored) return stored;
  } catch (_) {}

  if (isLocalhost) return "http://localhost:8080/api";
  return "/api";
}

const API_BASE_URL = resolveApiBaseUrl();

async function apiRequest(endpoint, options = {}, retries = 1) {
  if (!API_BASE_URL) throw new Error("Backend API not configured for this host");
  const url = `${API_BASE_URL}${endpoint}`;
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };

  let lastError = null;
  for (let attempt = 0; attempt <= retries; attempt++) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 12000);
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
      const name = err && err.name;
      const transient = name === "AbortError" || name === "TypeError" || !navigator.onLine;
      if (!transient || attempt >= retries) break;
      await new Promise(r => setTimeout(r, 600));
    }
  }
  throw lastError || new Error(`API request failed: ${url}`);
}

async function safeJson(response) {
  const text = await response.text();
  if (!text) return {};
  try {
    return JSON.parse(text);
  } catch (_) {
    return {};
  }
}