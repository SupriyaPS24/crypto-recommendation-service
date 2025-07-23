package com.xm.cryptoservice.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Bucket4jFilter implements Filter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String ip = ((HttpServletRequest) req).getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(ip, this::newBucket);
        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            ((HttpServletResponse) res).sendError(429, "Too Many Requests");
        }
    }

    private Bucket newBucket(String ip) {
        return Bucket4j.builder()
                .addLimit(Bandwidth.simple(100, Duration.ofMinutes(1)))
                .build();
    }
}

