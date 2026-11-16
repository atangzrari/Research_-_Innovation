package com.stride.controller;

import com.stride.config.App;
import com.stride.model.AttendanceSession;
import com.stride.model.CheckIn;
import com.stride.model.RosterRow;
import com.stride.model.TodayClass;
import com.stride.model.User;
import com.stride.report.Report;
import com.stride.service.ServiceException;
import com.stride.util.Json;
import com.stride.util.QrCodes;
import com.stride.web.BaseController;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The lecturer's attendance tools (RQ15, RQ18): open sessions, show the code, review and correct. */
@WebServlet(name = "lecturerController", urlPatterns = "/lecturer/*")
public class LecturerController extends BaseController {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        App app = app();
        User user = user(request);
        String action = request.getPathInfo() == null ? "/sessions" : request.getPathInfo();
        try {
            switch (action) {
                case "/sessions": {
                    List<TodayClass> today = app.schedule.classesToday(user, app.now());
                    List<AttendanceSession> recent = app.attendance.recentSessions(user);
                    Map<Integer, AttendanceSession> openByEntry = new HashMap<>();
                    for (AttendanceSession s : recent) {
                        if (s.isOpenAt(app.now()) && s.getTimetableEntryId() != null) {
                            openByEntry.put(s.getTimetableEntryId(), s);
                        }
                    }
                    request.setAttribute("todayClasses", today);
                    request.setAttribute("openByEntry", openByEntry);
                    request.setAttribute("sessions", recent);
                    request.setAttribute("modules", app.curriculum.modulesFor(user));
                    request.setAttribute("rooms", app.curriculum.rooms());
                    render(request, response, "lecturer/sessions");
                    return;
                }
                case "/session": {
                    AttendanceSession s = app.attendance.sessionForLecturer(user, intParam(request, "id", -1));
                    List<RosterRow> roster = app.attendance.roster(s);
                    request.setAttribute("s", s);
                    request.setAttribute("open", app.attendance.isOpen(s));
                    request.setAttribute("roster", roster);
                    request.setAttribute("statuses", CheckIn.Status.values());
                    int in = 0;
                    int flagged = 0;
                    for (RosterRow r : roster) {
                        if (r.isCheckedIn()) in++;
                        if (r.getCheckIn() != null && r.getCheckIn().isFlagged()) flagged++;
                    }
                    request.setAttribute("inCount", in);
                    request.setAttribute("flaggedCount", flagged);
                    if (app.attendance.isOpen(s)) {
                        request.setAttribute("code", app.attendance.currentCode(s));
                        request.setAttribute("secondsLeft", app.attendance.secondsUntilNextCode());
                    }
                    render(request, response, "lecturer/session");
                    return;
                }
                case "/display": {
                    AttendanceSession s = app.attendance.sessionForLecturer(user, intParam(request, "id", -1));
                    request.setAttribute("s", s);
                    request.setAttribute("open", app.attendance.isOpen(s));
                    request.setAttribute("code", app.attendance.currentCode(s));
                    request.setAttribute("secondsLeft", app.attendance.secondsUntilNextCode());
                    request.setAttribute("codeSeconds", app.attendance.getCodeSeconds());
                    request.setAttribute("checkinUrl", baseUrl(request) + "/checkin");
                    render(request, response, "lecturer/display");
                    return;
                }
                case "/live": {
                    AttendanceSession s = app.attendance.sessionForLecturer(user, intParam(request, "id", -1));
                    Map<String, Object> out = new LinkedHashMap<>();
                    boolean open = app.attendance.isOpen(s);
                    out.put("open", open);
                    out.put("code", open ? app.attendance.currentCode(s) : "");
                    out.put("secondsLeft", app.attendance.secondsUntilNextCode());
                    AttendanceSession fresh = app.attendanceDao.session(s.getId()).orElse(s);
                    out.put("checkedIn", fresh.getCheckedIn());
                    out.put("enrolled", fresh.getEnrolled());
                    out.put("flagged", fresh.getFlagged());
                    out.put("closes", fresh.getClosesText());
                    response.setContentType("application/json;charset=UTF-8");
                    response.setHeader("Cache-Control", "no-store");
                    response.getWriter().write(Json.write(out));
                    return;
                }
                case "/qr": {
                    AttendanceSession s = app.attendance.sessionForLecturer(user, intParam(request, "id", -1));
                    if (!app.attendance.isOpen(s)) {
                        notFound(response);
                        return;
                    }
                    String link = baseUrl(request) + "/checkin?s=" + s.getId() + "&t=" + app.attendance.currentQrToken(s);
                    response.setContentType("image/svg+xml");
                    response.setHeader("Cache-Control", "no-store");
                    response.getOutputStream().write(QrCodes.svg(link).getBytes(StandardCharsets.UTF_8));
                    return;
                }
                case "/roster.csv": {
                    AttendanceSession s = app.attendance.sessionForLecturer(user, intParam(request, "id", -1));
                    Report csv = new Report("roster", "Roster", "", List.of("Student", "Student number", "Status",
                            "Time", "Method", "Confidence", "Flagged", "Factors", "Correction"));
                    for (RosterRow r : app.attendance.roster(s)) {
                        CheckIn c = r.getCheckIn();
                        csv.row(r.getStudent().getFullName(), r.getStudent().getStudentNumber(), r.getStatusLabel(),
                                c == null ? "" : c.getTimeText(), c == null ? "" : c.getMethodLabel(),
                                c == null ? "" : c.getConfidence(), c != null && c.isFlagged() ? "Yes" : "",
                                c == null ? "" : c.getFactors(), c == null || c.getCorrectionReason() == null ? ""
                                        : c.getCorrectionReason());
                    }
                    response.setContentType("text/csv;charset=UTF-8");
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + s.getModuleCode() + "-"
                            + s.getSessionDate() + ".csv\"");
                    response.getOutputStream().write(csv.toCsv().getBytes(StandardCharsets.UTF_8));
                    return;
                }
                default:
                    notFound(response);
            }
        } catch (ServiceException e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        App app = app();
        User user = user(request);
        String action = request.getPathInfo() == null ? "" : request.getPathInfo();
        int id = intParam(request, "id", -1);
        try {
            switch (action) {
                case "/open": {
                    AttendanceSession s = app.attendance.openForEntry(user, intParam(request, "entry", -1));
                    redirect(request, response, "/lecturer/session?id=" + s.getId());
                    return;
                }
                case "/open-extra": {
                    AttendanceSession s = app.attendance.openAdHoc(user, intParam(request, "module", -1),
                            intParam(request, "room"));
                    redirect(request, response, "/lecturer/session?id=" + s.getId());
                    return;
                }
                case "/close":
                    app.attendance.close(user, id);
                    ok(request, "Check-in is closed. Late arrivals can be added by hand below.");
                    break;
                case "/extend":
                    app.attendance.extend(user, id);
                    ok(request, "Check-in stays open for ten more minutes.");
                    break;
                case "/correct":
                    app.attendance.correct(user, id, intParam(request, "student", -1), status(param(request, "status")),
                            request.getParameter("reason"));
                    ok(request, "The record was updated and the change was logged.");
                    break;
                case "/accept":
                    app.attendance.acceptFlagged(user, id, intParam(request, "student", -1));
                    ok(request, "Check-in accepted.");
                    break;
                default:
                    notFound(response);
                    return;
            }
        } catch (ServiceException e) {
            problem(request, e.getMessage());
            if (action.startsWith("/open")) {
                redirect(request, response, "/lecturer/sessions");
                return;
            }
        }
        redirect(request, response, "/lecturer/session?id=" + id);
    }

    private static CheckIn.Status status(String v) {
        try {
            return CheckIn.Status.valueOf(v);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** scheme://host[:port]/context, for the link inside the QR image. */
    static String baseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        return scheme + "://" + request.getServerName() + (defaultPort ? "" : ":" + port) + request.getContextPath();
    }
}
