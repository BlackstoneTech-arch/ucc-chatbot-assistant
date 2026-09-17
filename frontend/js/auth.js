/* ============================================
   UCC Chatbot - Centralized Auth Service
   Single source of truth for all authentication
   logic. Used by login.html, chat.html, and
   every admin page. No duplicate fetch logic.
   ============================================ */

(function (global) {
  "use strict";

  const AUTH_STORAGE = {
    TOKEN: "ucc_auth_token",
    REFRESH: "ucc_auth_refresh_token",
    ROLE: "ucc_auth_role",
    USER: "ucc_auth_user",
    SESSION: "ucc_chat_session"
  };

  function apiBase() {
    return (typeof API_BASE_URL !== "undefined" && API_BASE_URL) ? API_BASE_URL : "";
  }

  function storage() {
    try { return window.localStorage; } catch (_) { return null; }
  }

  function get(key) {
    const s = storage();
    return s ? s.getItem(key) : null;
  }

  function set(key, value) {
    const s = storage();
    if (s) s.setItem(key, value);
  }

  function remove(key) {
    const s = storage();
    if (s) s.removeItem(key);
  }

  function clearSession() {
    remove(AUTH_STORAGE.TOKEN);
    remove(AUTH_STORAGE.REFRESH);
    remove(AUTH_STORAGE.ROLE);
    remove(AUTH_STORAGE.USER);
  }

  function getToken() { return get(AUTH_STORAGE.TOKEN); }
  function getRefreshToken() { return get(AUTH_STORAGE.REFRESH); }
  function getRole() { return get(AUTH_STORAGE.ROLE); }
  function getUser() {
    try {
      const raw = get(AUTH_STORAGE.USER);
      return raw ? JSON.parse(raw) : null;
    } catch (_) { return null; }
  }

  function isAuthenticated() {
    return !!getToken() && !!getRole();
  }

  function hasRole(role) {
    const r = (getRole() || "").toUpperCase();
    if (!role) return !!r;
    if (Array.isArray(role)) return role.map(function (x) { return x.toUpperCase(); }).includes(r);
    return r === role.toUpperCase();
  }

  function requiresAdmin() {
    return hasRole(["ADMIN", "SUPERADMIN"]);
  }

  function requiresAuth() {
    return !!getToken();
  }

  function authHeaders(extra) {
    const t = getToken();
    const h = { "Content-Type": "application/json" };
    if (t) h.Authorization = "Bearer " + t;
    if (extra) Object.assign(h, extra);
    return h;
  }

  async function apiFetch(endpoint, options) {
    options = options || {};
    const base = apiBase();
    if (!base) throw new Error("Backend API not configured");
    const res = await fetch(base + endpoint, {
      headers: authHeaders(options.headers),
      ...options
    });
    if (!res.ok) {
      const txt = await res.text().catch(function () { return ""; });
      let msg = "Request failed";
      try { const j = JSON.parse(txt); if (j && j.message) msg = j.message; } catch (_) {}
      throw new Error(msg);
    }
    const txt = await res.text().catch(function () { return ""; });
    return txt ? JSON.parse(txt) : null;
  }

  async function login(email, password) {
    const data = await apiFetch("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email: email, password: password })
    });
    storeSession(data);
    return data;
  }

  async function adminLogin(email, password) {
    const data = await apiFetch("/auth/admin-login", {
      method: "POST",
      body: JSON.stringify({ email: email, password: password })
    });
    storeSession(data);
    return data;
  }

  async function registerVisitor(fullName, email, password) {
    return apiFetch("/auth/visitor-register", {
      method: "POST",
      body: JSON.stringify({ fullName: fullName, email: email, password: password })
    });
  }

  async function registerStudent(fullName, email, registrationNumber, password) {
    return apiFetch("/auth/student/register", {
      method: "POST",
      body: JSON.stringify({ fullName: fullName, email: email, registrationNumber: registrationNumber, password: password })
    });
  }

  async function studentStep1(email, password) {
    return apiFetch("/auth/student/verify-email", {
      method: "POST",
      body: JSON.stringify({ email: email, password: password })
    });
  }

  async function studentStep2(challengeToken, registrationNumber, password) {
    return apiFetch("/auth/student/verify-registration", {
      method: "POST",
      body: JSON.stringify({ challengeToken: challengeToken, registrationNumber: registrationNumber, password: password })
    });
  }

  async function me() {
    return apiFetch("/auth/me");
  }

  async function refresh() {
    const rt = getRefreshToken();
    if (!rt) throw new Error("No refresh token");
    const data = await apiFetch("/auth/refresh", {
      method: "POST",
      body: JSON.stringify({ refreshToken: rt })
    });
    if (data && data.token) set(AUTH_STORAGE.TOKEN, data.token);
    if (data && data.refreshToken) set(AUTH_STORAGE.REFRESH, data.refreshToken);
    scheduleRefresh();
    return data;
  }

  function logout() {
    const rt = getRefreshToken();
    clearSession();
    if (rt && apiBase()) {
      fetch(apiBase() + "/auth/logout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken: rt })
      }).catch(function () {});
    }
  }

  function storeSession(data) {
    if (!data) return;
    if (data.token) set(AUTH_STORAGE.TOKEN, data.token);
    if (data.refreshToken) set(AUTH_STORAGE.REFRESH, data.refreshToken);
    if (data.user) {
      set(AUTH_STORAGE.USER, JSON.stringify(data.user));
      if (data.user.role) set(AUTH_STORAGE.ROLE, data.user.role);
    }
    scheduleRefresh();
  }

  const REFRESH_GRACE_MS = 5 * 60 * 1000;
  let refreshTimer = null;

  function scheduleRefresh() {
    clearTimeout(refreshTimer);
    if (!getToken() || !getRefreshToken()) return;
    const delay = 7 * 24 * 60 * 60 * 1000 - REFRESH_GRACE_MS;
    refreshTimer = setTimeout(function () { refresh().catch(function () {}); }, Math.max(60000, delay));
  }

  async function linkConversation(sessionId) {
    if (!isAuthenticated()) return null;
    try {
      return await apiFetch("/auth/link-conversation", {
        method: "POST",
        body: JSON.stringify({ sessionId: sessionId || getSessionId() })
      });
    } catch (_) { return null; }
  }

  function getSessionId() {
    let sid = get(AUTH_STORAGE.SESSION);
    if (!sid) {
      sid = "sess_" + Math.random().toString(36).slice(2) + Date.now().toString(36);
      set(AUTH_STORAGE.SESSION, sid);
    }
    return sid;
  }

  function requireAuth(redirect) {
    if (!isAuthenticated()) {
      window.location.href = redirect || "login.html";
      return false;
    }
    return true;
  }

  function requireAdmin(redirect) {
    if (!isAuthenticated()) {
      window.location.href = redirect || "login.html";
      return false;
    }
    if (!requiresAdmin()) {
      window.location.href = redirect || "../index.html";
      return false;
    }
    return true;
  }

  const AuthService = {
    STORAGE: AUTH_STORAGE,
    getToken: getToken,
    getRefreshToken: getRefreshToken,
    getRole: getRole,
    getUser: getUser,
    isAuthenticated: isAuthenticated,
    hasRole: hasRole,
    requiresAdmin: requiresAdmin,
    requiresAuth: requiresAuth,
    authHeaders: authHeaders,
    apiFetch: apiFetch,
    login: login,
    adminLogin: adminLogin,
    registerVisitor: registerVisitor,
    registerStudent: registerStudent,
    studentStep1: studentStep1,
    studentStep2: studentStep2,
    me: me,
    refresh: refresh,
    logout: logout,
    storeSession: storeSession,
    scheduleRefresh: scheduleRefresh,
    linkConversation: linkConversation,
    getSessionId: getSessionId,
    requireAuth: requireAuth,
    requireAdmin: requireAdmin,
    clearSession: clearSession
  };

  global.AuthService = AuthService;
  global.UCCSession = AuthService;
})(window);
