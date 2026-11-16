package com.stride.filter;

import com.stride.model.Role;
import com.stride.model.User;
import com.stride.util.SessionKeys;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Role rules for whole areas of the site (RQ02, defect D6), checked on every request. Services
 * check again (for example, a lecturer may only manage modules they teach), so hiding a link is
 * never the only protection (Finding F10).
 */
public class AuthorizationFilter implements Filter {

    private static final Map<String, Set<Role>> RULES = new LinkedHashMap<>();

    static {
        RULES.put("/admin", EnumSet.of(Role.ADMINISTRATOR));
        RULES.put("/lecturer", EnumSet.of(Role.LECTURER));
        RULES.put("/reports", EnumSet.of(Role.LECTURER, Role.ADMINISTRATOR));
        RULES.put("/report", EnumSet.of(Role.LECTURER, Role.ADMINISTRATOR));
        RULES.put("/notices/new", EnumSet.of(Role.LECTURER, Role.ADMINISTRATOR));
        RULES.put("/checkin", EnumSet.of(Role.STUDENT));
        RULES.put("/planner", EnumSet.of(Role.STUDENT));
        RULES.put("/assistant", EnumSet.of(Role.STUDENT));
        RULES.put("/attendance", EnumSet.of(Role.STUDENT));
    }

    /** The roles allowed on a path, or null if every signed-in role may use it. */
    static Set<Role> allowed(String path) {
        for (Map.Entry<String, Set<Role>> rule : RULES.entrySet()) {
            String prefix = rule.getKey();
            if (path.equals(prefix) || path.startsWith(prefix + "/") || path.startsWith(prefix + ".")) {
                return rule.getValue();
            }
        }
        return null;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = AuthenticationFilter.pathOf(request);
        Set<Role> roles = allowed(path);
        if (roles != null) {
            HttpSession session = request.getSession(false);
            User user = session == null ? null : (User) session.getAttribute(SessionKeys.USER);
            if (user == null || !roles.contains(user.getRole())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
