package com.eventos.registration_service.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.slf4j.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            // Captura o token para auditoria (opcional, mas bom para segurança)
            String auth = request.getHeader("Authorization");
            
            logger.info("REQ START - method={} path={} query={} auth={}",
                    request.getMethod(), request.getRequestURI(), request.getQueryString(), auth);
            
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            logger.info("REQ END - path={} status={} durationMs={}",
                    request.getRequestURI(), response.getStatus(), duration);
        }
    }
}