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

@Component
public class RequestLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    
    // Professional ISO timestamp formatter
    private static final DateTimeFormatter ISO_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of("Asia/Kolkata"));

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        
        // Skip logging successful health pings to keep Render log streams clean
        boolean isHealthCheck = uri.equals("/api/public/health");

        // 1. Capture Client Real IP (handles Render/Nginx reverse proxy headers)
        String clientIp = httpRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = httpRequest.getRemoteAddr();
        } else {
            // X-Forwarded-For can contain a chain of IPs, take the first one
            clientIp = clientIp.split(",")[0].trim();
        }

        // 2. Capture authenticated User Context from Spring Security
        String username = "ANONYMOUS";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            username = auth.getName(); 
        }

        // 3. Inject variables into MDC (Makes them available automatically to your log layout config)
        MDC.put("user", username);
        MDC.put("ip", clientIp);

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
                // Keep track of slow warm-up pings
                logger.warn("[{}] <<< SLOW PING | User: {} | IP: {} | {} {} | Status: {} | Execution: {}ms", 
                        endTimestamp, username, clientIp, method, uri, status, duration);
            }

            // 4. Always clear MDC context threads to prevent memory leaks
            MDC.clear();
        }
    }
}