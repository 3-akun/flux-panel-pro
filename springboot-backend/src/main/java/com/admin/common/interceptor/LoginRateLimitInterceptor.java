package com.admin.common.interceptor;

import com.alibaba.fastjson2.JSON;
import com.admin.common.lang.R;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 登录接口简易限流：同一 IP 5 分钟内最多 10 次失败尝试。
 */
@Component
public class LoginRateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_FAILURES = 10;
    private static final long WINDOW_MS = 5 * 60 * 1000L;

    private final ConcurrentHashMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String ip = resolveClientIp(request);
        AttemptWindow window = attempts.computeIfAbsent(ip, key -> new AttemptWindow());

        synchronized (window) {
            window.refreshIfExpired();
            if (window.failures.get() >= MAX_FAILURES) {
                writeTooManyRequests(response);
                return false;
            }
        }
        return true;
    }

    public void recordFailure(HttpServletRequest request) {
        String ip = resolveClientIp(request);
        AttemptWindow window = attempts.computeIfAbsent(ip, key -> new AttemptWindow());
        synchronized (window) {
            window.refreshIfExpired();
            window.failures.incrementAndGet();
        }
    }

    public void reset(HttpServletRequest request) {
        attempts.remove(resolveClientIp(request));
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JSON.toJSONString(R.err(429, "登录尝试过于频繁，请稍后再试")));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class AttemptWindow {
        private long windowStart = System.currentTimeMillis();
        private final AtomicInteger failures = new AtomicInteger();

        void refreshIfExpired() {
            if (System.currentTimeMillis() - windowStart > WINDOW_MS) {
                windowStart = System.currentTimeMillis();
                failures.set(0);
            }
        }
    }
}
