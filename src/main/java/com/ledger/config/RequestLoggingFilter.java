package com.ledger.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Request logging filter that adds correlation ID to MDC and logs requests/responses.
 */
@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String correlationId = getOrCreateCorrelationId(request);

        MDC.put(CORRELATION_ID_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            logRequest(request, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logResponse(response, duration, correlationId);
            MDC.remove(CORRELATION_ID_KEY);
        }
    }

    private String getOrCreateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private void logRequest(HttpServletRequest request, String correlationId) {
        log.info("REQUEST: {} {} correlationId={}", request.getMethod(), request.getRequestURI(), correlationId);
    }

    private void logResponse(HttpServletResponse response, long duration, String correlationId) {
        log.info("RESPONSE: {} {}ms correlationId={}", response.getStatus(), duration, correlationId);
    }
}
