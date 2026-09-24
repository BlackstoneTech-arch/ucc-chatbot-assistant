(function () {
  "use strict";

  const API_BASE = (typeof API_BASE_URL !== "undefined" && API_BASE_URL) ? API_BASE_URL : "";

  function flash(msg, ok) {
    const area = document.getElementById("flash-area");
    if (!area) return;
    const el = document.createElement("div");
    el.className = "flash " + (ok ? "flash-ok" : "flash-err");
    el.textContent = msg;
    area.appendChild(el);
    setTimeout(function () { el.remove(); }, 4000);
  }

  async function api(path) {
    const data = await AuthService.apiFetch(path);
    return data;
  }

  function renderStats(stats) {
    const grid = document.getElementById("stats-grid");
    if (!grid || !stats) return;
    const items = [
      { label: "Users", value: stats.totalUsers || 0 },
      { label: "Conversations", value: stats.totalConversations || 0 },
      { label: "Messages", value: stats.totalMessages || 0 },
      { label: "Feedbacks", value: stats.totalFeedbacks || 0 },
      { label: "Documents", value: stats.totalDocuments || 0 }
    ];
    grid.innerHTML = items.map(function (item) {
      return '<div class="stat-card"><div class="num">' + esc(item.value) + '</div><div class="label">' + esc(item.label) + '</div></div>';
    }).join("");
  }

  function renderActivity(activities) {
    const feed = document.getElementById("activity-feed");
    if (!feed) return;
    if (!Array.isArray(activities) || !activities.length) {
      feed.innerHTML = '<div class="panel empty">No recent activity.</div>';
      return;
    }
    feed.innerHTML = '<ul style="list-style:none;padding:0;margin:0">' + activities.slice(0, 20).map(function (a) {
      const time = a.createdAt ? new Date(a.createdAt).toLocaleString() : "";
      return '<li style="padding:10px 0;border-bottom:1px solid #e2e8f0">' +
        '<div style="font-size:.88rem;color:#334155">' + esc(a.action || "Activity") + '</div>' +
        '<div style="font-size:.78rem;color:#64748b">' + esc(time) + (a.userEmail ? ' — ' + esc(a.userEmail) : "") + '</div></li>';
    }).join("") + "</ul>";
  }

  function esc(s) {
    return (s == null ? "" : String(s)).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  async function loadDashboard() {
    try {
      const data = await api("/admin/dashboard");
      if (!data) { flash("Unable to load dashboard.", false); return; }
      renderStats(data.stats || data);
      renderActivity(data.activities || data.recentActivity || []);
    } catch (e) {
      flash(e.message || "Failed to load dashboard.", false);
    }
  }

  function initTabs() {
    document.querySelectorAll(".admin-sidebar a[data-tab]").forEach(function (a) {
      a.addEventListener("click", function (e) {
        e.preventDefault();
        const tab = a.getAttribute("data-tab");
        document.querySelectorAll(".tab-content").forEach(function (t) { t.classList.add("hidden"); });
        const target = document.getElementById("tab-" + tab);
        if (target) target.classList.remove("hidden");
        document.querySelectorAll(".admin-sidebar a").forEach(function (x) { x.classList.remove("active"); });
        a.classList.add("active");
        const title = document.getElementById("page-title");
        if (title) title.textContent = a.textContent.trim();
      });
    });
  }

  function initLogout() {
    const logout = function () {
      AuthService.logout().then(function () {
        window.location.href = "../admin-login.html";
      }).catch(function () {
        window.location.href = "../admin-login.html";
      });
    };
    var sidebar = document.getElementById("sidebar-logout");
    if (sidebar) sidebar.addEventListener("click", function (e) { e.preventDefault(); logout(); });
    var header = document.getElementById("header-logout");
    if (header) header.addEventListener("click", logout);
  }

  async function init() {
    if (!window.AuthService || !(await window.AuthService.requireAdmin("../admin-login.html"))) return;
    initTabs();
    initLogout();
    await loadDashboard();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
