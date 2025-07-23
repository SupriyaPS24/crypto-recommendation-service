package com.xm.cryptoservice.config;

import com.xm.cryptoservice.config.Bucket4jFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.io.IOException;

import static org.mockito.Mockito.*;

class Bucket4jFilterTest {

    Bucket4jFilter filter;

    @BeforeEach
    void setup() {
        filter = new Bucket4jFilter();
    }

    @Test
    void doFilter_allowsRequest() throws IOException, ServletException {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        // Consume 1 token, bucket initially full
        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        verify(res, never()).sendError(anyInt(), anyString());
    }

    @Test
    void doFilter_blocksRequest() throws IOException, ServletException {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        // Consume 100 tokens to exhaust bucket
        for (int i = 0; i < 100; i++) {
            filter.doFilter(req, res, chain);
        }

        filter.doFilter(req, res, chain);

        verify(res).sendError(429, "Too Many Requests");
        verify(chain, times(100)).doFilter(req, res);
    }
}
