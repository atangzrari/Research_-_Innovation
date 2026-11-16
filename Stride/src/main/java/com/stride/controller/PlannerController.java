package com.stride.controller;

import com.stride.config.App;
import com.stride.model.User;
import com.stride.service.IcsWriter;
import com.stride.service.StudyPlanService;
import com.stride.web.BaseController;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/** The study planner (RQ23) and its calendar download. Preferences come from the form (GET). */
@WebServlet(name = "plannerController", urlPatterns = {"/planner", "/planner.ics"})
public class PlannerController extends BaseController {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        App app = app();
        User user = user(request);
        LocalDateTime now = app.now();
        StudyPlanService.Preferences prefs = preferences(request);
        StudyPlanService.Plan plan = app.planner.plan(user, now, prefs);
        if ("/planner.ics".equals(request.getServletPath())) {
            IcsWriter ics = new IcsWriter("Stride study plan", app.zone.getId());
            for (StudyPlanService.Day d : plan.getDays()) {
                for (StudyPlanService.Block b : d.getBlocks()) {
                    if (b.isStudy()) {
                        ics.event(b.getStart(), b.getEnd(), "Study: " + b.getModuleCode() + " " + b.getTitle(),
                                "Planned by Stride", null, null);
                    }
                }
            }
            response.setContentType("text/calendar;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"stride-study-plan.ics\"");
            response.getOutputStream().write(ics.build().getBytes(StandardCharsets.UTF_8));
            return;
        }
        request.setAttribute("plan", plan);
        request.setAttribute("query", request.getQueryString() == null ? "" : request.getQueryString());
        render(request, response, "planner");
    }

    static StudyPlanService.Preferences preferences(HttpServletRequest request) {
        StudyPlanService.Preferences d = StudyPlanService.Preferences.defaults();
        LocalTime from = time(request.getParameter("from"), d.from);
        LocalTime to = time(request.getParameter("to"), d.to);
        if (!from.isBefore(to)) {
            from = d.from;
            to = d.to;
        }
        return new StudyPlanService.Preferences(from, to, intParam(request, "hours", d.maxHoursPerDay),
                "on".equals(request.getParameter("sundays")));
    }

    private static LocalTime time(String v, LocalTime fallback) {
        try {
            return v == null || v.isBlank() ? fallback : LocalTime.parse(v).withMinute(0);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }
}
