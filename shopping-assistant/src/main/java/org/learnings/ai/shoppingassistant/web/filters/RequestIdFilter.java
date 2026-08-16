package org.learnings.ai.shoppingassistant.web.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestId = getOrGenerateRequestId(request);
            log.info("Request ID: [{}]", requestId);

            MDC.put(MDC_KEY, requestId);

            response.setHeader(REQUEST_ID_HEADER, requestId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String getOrGenerateRequestId(HttpServletRequest request) {
        try {
            String requestId = request.getHeader(REQUEST_ID_HEADER);

            return UUID.fromString(requestId).toString();
        } catch (NullPointerException | IllegalArgumentException e) {
            log.debug("Failed to generate request id for [{}]", request.getHeader(REQUEST_ID_HEADER));

            return UUID.randomUUID().toString();
        }
    }
}
