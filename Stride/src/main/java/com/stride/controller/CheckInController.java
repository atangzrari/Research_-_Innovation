package com.stride.controller;

import com.stride.config.App;
import com.stride.model.AttendanceSession;
import com.stride.model.User;
import com.stride.service.AttendanceService;
import com.stride.service.CheckInResult;
import com.stride.web.BaseController;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Student check-in (RQ16). GET shows the code form, or, when opened from a QR scan, a one-tap
 * confirmation. POST validates and records the check-in. Checking in changes data, so it is a POST
 * with a CSRF token even when it starts from a QR link.
 */
@WebServlet(name = "checkInController", urlPatterns = "/checkin")
public class CheckInController extends BaseController {

    static final String DEVICE_COOKIE = "stride_device";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        App app = app();
        User user = user(request);
        deviceHash(request, response);
        Integer qrSession = intParam(request, "s");
        if (qrSession != null) {
            app.attendanceDao.session(qrSession).ifPresent(s -> request.setAttribute("qrSession", s));
            request.setAttribute("qrToken", param(request, "t"));
        }
        request.setAttribute("openSessions", app.attendance.openSessionsFor(user));
        request.setAttribute("locationOn", app.campusLocation.isEnabled());
        request.setAttribute("codeSeconds", app.attendance.getCodeSeconds());
        render(request, response, "checkin");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        App app = app();
        User user = user(request);
        AttendanceService.Attempt attempt = new AttendanceService.Attempt(request.getParameter("code"),
                intParam(request, "s"), request.getParameter("t"), clientIp(request), deviceHash(request, response),
                app.campusLocation.classify(request.getParameter("lat"), request.getParameter("lon"),
                        request.getParameter("acc")));
        CheckInResult result = app.attendance.checkIn(user, attempt);
        if (!result.isOk()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        request.setAttribute("result", result);
        request.setAttribute("openSessions", app.attendance.openSessionsFor(user));
        request.setAttribute("locationOn", app.campusLocation.isEnabled());
        request.setAttribute("codeSeconds", app.attendance.getCodeSeconds());
        AttendanceSession s = result.getSession();
        if (s != null && !result.isOk() && attempt != null && request.getParameter("s") != null) {
            request.setAttribute("qrSession", s);
        }
        render(request, response, "checkin");
    }

    /**
     * A random id kept in a long-lived cookie on this device. Only its SHA-256 hash is stored, and
     * only to notice when one phone checks in several students to the same class (RQ17).
     */
    static String deviceHash(HttpServletRequest request, HttpServletResponse response) {
        String id = null;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (DEVICE_COOKIE.equals(c.getName()) && c.getValue().matches("[0-9a-f]{32}")) {
                    id = c.getValue();
                }
            }
        }
        if (id == null) {
            byte[] b = new byte[16];
            RANDOM.nextBytes(b);
            id = HexFormat.of().formatHex(b);
            Cookie cookie = new Cookie(DEVICE_COOKIE, id);
            cookie.setHttpOnly(true);
            cookie.setMaxAge(60 * 60 * 24 * 365);
            cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
            cookie.setSecure(request.isSecure());
            cookie.setAttribute("SameSite", "Lax");
            response.addCookie(cookie);
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(id.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
