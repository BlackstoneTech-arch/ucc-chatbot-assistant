export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "access-control-allow-origin": "*",
          "access-control-allow-methods": "GET,POST,PUT,PATCH,DELETE,OPTIONS",
          "access-control-allow-headers": "authorization,content-type,accept,x-xsrf-token",
          "access-control-max-age": "3600"
        }
      });
    }

    if (!env.BACKEND_URL) {
      return new Response("Missing BACKEND_URL env", { status: 500 });
    }

    const backendBase = env.BACKEND_URL.replace(/\/$/, "");

    if (url.pathname.startsWith("/api/")) {
      const backendUrl = backendBase + url.pathname.replace(/^\/api/, "/api") + url.search;
      const backendRequest = new Request(backendUrl, {
        method: request.method,
        headers: request.headers,
        body: request.body,
        redirect: request.redirect
      });

      const backendResponse = await fetch(backendRequest, {
        cf: { pol: "smart" }
      });

      const responseHeaders = new Headers(backendResponse.headers);
      responseHeaders.set("access-control-allow-origin", "*");

      return new Response(backendResponse.body, {
        status: backendResponse.status,
        statusText: backendResponse.statusText,
        headers: responseHeaders
      });
    }

    return env.ASSETS.fetch(request);
  }
};
