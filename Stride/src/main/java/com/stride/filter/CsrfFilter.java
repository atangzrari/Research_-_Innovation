package com.stride.filter;

import com.stride.security.CsrfTokens;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Every state-changing request (POST) must carry the session's anti-CSRF token (Finding F8).
 * The sign-in form checks its own token so it can show a friendly "page expired" message.
 */
public class CsrfFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())
                && !"/login".equals(AuthenticationFilter.pathOf(request))) {
            request.setCharacterEncoding("UTF-8");
            if (!CsrfTokens.isValid(request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
