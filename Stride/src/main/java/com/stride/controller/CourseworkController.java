package com.stride.controller;

import com.stride.config.App;
import com.stride.model.Assessment;
import com.stride.model.Module;
import com.stride.model.User;
import com.stride.service.ServiceException;
import com.stride.web.BaseController;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/** Lecturers add and remove assessments and learning resources on their module pages. */
@WebServlet(name = "courseworkController", urlPatterns = {"/module/assessment/add", "/module/assessment/delete",
        "/module/resource/add", "/module/resource/delete"})
public class CourseworkController extends BaseController {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        App app = app();
        User user = user(request);
        Optional<Module> found = app.curriculum.module(param(request, "code"));
        if (found.isEmpty()) {
            notFound(response);
            return;
        }
        Module m = found.get();
        try {
            switch (request.getServletPath()) {
                case "/module/assessment/add":
                    app.curriculum.addAssessment(user, m, param(request, "title"), kind(param(request, "kind")),
                            due(param(request, "due")), intParam(request, "weight", -1), intParam(request, "hours"));
                    ok(request, "Assessment added. Students will see it in their study planner.");
                    break;
                case "/module/assessment/delete":
                    app.curriculum.removeAssessment(user, m, intParam(request, "id", -1));
                    ok(request, "Assessment removed.");
                    break;
                case "/module/resource/add":
                    app.curriculum.addResource(user, m, param(request, "title"), param(request, "url"),
                            param(request, "kind"));
                    ok(request, "Resource added.");
                    break;
                case "/module/resource/delete":
                    app.curriculum.removeResource(user, m, intParam(request, "id", -1));
                    ok(request, "Resource removed.");
                    break;
                default:
                    notFound(response);
                    return;
            }
        } catch (ServiceException e) {
            problem(request, e.getMessage());
        }
        redirect(request, response, "/module?code=" + URLEncoder.encode(m.getCode(), StandardCharsets.UTF_8));
    }

    private static Assessment.Kind kind(String v) {
        try {
            return Assessment.Kind.valueOf(v);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static LocalDateTime due(String v) {
        try {
            return v.isEmpty() ? null : LocalDateTime.parse(v);
        } catch (DateTimeParseException e) {
            throw new ServiceException("Enter the due date and time.");
        }
    }
}
