/* ============================================
   UCC Chatbot - Centralized Auth Service
   HttpOnly session cookies are the source of truth.
   The browser never persists access or refresh tokens.
   ============================================ */

(function (global) {
  "use strict";

  const SESSION_KEY = "ucc_chat_session";
  let currentUser = null;
  let csrfToken = null;
  let refreshPromise = null;

  function apiBase() {
    return (typeof API_BASE_URL !== "undefined" && API_BASE_URL) ? API_BASE_URL : "";
  }

  function isPublicAuthEndpoint(endpoint) {
    return endpoint === "/auth/login" || endpoint === "/auth/refresh" || endpoint === "/auth/logout";
  }

  function setUser(user) {
    currentUser = user && typeof user === "object" ? user : null;
  }

  function getUser() {
    return currentUser;
  }

  function getSessionId() {
    try {
      let id = sessionStorage.getItem(SESSION_KEY);
      if (!id) {
        id = (global.crypto && global.crypto.randomUUID)
          ? global.crypto.randomUUID()
          : "sess-" + Date.now() + "-" + Math.random().toString(36).slice(2);
        sessionStorage.setItem(SESSION_KEY, id);
      }
      return id;
    } catch (_) {
      return "sess-" + Date.now() + "-" + Math.random().toString(36).slice(2);
    }
  }

  function clearMemorySession() {
    currentUser = null;
    csrfToken = null;
    refreshPromise = null;
  }

  async function parseResponse(response) {
    const text = await response.text().catch(function () { return ""; });
    let data = {};
    if (text) {
      try { data = JSON.parse(text); } catch (_) { data = {}; }
    }
    if (!response.ok) {
      const error = new Error(data.message || "Unable to complete the request");
      error.status = response.status;
      error.code = data.error && data.error.code;
      throw error;
    }
    return data;
  }

  async function request(endpoint, options, allowRefresh) {
    options = options || {};
    const method = (options.method || "GET").toUpperCase();
    const headers = Object.assign({ "Content-Type": "application/json" }, options.headers || {});
    if (csrfToken && !["GET", "HEAD", "OPTIONS"].includes(method)) {
      headers["X-XSRF-TOKEN"] = csrfToken;
    }
    const response = await fetch(apiBase() + endpoint, {
      method: method,
      headers: headers,
      body: options.body,
      credentials: "include",
      signal: options.signal
    });
    if (response.status === 401 && allowRefresh !== false && !isPublicAuthEndpoint(endpoint)) {
      try {
        await refresh();
        return request(endpoint, options, false);
      } catch (_) {
        clearMemorySession();
      }
    }
    return parseResponse(response);
  }

  async function loadCsrf() {
    if (csrfToken) return csrfToken;
    const data = await request("/auth/csrf", { method: "GET" }, false);
    csrfToken = data.token || null;
    return csrfToken;
  }

  async function apiFetch(endpoint, options) {
    options = options || {};
    const method = (options.method || "GET").toUpperCase();
    if (!["GET", "HEAD", "OPTIONS"].includes(method) && !isPublicAuthEndpoint(endpoint)) {
      await loadCsrf();
    }
    return request(endpoint, options, true);
  }

  async function login(email, password) {
    const data = await request("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email: email, password: password })
    }, false);
    setUser(data.user);
    return data;
  }

  async function registerVisitor(fullName, email, password, confirmPassword) {
    return request("/auth/visitor-register", {
      method: "POST",
      body: JSON.stringify({
        fullName: fullName,
        email: email,
        password: password,
        confirmPassword: confirmPassword
      })
    }, false);
  }

  async function registerStudent(fullName, email, registrationNumber, password, confirmPassword) {
    return request("/auth/student/register", {
      method: "POST",
      body: JSON.stringify({
        fullName: fullName,
        email: email,
        registrationNumber: registrationNumber,
        password: password,
        confirmPassword: confirmPassword
      })
    }, false);
  }

  async function studentStep1(email, password) {
    return request("/auth/student/verify-email", {
      method: "POST",
      body: JSON.stringify({ email: email, password: password })
    }, false);
  }

  async function studentStep2(challengeToken, registrationNumber, password) {
    const data = await request("/auth/student/verify-registration", {
      method: "POST",
      body: JSON.stringify({
        challengeToken: challengeToken,
        registrationNumber: registrationNumber,
        password: password
      })
    }, false);
    setUser(data.user);
    return data;
  }

  async function me() {
    const data = await request("/auth/me", { method: "GET" }, true);
    setUser(data.user);
    return data;
  }

  async function refresh() {
    if (refreshPromise) return refreshPromise;
    refreshPromise = request("/auth/refresh", { method: "POST", body: "{}" }, false)
      .then(function (data) {
        setUser(data.user);
        return data;
      })
      .catch(function (error) {
        clearMemorySession();
        throw error;
      })
      .finally(function () { refreshPromise = null; });
    return refreshPromise;
  }

  async function logout() {
    try {
      await request("/auth/logout", { method: "POST", body: "{}" }, false);
    } finally {
      clearMemorySession();
    }
  }

  async function linkConversation(sessionId) {
    if (!currentUser) return null;
    return apiFetch("/auth/link-conversation", {
      method: "POST",
      body: JSON.stringify({ sessionId: sessionId || getSessionId() })
    });
  }

  function isAuthenticated() {
    return !!currentUser;
  }

  function hasRole(role) {
    const actual = (currentUser && currentUser.role ? currentUser.role : "").toUpperCase();
    if (!role) return !!actual;
    if (Array.isArray(role)) return role.map(function (item) { return item.toUpperCase(); }).includes(actual);
    return actual === role.toUpperCase();
  }

  function requireAuth(redirect) {
    if (!isAuthenticated()) {
      global.location.href = redirect || "/login";
      return false;
    }
    return true;
  }

  async function requireAdmin(redirect) {
    try {
      if (!currentUser) await me();
    } catch (_) {
      global.location.href = redirect || "/login";
      return false;
    }
    if (!hasRole(["ADMIN", "SUPER_ADMIN", "SUPERADMIN"])) {
      global.location.href = "/chat.html";
      return false;
    }
    return true;
  }

  global.AuthService = {
    getUser: getUser,
    getSessionId: getSessionId,
    isAuthenticated: isAuthenticated,
    hasRole: hasRole,
    authHeaders: function () { return { "Content-Type": "application/json" }; },
    apiFetch: apiFetch,
    login: login,
    registerVisitor: registerVisitor,
    registerStudent: registerStudent,
    studentStep1: studentStep1,
    studentStep2: studentStep2,
    me: me,
    refresh: refresh,
    logout: logout,
    linkConversation: linkConversation,
    requireAuth: requireAuth,
    requireAdmin: requireAdmin,
    clearSession: clearMemorySession
  };
})(window);