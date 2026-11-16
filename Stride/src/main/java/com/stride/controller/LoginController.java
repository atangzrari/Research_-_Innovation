package com.stride.controller;

import com.stride.config.App;
import com.stride.model.User;
import com.stride.security.CsrfTokens;
import com.stride.service.AccountService;
import com.stride.util.SessionKeys;
import com.stride.web.BaseController;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

/**
 * GET  /login: show the sign-in form.
 * POST /login: check CSRF token, throttles, credentials; on success start a fresh session.
 */
@WebServlet(name = "loginController", urlPatterns = "/login")
public class LoginController extends BaseController {

    private static final String VIEW = "/WEB-INF/views/login.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession existing = request.getSession(false);
        if (existing != null && existing.getAttribute(SessionKeys.USER) != null) {
            redirect(request, response, "/dashboard");
            return;
        }
        showForm(request, response, null, HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        App app = app();

        if (!CsrfTokens.isValid(request)) {
            showForm(request, response, "Your sign-in page expired. Try again.", HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String key = AccountService.normalise(email);
        String ip = clientIp(request);

        Duration locked = app.signInGuard.check(key, ip);
        if (!locked.isZero()) {
            long minutes = Math.max(1, (locked.getSeconds() + 59) / 60);
            response.setHeader("Retry-After", String.valueOf(Math.max(1, locked.getSeconds())));
            showForm(request, response, "Too many failed attempts. Try again in " + minutes
                    + (minutes == 1 ? " minute." : " minutes."), 429);
            return;
        }

        Optional<User> user = app.accounts.authenticate(email, password, ip);
        if (user.isEmpty()) {
            app.signInGuard.failure(key, ip);
            // One message for "no such email", "wrong password" and "account off" so emails can't be probed.
            showForm(request, response, "That email and password don't match. Check both and try again.",
                    HttpServletResponse.SC_OK);
            return;
        }

        app.signInGuard.success(key, ip);
        startSession(request, user.get());
        redirect(request, response, user.get().isMustChangePassword() ? "/account/password" : "/dashboard");
    }

    /** New session id on sign-in prevents session fixation. */
    static void startSession(HttpServletRequest request, User user) {
        HttpSession old = request.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        request.getSession(true).setAttribute(SessionKeys.USER, user);
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response,
                          String message, int status) throws ServletException, IOException {
        App app = app();
        response.setStatus(status);
        response.setHeader("Cache-Control", "no-store");
        response.setContentType("text/html;charset=UTF-8");
        request.setAttribute("csrfToken", CsrfTokens.token(request.getSession(true)));
        request.setAttribute("services", app.curriculum.sites());
        request.setAttribute("message", message);
        request.setAttribute("ssoEnabled", app.sso.isEnabled());
        request.setAttribute("ssoLabel", app.sso.getLabel());
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
