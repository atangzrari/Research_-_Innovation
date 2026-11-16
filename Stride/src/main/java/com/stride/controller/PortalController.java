package com.stride.controller;

import com.stride.config.App;
import com.stride.data.ServiceCatalogue;
import com.stride.model.Announcement;
import com.stride.model.Assessment;
import com.stride.model.AttendanceRecord;
import com.stride.model.AttendanceSession;
import com.stride.model.Building;
import com.stride.model.CampusService;
import com.stride.model.FreeRoom;
import com.stride.model.Module;
import com.stride.model.TimetableEntry;
import com.stride.model.TodayClass;
import com.stride.model.User;
import com.stride.service.IcsWriter;
import com.stride.web.BaseController;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The signed-in pages everyone uses. Each URL gets the data its view needs, then forwards to it.
 * Sign-in and role rules are checked by the filters before this class runs.
 */
@WebServlet(name = "portalController", urlPatterns = {"/dashboard", "/classes", "/timetable", "/timetable.ics",
        "/attendance", "/sites", "/help", "/map", "/modules", "/module"})
public class PortalController extends BaseController {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        App app = app();
        User user = user(request);
        LocalDateTime now = app.now();
        switch (request.getServletPath()) {
            case "/dashboard":
                dashboard(request, response, app, user, now);
                return;
            case "/classes":
                request.setAttribute("todayClasses", app.schedule.classesToday(user, now));
                request.setAttribute("freeRooms", app.schedule.freeRoomsNow(now));
                request.setAttribute("roomsOpen", app.schedule.roomsOpenNow(now));
                request.setAttribute("openText", app.schedule.getOpen() + " and " + app.schedule.getClose());
                render(request, response, "classes");
                return;
            case "/timetable":
                request.setAttribute("week", app.schedule.week(user).entrySet());
                request.setAttribute("todayName", now.getDayOfWeek().name());
                render(request, response, "timetable");
                return;
            case "/timetable.ics":
                response.setContentType("text/calendar;charset=UTF-8");
                response.setHeader("Content-Disposition", "attachment; filename=\"stride-timetable.ics\"");
                response.getOutputStream().write(IcsWriter.forTimetable(app.schedule.entriesFor(user),
                        now.toLocalDate(), app.zone.getId()).getBytes(StandardCharsets.UTF_8));
                return;
            case "/attendance": {
                List<Module> mods = app.curriculum.modulesFor(user);
                List<AttendanceRecord> records = app.attendance.summaryFor(user, mods);
                request.setAttribute("attendance", records);
                request.setAttribute("history", app.attendance.history(user, 15));
                request.setAttribute("riskPercent", app.riskPercent);
                int att = 0;
                int tot = 0;
                for (AttendanceRecord r : records) {
                    att += r.getAttended();
                    tot += r.getTotal();
                }
                request.setAttribute("overall", new AttendanceRecord("ALL", "All modules", att, tot));
                render(request, response, "attendance");
                return;
            }
            case "/sites": {
                String query = request.getParameter("q");
                List<CampusService> all = app.curriculum.sites();
                request.setAttribute("query", query == null ? "" : query.trim());
                request.setAttribute("services", ServiceCatalogue.search(all, query));
                request.setAttribute("total", all.size());
                request.setAttribute("moduleHits", moduleHits(app, user, query));
                render(request, response, "sites");
                return;
            }
            case "/help":
                render(request, response, "help");
                return;
            case "/map": {
                List<Building> buildings = app.curriculum.buildings();
                Map<String, FreeRoom> free = new HashMap<>();
                for (FreeRoom r : app.schedule.freeRoomsNow(now)) free.put(r.getName(), r);
                for (Building b : buildings) {
                    int n = 0;
                    for (com.stride.model.Room r : b.getRooms()) if (free.containsKey(r.getName())) n++;
                    b.setFreeNow(n);
                }
                request.setAttribute("buildings", buildings);
                request.setAttribute("free", free);
                String highlight = request.getParameter("room");
                request.setAttribute("highlight", highlight);
                for (Building b : buildings) {
                    for (com.stride.model.Room r : b.getRooms()) {
                        if (r.getName().equals(highlight)) request.setAttribute("highlightBuilding", b.getName());
                    }
                }
                render(request, response, "map");
                return;
            }
            case "/modules":
                request.setAttribute("modules", app.curriculum.modulesFor(user));
                render(request, response, "modules");
                return;
            case "/module":
                module(request, response, app, user);
                return;
            default:
                notFound(response);
        }
    }

    private void dashboard(HttpServletRequest request, HttpServletResponse response, App app, User user,
                           LocalDateTime now) throws ServletException, IOException {
        List<TodayClass> classes = app.schedule.classesToday(user, now);
        request.setAttribute("todayClasses", classes);
        request.setAttribute("classCount", classes.size());
        request.setAttribute("nextClass", firstNotFinished(classes));
        List<Announcement> feed = app.announcements.feed(user);
        request.setAttribute("latestNotice", feed.isEmpty() ? null : feed.get(0));
        if (user.isStudent()) {
            request.setAttribute("tiles", app.curriculum.sites());
            request.setAttribute("openSessions", app.attendance.openSessionsFor(user));
            List<AttendanceRecord> att = app.attendance.summaryFor(user, app.curriculum.modulesFor(user));
            AttendanceRecord lowest = null;
            for (AttendanceRecord r : att) {
                if (r.getTotal() > 0 && (lowest == null || r.getPercent() < lowest.getPercent())) lowest = r;
            }
            request.setAttribute("lowest", lowest);
            request.setAttribute("riskPercent", app.riskPercent);
            List<Assessment> due = app.planner.upcoming(user, now, 21);
            request.setAttribute("nextDeadline", due.isEmpty() ? null : due.get(0));
            render(request, response, "dashboard");
        } else if (user.isLecturer()) {
            List<AttendanceSession> recent = app.attendance.recentSessions(user);
            List<AttendanceSession> live = new ArrayList<>();
            int flagged = 0;
            for (AttendanceSession s : recent) {
                if (s.isOpenAt(now)) live.add(s);
                flagged += s.getFlagged();
            }
            request.setAttribute("liveSessions", live);
            request.setAttribute("flaggedCount", flagged);
            request.setAttribute("modules", app.curriculum.modulesFor(user));
            render(request, response, "dashboard-lecturer");
        } else {
            Map<String, Integer> counts = new HashMap<>();
            app.accounts.countByRole().forEach((role, n) -> counts.put(role.name(), n));
            request.setAttribute("counts", counts);
            request.setAttribute("moduleCount", app.curriculum.allModules().size());
            request.setAttribute("roomCount", app.curriculum.rooms().size());
            request.setAttribute("slotCount", app.curriculum.timetable().size());
            request.setAttribute("recentAudit", app.audit.recent(null, 8));
            request.setAttribute("freeNow", app.schedule.freeRoomsNow(now).size());
            render(request, response, "dashboard-admin");
        }
    }

    private void module(HttpServletRequest request, HttpServletResponse response, App app, User user)
            throws ServletException, IOException {
        Optional<Module> found = app.curriculum.module(param(request, "code"));
        if (found.isEmpty() || !app.curriculum.canView(user, found.get())) {
            notFound(response);
            return;
        }
        Module m = found.get();
        request.setAttribute("module", m);
        request.setAttribute("teaches", app.curriculum.teaches(user, m) || user.isAdministrator());
        request.setAttribute("assessments", app.curriculum.assessments(m.getId()));
        request.setAttribute("resources", app.curriculum.resources(m.getId()));
        request.setAttribute("notices", app.announcements.forModule(user, m.getId()));
        List<TimetableEntry> slots = new ArrayList<>();
        for (TimetableEntry e : app.curriculum.timetable()) {
            if (e.getModuleId() == m.getId()) slots.add(e);
        }
        request.setAttribute("slots", slots);
        request.setAttribute("kinds", com.stride.model.Assessment.Kind.values());
        if (user.isStudent()) {
            for (AttendanceRecord r : app.attendance.summaryFor(user, List.of(m))) {
                request.setAttribute("myAttendance", r);
            }
        }
        render(request, response, "module");
    }

    /** Modules whose code or name matches the search, so the top search also finds courses. */
    private static List<Module> moduleHits(App app, User user, String query) {
        List<Module> out = new ArrayList<>();
        if (query == null || query.isBlank()) return out;
        String q = query.trim().toLowerCase(java.util.Locale.ROOT);
        for (Module m : app.curriculum.modulesFor(user)) {
            if (m.getCode().toLowerCase(java.util.Locale.ROOT).contains(q)
                    || m.getName().toLowerCase(java.util.Locale.ROOT).contains(q)) {
                out.add(m);
            }
        }
        out.sort(Comparator.comparing(Module::getCode));
        return out;
    }

    /** The class happening now, or else the next one today; null when nothing is left. */
    static TodayClass firstNotFinished(List<TodayClass> classes) {
        for (TodayClass c : classes) {
            if (c.getStatus() != TodayClass.Status.DONE) {
                return c;
            }
        }
        return null;
    }

    static DayOfWeek day(String name) {
        try {
            return DayOfWeek.valueOf(name);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
