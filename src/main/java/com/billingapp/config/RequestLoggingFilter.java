package com.billingapp.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class RequestLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_CORRELATION_KEY = "CorrelationId";
    private static final String MDC_USER_KEY = "user";
    private static final String MDC_IP_KEY = "ip";
    
    private static final DateTimeFormatter ISO_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of("Asia/Kolkata"));

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        
        boolean isHealthCheck = uri.equals("/api/public/health");

        // 1. Capture Client Real IP
        String clientIp = httpRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = httpRequest.getRemoteAddr();
        } else {
            clientIp = clientIp.split(",")[0].trim();
        }

        // 2. Capture authenticated User Context
        String username = "ANONYMOUS";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            username = auth.getName(); 
        }

        // 3. Extract or Generate universal Correlation ID Trace Token
        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // 4. Inject variables into MDC context threads
        MDC.put(MDC_USER_KEY, username);
        MDC.put(MDC_IP_KEY, clientIp);
        MDC.put(MDC_CORRELATION_KEY, correlationId);

        // 5. Attach tracer to outbound header response packet
        httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

        long startTime = System.currentTimeMillis();
        String timestamp = ISO_FORMATTER.format(Instant.now());

        if (!isHealthCheck) {
            logger.info("[{}] >>> REQUEST  | User: {} | IP: {} | {} {}", 
                    timestamp, username, clientIp, method, uri);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = httpResponse.getStatus();
            String endTimestamp = ISO_FORMATTER.format(Instant.now());

            if (status >= 400) {
                logger.error("[{}] <<< ERROR     | User: {} | IP: {} | {} {} | Status: {} | Execution: {}ms", 
                        endTimestamp, username, clientIp, method, uri, status, duration);
            } else if (!isHealthCheck) {
                logger.info("[{}] <<< RESPONSE  | User: {} | IP: {} | {} {} | Status: {} | Execution: {}ms", 
                        endTimestamp, username, clientIp, method, uri, status, duration);
            } else if (duration > 1500) {
                logger.warn("[{}] <<< SLOW PING | User: {} | IP: {} | {} {} | Status: {} | Execution: {}ms", 
                        endTimestamp, username, clientIp, method, uri, status, duration);
            }

            // 6. Clear MDC completely to protect the container thread pool
            MDC.clear();
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void destroy() {}
}