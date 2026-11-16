package com.stride.controller;

import com.stride.config.App;
import com.stride.model.Module;
import com.stride.model.Role;
import com.stride.model.TimetableEntry;
import com.stride.model.User;
import com.stride.service.ServiceException;
import com.stride.web.BaseController;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Administration (RQ20-RQ22): accounts, modules and enrolments, timetable, rooms, sites, audit log. */
@WebServlet(name = "adminController", urlPatterns = "/admin/*")
public class AdminController extends BaseController {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        App app = app();
        String action = request.getPathInfo() == null ? "/" : request.getPathInfo();
        switch (action) {
            case "/":
                redirect(request, response, "/dashboard");
                return;
            case "/users":
                request.setAttribute("roleFilter", param(request, "role"));
                request.setAttribute("search", param(request, "q"));
                request.setAttribute("users", app.accounts.list(Role.parse(param(request, "role")), param(request, "q")));
                request.setAttribute("roles", Role.values());
                render(request, response, "admin/users");
                return;
            case "/user": {
                Optional<User> u = app.accounts.find(intParam(request, "id", -1));
                if (u.isEmpty()) {
                    notFound(response);
                    return;
                }
                request.setAttribute("u", u.get());
                request.setAttribute("roles", Role.values());
                request.setAttribute("modules", app.curriculum.modulesFor(u.get()));
                render(request, response, "admin/user");
                return;
            }
            case "/modules":
                request.setAttribute("modules", app.curriculum.allModules());
                request.setAttribute("lecturers", app.curriculum.lecturers());
                render(request, response, "admin/modules");
                return;
            case "/module": {
                Optional<Module> m = app.curriculum.module(intParam(request, "id", -1));
                if (m.isEmpty()) {
                    notFound(response);
                    return;
                }
                request.setAttribute("m", m.get());
                request.setAttribute("lecturers", app.curriculum.lecturers());
                request.setAttribute("students", app.curriculum.students(m.get().getId()));
                render(request, response, "admin/module");
                return;
            }
            case "/timetable": {
                Map<DayOfWeek, List<TimetableEntry>> byDay = new EnumMap<>(DayOfWeek.class);
                for (TimetableEntry e : app.curriculum.timetable()) {
                    byDay.computeIfAbsent(e.getDay(), d -> new ArrayList<>()).add(e);
                }
                request.setAttribute("byDay", byDay.entrySet());
                request.setAttribute("modules", app.curriculum.allModules());
                request.setAttribute("rooms", app.curriculum.rooms());
                request.setAttribute("days", List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY));
                render(request, response, "admin/timetable");
                return;
            }
            case "/rooms":
                request.setAttribute("buildings", app.curriculum.buildings());
                render(request, response, "admin/rooms");
                return;
            case "/sites":
                request.setAttribute("sites", app.curriculum.sites());
                render(request, response, "admin/sites");
                return;
            case "/audit":
                request.setAttribute("entries", app.audit.recent(param(request, "action"), 200));
                request.setAttribute("actions", app.audit.actions());
                request.setAttribute("actionFilter", param(request, "action"));
                render(request, response, "admin/audit");
                return;
            default:
                notFound(response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        App app = app();
        User admin = user(request);
        String action = request.getPathInfo() == null ? "" : request.getPathInfo();
        String back = "/admin/users";
        try {
            switch (action) {
                case "/users/create": {
                    String temp = app.accounts.create(admin, param(request, "first"), param(request, "last"),
                            param(request, "email"), Role.parse(param(request, "role")), param(request, "number"));
                    flash(request, false, "Account created for " + param(request, "email") + ".",
                            "Temporary password (shown once, give it to the person privately): " + temp);
                    break;
                }
                case "/users/update":
                    back = "/admin/user?id=" + intParam(request, "id", -1);
                    app.accounts.update(admin, intParam(request, "id", -1), param(request, "first"),
                            param(request, "last"), param(request, "email"), Role.parse(param(request, "role")),
                            param(request, "number"));
                    ok(request, "Account saved.");
                    break;
                case "/users/reset": {
                    back = "/admin/user?id=" + intParam(request, "id", -1);
                    String temp = app.accounts.resetPassword(admin, intParam(request, "id", -1));
                    flash(request, false, "Password reset. They must choose a new one when they next sign in.",
                            "Temporary password (shown once): " + temp);
                    break;
                }
                case "/users/active":
                    back = "/admin/user?id=" + intParam(request, "id", -1);
                    app.accounts.setActive(admin, intParam(request, "id", -1), "true".equals(param(request, "active")));
                    ok(request, "true".equals(param(request, "active")) ? "Account switched on." : "Account switched off. "
                            + "Any open session ends on the next page load.");
                    break;
                case "/modules/create":
                    back = "/admin/modules";
                    app.curriculum.createModule(admin, param(request, "code"), param(request, "name"),
                            param(request, "description"), intParam(request, "lecturer"));
                    ok(request, "Module created.");
                    break;
                case "/modules/update":
                    back = "/admin/module?id=" + intParam(request, "id", -1);
                    app.curriculum.updateModule(admin, intParam(request, "id", -1), param(request, "name"),
                            param(request, "description"), intParam(request, "lecturer"));
                    ok(request, "Module saved.");
                    break;
                case "/modules/enrol": {
                    back = "/admin/module?id=" + intParam(request, "id", -1);
                    List<String> lines = app.curriculum.enrolMany(admin, intParam(request, "id", -1),
                            request.getParameter("students"));
                    flash(request, false, lines.get(0), lines.size() > 1 ? String.join(" | ", lines.subList(1, lines.size())) : null);
                    break;
                }
                case "/modules/unenrol":
                    back = "/admin/module?id=" + intParam(request, "id", -1);
                    app.curriculum.unenrol(admin, intParam(request, "id", -1), intParam(request, "student", -1));
                    ok(request, "Student removed from the module.");
                    break;
                case "/timetable/add":
                    back = "/admin/timetable";
                    app.curriculum.addSlot(admin, intParam(request, "module", -1), intParam(request, "room", -1),
                            day(param(request, "day")), time(param(request, "start")), time(param(request, "end")));
                    ok(request, "Class added to the timetable. No clashes were found.");
                    break;
                case "/timetable/remove":
                    back = "/admin/timetable";
                    app.curriculum.removeSlot(admin, intParam(request, "id", -1));
                    ok(request, "Class removed from the timetable.");
                    break;
                case "/rooms/add":
                    back = "/admin/rooms";
                    app.curriculum.addRoom(admin, param(request, "name"), intParam(request, "building", -1),
                            intParam(request, "capacity", 0));
                    ok(request, "Room added.");
                    break;
                case "/sites/update":
                    back = "/admin/sites";
                    app.curriculum.updateSite(admin, param(request, "slug"), param(request, "name"),
                            param(request, "task"), param(request, "description"), param(request, "keywords"),
                            param(request, "url"));
                    ok(request, "Site saved.");
                    break;
                default:
                    notFound(response);
                    return;
            }
        } catch (ServiceException e) {
            problem(request, e.getMessage());
        }
        redirect(request, response, back);
    }

    private static DayOfWeek day(String v) {
        try {
            return DayOfWeek.valueOf(v);
        } catch (IllegalArgumentException e) {
            throw new ServiceException("Choose a day.");
        }
    }

    private static LocalTime time(String v) {
        try {
            return LocalTime.parse(v);
        } catch (DateTimeParseException e) {
            throw new ServiceException("Enter times as hours and minutes, for example 08:00.");
        }
    }
}
