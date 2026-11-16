package com.stride.web;

import com.stride.config.App;
import com.stride.model.User;
import com.stride.security.CsrfTokens;
import com.stride.util.SessionKeys;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared plumbing for Stride's controllers: data every page needs, flashes, redirects, parameters. */
public abstract class BaseController extends HttpServlet {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);

    protected App app() {
        return (App) getServletContext().getAttribute(App.CONTEXT_KEY);
    }

    /** The signed-in user (AuthenticationFilter guarantees one on protected paths). */
    protected static User user(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        return s == null ? null : (User) s.getAttribute(SessionKeys.USER);
    }

    /** Sets what every page uses (date, greeting, token, unread count, flashes) and shows the view. */
    protected void render(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        App app = app();
        LocalDateTime now = app.now();
        User user = user(request);
        request.setAttribute("now", now);
        request.setAttribute("today", DATE.format(now));
        request.setAttribute("greeting", greetingFor(now.getHour()));
        request.setAttribute("csrfToken", CsrfTokens.token(request.getSession()));
        if (user != null && request.getAttribute("unreadCount") == null) {
            request.setAttribute("unreadCount", app.announcements.unread(user));
        }
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SessionKeys.FLASH) != null) {
            request.setAttribute("flashes", session.getAttribute(SessionKeys.FLASH));
            session.removeAttribute(SessionKeys.FLASH);
        }
        response.setContentType("text/html;charset=UTF-8");
        request.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(request, response);
    }

    protected static void redirect(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }

    @SuppressWarnings("unchecked")
    protected static void flash(HttpServletRequest request, boolean error, String text, String detail) {
        HttpSession s = request.getSession();
        List<Flash> list = (List<Flash>) s.getAttribute(SessionKeys.FLASH);
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(new Flash(error, text, detail));
        s.setAttribute(SessionKeys.FLASH, list);
    }

    protected static void ok(HttpServletRequest request, String text) {
        flash(request, false, text, null);
    }

    protected static void problem(HttpServletRequest request, String text) {
        flash(request, true, text, null);
    }

    protected static Integer intParam(HttpServletRequest request, String name) {
        String v = request.getParameter(name);
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected static int intParam(HttpServletRequest request, String name, int fallback) {
        Integer v = intParam(request, name);
        return v == null ? fallback : v;
    }

    protected static String param(HttpServletRequest request, String name) {
        String v = request.getParameter(name);
        return v == null ? "" : v.trim();
    }

    /**
     * The client's address. Tomcat's RemoteIpValve should be configured if Stride runs behind a
     * proxy; Stride itself never trusts X-Forwarded-For, which a client can forge.
     */
    protected static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    protected static void notFound(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    public static String greetingFor(int hour) {
        if (hour < 12) return "Good morning";
        if (hour < 18) return "Good afternoon";
        return "Good evening";
    }
}
