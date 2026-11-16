package com.stride.controller;

import com.stride.config.App;
import com.stride.model.User;
import com.stride.service.ServiceException;
import com.stride.web.BaseController;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Notices: read (RQ10), post and remove (RQ19). */
@WebServlet(name = "noticeController", urlPatterns = {"/notices", "/notices/read", "/notices/new", "/notices/delete"})
public class NoticeController extends BaseController {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        App app = app();
        User user = user(request);
        if ("/notices/new".equals(request.getServletPath())) {
            request.setAttribute("modules", app.curriculum.modulesFor(user));
            request.setAttribute("preselect", intParam(request, "module"));
            render(request, response, "notice-new");
            return;
        }
        request.setAttribute("announcements", app.announcements.feed(user));
        render(request, response, "notices");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        App app = app();
        User user = user(request);
        switch (request.getServletPath()) {
            case "/notices/read":
                app.announcements.markAllRead(user);
                redirect(request, response, "/notices");
                return;
            case "/notices/new":
                try {
                    app.announcements.post(user, param(request, "title"), request.getParameter("body"),
                            intParam(request, "module"), "on".equals(request.getParameter("pinned")));
                    ok(request, "Your notice is posted.");
                    redirect(request, response, "/notices");
                } catch (ServiceException e) {
                    problem(request, e.getMessage());
                    redirect(request, response, "/notices/new");
                }
                return;
            case "/notices/delete":
                try {
                    app.announcements.delete(user, intParam(request, "id", -1));
                    ok(request, "The notice was removed.");
                } catch (ServiceException e) {
                    problem(request, e.getMessage());
                }
                redirect(request, response, "/notices");
                return;
            default:
                notFound(response);
        }
    }
}
