const API_BASE_URL = window.location.hostname === "localhost"
    ? "http://localhost:8081/api"
    : "https://YOUR-JAVA-BACKEND-DOMAIN/api";

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
