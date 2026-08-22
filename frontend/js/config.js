// ============================================
// API CONFIGURATION - UPDATE FOR PRODUCTION
// ============================================
const API_CONFIG = {
  BASE_URL: window.location.hostname === "localhost"
    ? "http://localhost:8081/api"
    : window.location.hostname === "127.0.0.1"
      ? "http://127.0.0.1:8081/api"
      : "https://YOUR-BACKEND-DOMAIN/api"
};
// ============================================

const API_BASE_URL = API_CONFIG.BASE_URL;

async function apiRequest(endpoint, options = {}) {
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
