package com.tanit.cto.user_management;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RateLimitingFilter
 * 
 * - Signup -> per IP (max 5/hour)
 * - Login -> per user (max 5/minute) AND per IP (max 20/10 minutes)
 * - Global -> per IP (max 100/minute)
 * 
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public RateLimitingFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    private final Map<String, RateLimitCounter> ipCounters = new ConcurrentHashMap<>();
    private final Map<String, RateLimitCounter> userCounters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            jakarta.servlet.FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = request.getRemoteAddr();

        HttpServletRequest requestToUse = request;
        String username = null;

        // If JSON body on signup/login, read body bytes and create a replayable wrapper
        if ((path.equals("/signup") || path.equals("/login"))
                && request.getContentType() != null
                && request.getContentType().toLowerCase().contains("application/json")) {

            // read raw bytes from original request
            byte[] bodyBytes = StreamUtils.copyToByteArray(request.getInputStream());

            // try parse email from body
            if (bodyBytes != null && bodyBytes.length > 0) {
                try {
                    String body = new String(bodyBytes, request.getCharacterEncoding() != null
                            ? request.getCharacterEncoding()
                            : StandardCharsets.UTF_8.name());
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> jsonMap = mapper.readValue(body, Map.class);
                    Object e = jsonMap.get("email");
                    if (e != null)
                        username = e.toString();
                } catch (Exception ignored) {
                }
            }

            // wrap request so controller still receives the same body
            requestToUse = new CachedBodyHttpServletRequest(request, bodyBytes);
        } else {
            username = request.getParameter("email");
        }

        // If still null, try JWT extraction
        if (username == null) {
            String authHeader = requestToUse.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    username = jwtUtil.extractEmail(token);
                } catch (Exception ignored) {
                }
            }
        }

        // 1. Global per-IP limit (100 requests/minute)
        if (isRateLimited(ipCounters, clientIp, 100, 60)) {
            block(response, "Too many requests from IP (global limit)");
            return;
        }

        // 2. Signup limit (per IP 5/hour)
        if (path.equals("/signup")) {
            if (isRateLimited(ipCounters, "signup:" + clientIp, 5, 3600)) {
                block(response, "Too many signups from this IP (limit 5/hour)");
                return;
            }
        }

        // 3. Login limits
        else if (path.equals("/login")) {
            if (username != null) {
                if (isRateLimited(userCounters, "login:user:" + username, 5, 60)) {
                    block(response, "Too many login attempts for this user (limit 5/minute)");
                    return;
                }
            }
            if (isRateLimited(ipCounters, "login:ip:" + clientIp, 20, 600)) {
                block(response, "Too many login attempts from this IP (limit 20/10 minutes)");
                return;
            }
        }

        filterChain.doFilter(requestToUse, response);
    }

    private boolean isRateLimited(Map<String, RateLimitCounter> store,
            String key,
            int limit,
            int windowSeconds) {
        long now = Instant.now().getEpochSecond();
        store.putIfAbsent(key, new RateLimitCounter(0, now));

        RateLimitCounter counter = store.get(key);

        synchronized (counter) {
            if (now - counter.timestamp >= windowSeconds) {
                counter.count = 1;
                counter.timestamp = now;
                return false;
            } else {
                if (counter.count >= limit) {
                    return true;
                } else {
                    counter.count++;
                    return false;
                }
            }
        }
    }

    private void block(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.getWriter().write(message);
    }

    private static class RateLimitCounter {
        int count;
        long timestamp;

        RateLimitCounter(int count, long timestamp) {
            this.count = count;
            this.timestamp = timestamp;
        }
    }

    /**
     * Wrapper that replays the cached body to downstream consumers.
     */
    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request, byte[] cachedBody) {
            super(request);
            this.cachedBody = (cachedBody == null) ? new byte[0] : cachedBody;
        }

        @Override
        public ServletInputStream getInputStream() {
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }

                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            String enc = getCharacterEncoding() != null ? getCharacterEncoding() : StandardCharsets.UTF_8.name();
            return new BufferedReader(new InputStreamReader(getInputStream(), java.nio.charset.Charset.forName(enc)));
        }
    }
}
