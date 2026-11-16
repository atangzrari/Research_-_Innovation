package com.stride.controller;

import com.stride.config.App;
import com.stride.model.User;
import com.stride.security.OidcClient;
import com.stride.util.SessionKeys;
import com.stride.web.BaseController;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

/** Single sign-on with the college identity provider (RQ27): /sso/start and /sso/callback. */
@WebServlet(name = "ssoController", urlPatterns = {"/sso/start", "/sso/callback"})
public class SsoController extends BaseController {

    private static final Logger LOG = Logger.getLogger(SsoController.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        App app = app();
        if (!app.sso.isEnabled()) {
            notFound(response);
            return;
        }
        if ("/sso/start".equals(request.getServletPath())) {
            OidcClient.Pending pending = app.sso.newPending();
            request.getSession(true).setAttribute(SessionKeys.SSO, pending);
            response.sendRedirect(app.sso.authorizeRedirect(pending));
            return;
        }
        HttpSession session = request.getSession(false);
        OidcClient.Pending pending = session == null ? null : (OidcClient.Pending) session.getAttribute(SessionKeys.SSO);
        if (session != null) {
            session.removeAttribute(SessionKeys.SSO);
        }
        if (request.getParameter("error") != null) {
            redirect(request, response, "/login?sso=cancelled");
            return;
        }
        try {
            String email = app.sso.completeSignIn(pending, request.getParameter("state"), request.getParameter("code"));
            Optional<User> user = app.accounts.signInVerified(email, clientIp(request));
            if (user.isEmpty()) {
                redirect(request, response, "/login?sso=noaccount");
                return;
            }
            LoginController.startSession(request, user.get());
            redirect(request, response, "/dashboard");
        } catch (IOException e) {
            LOG.warning("Single sign-on failed: " + e.getMessage());
            redirect(request, response, "/login?sso=failed");
        }
    }
}
