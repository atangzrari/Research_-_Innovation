package com.stride.controller;

import com.stride.model.User;
import com.stride.web.BaseController;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/** POST /logout ends the session (RQ03). POST-only with a CSRF token (checked by CsrfFilter). */
@WebServlet(name = "logoutController", urlPatterns = "/logout")
public class LogoutController extends BaseController {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        redirect(request, response, "/dashboard");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = user(request);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        if (user != null) {
            app().audit.log(app().now(), user.getUserId(), "SIGN_OUT", "Signed out", clientIp(request));
        }
        redirect(request, response, "/login?signedout=1");
    }
}
