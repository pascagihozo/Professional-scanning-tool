package com.lci.scannerdesktop.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class AuthFilter implements Filter {

    @Value("${api.auth.shared-secret:}")
    private String sharedSecret;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (sharedSecret == null || sharedSecret.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Allow health without auth for easier diagnostics
        String path = req.getRequestURI();
        if (path != null && (path.endsWith("/v1/health") || path.equals("/v1/health"))) {
            chain.doFilter(request, response);
            return;
        }

        String header = req.getHeader("X-Scanner-Secret");
        if (header != null && header.equals(sharedSecret)) {
            chain.doFilter(request, response);
            return;
        }

        res.setStatus(401);
        res.setContentType("application/json");
        res.getWriter().write("{\"status\":\"ERROR\",\"message\":\"Unauthorized\"}");
    }
}


