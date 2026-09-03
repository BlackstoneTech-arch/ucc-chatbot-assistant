/* ============================================
   API Configuration
   ============================================
   Production (Netlify): leave BASE_URL empty to use the
   bundled client-side knowledge base (ucc-kb.js). This
   guarantees the chat works without any backend.

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
  BASE_URL: (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1")
    ? "http://localhost:8081/api"
    : "" // Production: empty => KB-only mode
};
const API_BASE_URL = API_CONFIG.BASE_URL;

async function apiRequest(endpoint, options = {}) {
  if (!API_BASE_URL) throw new Error("Backend API not configured for this host");
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options
  });
  if (!response.ok) throw new Error(`API request failed: ${response.status}`);
  return response.json();
}
