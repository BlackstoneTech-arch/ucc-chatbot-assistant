package com.ucc.chatbot.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory token-bucket rate limiter.
 * - /api/chat: 30 req/min per IP (anonymous public endpoint)
 * - /api/auth/login: 10 req/min per IP (brute-force protection)
 * - /api/chat/feedback: 20 req/min per IP
 *
 * For production at scale, replace with Bucket4j + Redis, or with
 * an upstream WAF (Cloudflare, Netlify Edge, etc.).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        Integer limit = limitFor(path, request.getMethod());
        if (limit == null) {
            chain.doFilter(request, response);
            return;
        }

        String ip = clientIp(request);
        String key = path + "|" + ip;
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(WINDOW_MS, limit));
        if (!bucket.tryConsume()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Too many requests. Please slow down and try again in a minute.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private Integer limitFor(String path, String method) {
        if (!"POST".equalsIgnoreCase(method) && !"GET".equalsIgnoreCase(method)) return null;
        if (path.equals("/api/chat")) return 30;
        if (path.equals("/api/chat/welcome")) return 60;
        if (path.equals("/api/chat/feedback")) return 20;
        if (path.equals("/api/auth/login")) return 10;
        if (path.equals("/api/auth/register")) return 5;
        return null;
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return req.getRemoteAddr();
    }

    private static final class Bucket {
        private final long windowMs;
        private final int capacity;
        private long windowStart;
        private final AtomicInteger count = new AtomicInteger(0);

        Bucket(long windowMs, int capacity) {
            this.windowMs = windowMs;
            this.capacity = capacity;
            this.windowStart = System.currentTimeMillis();
        }

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStart >= windowMs) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= capacity;
        }
    }
}
