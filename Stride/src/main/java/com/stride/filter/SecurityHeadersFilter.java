package com.stride.filter;

import com.stride.config.App;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Adds the standard browser-side protections to every response (OWASP secure-headers set, F9). */
public class SecurityHeadersFilter implements Filter {

    // Only our own scripts and styles, plus the Google Fonts stylesheet and font files (Raleway).
    private static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' https://fonts.googleapis.com",
            "font-src https://fonts.gstatic.com",
            "img-src 'self' data:",
            "connect-src 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'",
            "base-uri 'self'",
            "object-src 'none'");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        response.setHeader("Content-Security-Policy", CSP);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        // Location is allowed for this site only, for the optional check-in location factor.
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(self)");
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        App app = (App) ((HttpServletRequest) req).getServletContext().getAttribute(App.CONTEXT_KEY);
        if (app != null && app.secureCookies) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        chain.doFilter(req, res);
    }
}
