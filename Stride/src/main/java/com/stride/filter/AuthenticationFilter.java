package com.stride.filter;

import com.stride.config.App;
import com.stride.model.User;
import com.stride.util.SessionKeys;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Optional;

/**
 * The single guard in front of every page (RQ04). Mapped to /* in web.xml.
 * Public: the sign-in page, single sign-on, static assets. Everything else needs a signed-in,
 * still-active account. The account is re-read on each request, so a deactivated account or a
 * changed role takes effect at once rather than at the next sign-in.
 */
public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = pathOf(request);
        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }
        HttpSession session = request.getSession(false);
        User sessionUser = session == null ? null : (User) session.getAttribute(SessionKeys.USER);
        if (path.equals("/") || path.isEmpty()) {
            response.sendRedirect(request.getContextPath() + (sessionUser != null ? "/dashboard" : "/login"));
            return;
        }
        if (sessionUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        App app = (App) request.getServletContext().getAttribute(App.CONTEXT_KEY);
        Optional<User> fresh = app.accounts.find(sessionUser.getUserId());
        if (fresh.isEmpty() || !fresh.get().isActive()) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        session.setAttribute(SessionKeys.USER, fresh.get());
        // After an administrator reset, the person must choose their own password first.
        if (fresh.get().isMustChangePassword() && !path.equals("/account/password") && !path.equals("/logout")) {
            response.sendRedirect(request.getContextPath() + "/account/password");
            return;
        }
        // Signed-in pages must not be cached, so Back after sign-out shows nothing private.
        response.setHeader("Cache-Control", "no-store");
        chain.doFilter(request, response);
    }

    static boolean isPublic(String path) {
        return path.equals("/login") || path.startsWith("/assets/") || path.equals("/sso/start")
                || path.equals("/sso/callback") || path.equals("/favicon.ico");
    }

    /** Request path without the context path or a ;jsessionid parameter. */
    static String pathOf(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        int semicolon = path.indexOf(';');
        return semicolon >= 0 ? path.substring(0, semicolon) : path;
    }
}
