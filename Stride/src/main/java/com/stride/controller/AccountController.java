package com.stride.controller;

import com.stride.model.User;
import com.stride.service.ServiceException;
import com.stride.web.BaseController;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** The person's own account: details and changing the password. */
@WebServlet(name = "accountController", urlPatterns = {"/account", "/account/password"})
public class AccountController extends BaseController {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = user(request);
        request.setAttribute("modules", app().curriculum.modulesFor(user));
        request.setAttribute("forced", user.isMustChangePassword());
        request.setAttribute("ssoEnabled", app().sso.isEnabled());
        render(request, response, "account");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = user(request);
        try {
            app().accounts.changePassword(user, request.getParameter("current"), request.getParameter("next"),
                    request.getParameter("repeat"));
            request.getSession().setAttribute(com.stride.util.SessionKeys.USER, user.withMustChangePassword(false));
            ok(request, "Your password has been changed.");
            redirect(request, response, "/account");
        } catch (ServiceException e) {
            problem(request, e.getMessage());
            redirect(request, response, "/account/password");
        }
    }
}
