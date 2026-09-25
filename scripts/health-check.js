#!/usr/bin/env node
/**
 * UCC Chatbot System Health Check
 *
 * Usage:
 *   node scripts/health-check.js [backendUrl]
 *
 * If no backendUrl is provided, it will try to detect it from:
 *   1. Command line argument
 *   2. API_BASE_URL environment variable
 *   3. Default to http://localhost:8080/api
 */

const http = require("http");
const https = require("https");

const args = process.argv.slice(2);
const explicitBackend = args[0];
const envBackend = process.env.API_BASE_URL || process.env.BACKEND_URL || process.env.API_URL;

function getBaseUrl() {
  if (explicitBackend) return explicitBackend.replace(/\/$/, "");
  if (envBackend) return envBackend.replace(/\/$/, "");
  return "http://localhost:8080/api";
}

function request(url) {
  return new Promise((resolve, reject) => {
    const lib = url.startsWith("https") ? https : http;
    const req = lib.get(url, { timeout: 5000 }, (res) => {
      let data = "";
      res.on("data", (chunk) => (data += chunk));
      res.on("end", () => resolve({ status: res.statusCode, body: data }));
    });
    req.on("error", reject);
    req.on("timeout", () => {
      req.destroy();
      reject(new Error("timeout"));
    });
  });
}

async function checkHealth(baseUrl) {
  const results = {
    backend: "FAIL",
    database: "FAIL",
    ai: "FAIL",
    retrieval: "FAIL",
    apiUrl: baseUrl.replace(/\/api$/, ""),
    details: {}
  };

  try {
    const healthRes = await request(`${baseUrl}/health`);
    if (healthRes.status === 200) {
      results.backend = "PASS";
      try {
        const payload = JSON.parse(healthRes.body);
        results.details.backend = payload;
        results.database = payload.database === "UP" ? "PASS" : "FAIL";
        results.ai = (payload.aiService && payload.aiService.status === "UP") ? "PASS" : "FAIL";
        results.retrieval = (payload.retrieval && payload.retrieval.status === "UP") ? "PASS" : "FAIL";
      } catch (_) {
        results.details.backend = healthRes.body;
      }
    }
  } catch (err) {
    results.details.backend = String(err.message || err);
  }

  return results;
}

async function main() {
  const baseUrl = getBaseUrl();
  console.log(`UCC CHATBOT SYSTEM HEALTH`);
  console.log(`API: ${baseUrl}`);

  const results = await checkHealth(baseUrl);
  console.log(`Backend:        ${results.backend}`);
  console.log(`Database:       ${results.database}`);
  console.log(`AI:             ${results.ai}`);
  console.log(`Retrieval:      ${results.retrieval}`);

  if (results.details.backend) {
    console.log(`Details:`);
    console.log(JSON.stringify(results.details.backend, null, 2));
  }

  const allPass = [results.backend, results.database, results.ai, results.retrieval].every((v) => v === "PASS");
  process.exit(allPass ? 0 : 1);
}

main();
