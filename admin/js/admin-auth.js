const API_BASE_URL = window.location.hostname === "localhost"
    ? "http://localhost:8080/api"
    : "https://YOUR-JAVA-BACKEND-DOMAIN/api";

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
  localStorage.setItem('ucc_user', JSON.stringify(data.user));
  return data;
}

function logout() {
  localStorage.removeItem('ucc_token');
  localStorage.removeItem('ucc_user');
  window.location.href = 'index.html';
}

function isAuthenticated() {
  return !!localStorage.getItem('ucc_token');
}

async function getCurrentUser() {
  const token = localStorage.getItem('ucc_token');
  if (!token) return null;

  try {
      const response = await fetch(`${API_BASE_URL}/auth/me`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    if (!response.ok) return null;
    const data = await response.json();
    return data.user;
  } catch {
    return null;
  }
}

// Login form handler
document.addEventListener('DOMContentLoaded', () => {
  const loginForm = document.getElementById('login-form');
  const loginError = document.getElementById('login-error');

  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();

      const email = document.getElementById('email').value.trim();
      const password = document.getElementById('password').value;

      loginError.classList.add('hidden');

      try {
        await login(email, password);
        window.location.href = 'dashboard.html';
      } catch (error) {
        loginError.textContent = error.message;
        loginError.classList.remove('hidden');
      }
    });
  }
});
