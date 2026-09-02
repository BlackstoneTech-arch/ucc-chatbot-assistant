// ============================================
// API CONFIGURATION
// Backend runs locally on port 8081 (Spring Boot).
// For production, set a public URL or rely on the
// client-side ucc-kb.js fallback (always available).
// ============================================
const API_CONFIG = {
  BASE_URL: (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1")
    ? "http://localhost:8081/api"
    : "" // Production: leave empty to skip backend and use ucc-kb.js fallback
};
// ============================================

const API_BASE_URL = API_CONFIG.BASE_URL;

async function apiRequest(endpoint, options = {}) {
    if (!API_BASE_URL) {
      throw new Error("Backend API not configured for this host");
    }
    const response = await fetch(
        `${API_BASE_URL}${endpoint}`,
        {
            headers: {
                "Content-Type": "application/json",
                ...(options.headers || {})
            },
            ...options
        }
    );

    if (!response.ok) {
        throw new Error(`API request failed: ${response.status}`);
    }

    return response.json();
}
