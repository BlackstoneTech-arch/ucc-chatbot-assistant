#!/usr/bin/env node
/**
 * UCC Chatbot API Test
 *
 * Usage:
 *   node scripts/api-test.js [backendUrl] [adminEmail] [adminPassword]
 */

const http = require("http");
const https = require("https");

const args = process.argv.slice(2);
const explicitBackend = args[0];
const adminEmail = args[1] || "admin@ucc.co.tz";
const adminPassword = args[2] || "UCC@Admin2026!Secure";

function getBaseUrl() {
  if (explicitBackend) return explicitBackend.replace(/\/$/, "");
  const env = process.env.API_BASE_URL || process.env.BACKEND_URL || process.env.API_URL;
  if (env) return env.replace(/\/$/, "");
  return "http://localhost:8080/api";
}

function request(url, options = {}) {
  return new Promise((resolve, reject) => {
    const lib = url.startsWith("https") ? https : http;
    const req = lib.request(url, options, (res) => {
      let data = "";
      res.on("data", (chunk) => (data += chunk));
      res.on("end", () => resolve({ status: res.statusCode, body: data, headers: res.headers }));
    });
    req.on("error", reject);
    if (options.timeout) req.setTimeout(options.timeout, () => {
      req.destroy();
      reject(new Error("timeout"));
    });
    if (options.body) req.write(options.body);
    req.end();
  });
}

async function test(baseUrl) {
  const results = { baseUrl, tests: [] };

  async function run(name, fn) {
    try {
      const result = await fn();
      results.tests.push({ name, status: result.ok ? "PASS" : "FAIL", detail: result.detail || "", code: result.code });
    } catch (err) {
      results.tests.push({ name, status: "FAIL", detail: String(err.message || err), code: "ERROR" });
    }
  }

  await run("health", async () => {
    const res = await request(`${baseUrl}/health`, { timeout: 5000 });
    const ok = res.status === 200;
    return { ok, code: res.status, detail: ok ? res.body : res.body };
  });

  await run("login", async () => {
    const body = JSON.stringify({ email: adminEmail, password: adminPassword });
    const res = await request(`${baseUrl}/auth/login`, {
      method: "POST",
      timeout: 10000,
      headers: { "Content-Type": "application/json" },
      body
    });
    const ok = res.status === 200;
    let data = {};
    try { data = JSON.parse(res.body); } catch (_) {}
    return { ok, code: res.status, detail: ok ? `role=${data.user && data.user.role}` : res.body };
  });

  await run("chat-contract", async () => {
    const body = JSON.stringify({ message: "What programmes does UCC offer?", conversationId: "test", language: "en" });
    const res = await request(`${baseUrl}/chat`, {
      method: "POST",
      timeout: 15000,
      headers: { "Content-Type": "application/json" },
      body
    });
    const ok = res.status === 200;
    let data = {};
    try { data = JSON.parse(res.body); } catch (_) {}
    return { ok, code: res.status, detail: ok ? `confidence=${data.confidence}` : res.body };
  });

  results.tests.forEach((t) => {
    console.log(`${t.status === "PASS" ? "✔" : "✘"} ${t.name}: ${t.status} (${t.code})${t.detail ? " - " + t.detail : ""}`);
  });

  const pass = results.tests.every((t) => t.status === "PASS");
  console.log(`\nAPI: ${baseUrl}`);
  console.log(`Result: ${pass ? "PASS" : "FAIL"}`);
  return pass;
}

main().catch((err) => {
  console.error("Health check failed:", err);
  process.exit(1);
});

async function main() {
  const baseUrl = getBaseUrl();
  const ok = await test(baseUrl);
  process.exit(ok ? 0 : 1);
}
