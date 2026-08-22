// ============================================
// ADMIN CONFIGURATION - UPDATE THIS FOR PRODUCTION
// ============================================
const ADMIN_CONFIG = {
  API_BASE_URL: window.location.hostname === "localhost"
    ? "http://localhost:8081/api"
    : "https://YOUR-BACKEND-DOMAIN/api"
};
// ============================================

const API_BASE_URL = ADMIN_CONFIG.API_BASE_URL;

async function login(email, password) {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });

  const data = await response.json();

  if (!response.ok) {
    throw new Error(data.error || 'Login failed');
  }

  localStorage.setItem('ucc_token', data.token);
  localStorage.setItem('ucc_user', JSON.stringify(data));
  return data;
}

function logout() {
  localStorage.removeItem('ucc_token');
  localStorage.removeItem('ucc_user');
  window.location.reload();
}

function isAuthenticated() {
  return !!localStorage.getItem('ucc_token');
}

function getToken() {
  return localStorage.getItem('ucc_token');
}

function getUser() {
  const user = localStorage.getItem('ucc_user');
  return user ? JSON.parse(user) : null;
}

async function refreshToken() {
  const token = getToken();
  if (!token) return false;

  try {
    const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });

    if (!response.ok) {
      logout();
      return false;
    }

    const data = await response.json();
    localStorage.setItem('ucc_token', data.token);
    return true;
  } catch {
    return false;
  }
}

async function getCurrentUser() {
  const token = getToken();
  if (!token) return null;

  try {
    const response = await fetch(`${API_BASE_URL}/auth/me`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    if (!response.ok) return null;
    const data = await response.json();
    return data;
  } catch {
    return null;
  }
}
