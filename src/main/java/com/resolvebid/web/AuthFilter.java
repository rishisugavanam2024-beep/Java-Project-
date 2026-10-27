package com.resolvebid.web;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class AuthFilter implements Filter {
    @Override public void doFilter(ServletRequest rawRequest, ServletResponse rawResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) rawRequest;
        HttpServletResponse response = (HttpServletResponse) rawResponse;
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        response.setHeader("Content-Security-Policy", "default-src 'self'; style-src 'self'; img-src 'self' data:; form-action 'self'; frame-ancestors 'none'; base-uri 'self'");
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean protectedPath = path.startsWith("/app/") || path.startsWith("/api/");
        if (protectedPath) response.setHeader("Cache-Control", "no-store");
        if (protectedPath && WebSupport.user(request) == null) {
            if (path.startsWith("/api/")) response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            else WebSupport.redirect(request, response, "/login");
            return;
        }
        chain.doFilter(request, response);
    }
}
