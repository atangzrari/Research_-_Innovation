package com.stride.config;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.SessionCookieConfig;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Starts Stride when Tomcat deploys it, and closes the database when it stops. */
public class AppListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext ctx = event.getServletContext();
        App app = (App) ctx.getAttribute(App.CONTEXT_KEY);
        if (app == null) {
            try {
                app = new App(AppConfig.load()).start();
            } catch (RuntimeException e) {
                LOG.log(Level.SEVERE, "Stride failed to start", e);
                throw e;
            }
            ctx.setAttribute(App.CONTEXT_KEY, app);
        }
        // Defect D7: the Secure flag is switched on from configuration when Stride is served over HTTPS.
        SessionCookieConfig cookies = ctx.getSessionCookieConfig();
        cookies.setSecure(app.secureCookies);
        LOG.info("Stride started (database: " + (app.db.isMySql() ? "MySQL" : "H2") + ", secure cookies: "
                + app.secureCookies + ")");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Object app = event.getServletContext().getAttribute(App.CONTEXT_KEY);
        if (app instanceof App) {
            ((App) app).close();
        }
    }
}
