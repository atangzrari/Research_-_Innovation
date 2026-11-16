package com.stride;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.Wrapper;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;
import java.nio.file.Files;

/**
 * Runs Stride on an embedded Tomcat 11, with no Tomcat installation or run configuration needed.
 * In IntelliJ: right-click this class, Run 'DevServer.main()'. Then open http://localhost:8080/.
 * Arguments: [port]. Any setting can be overridden with -Dstride.key=value, for example
 * -Dstride.clock.fixed=2026-10-29T15:27 to reproduce the report's screenshots.
 */
public final class DevServer {

    private final Tomcat tomcat = new Tomcat();
    private Context context;

    public DevServer start(int port) throws Exception {
        File base = Files.createTempDirectory("stride-tomcat").toFile();
        tomcat.setBaseDir(base.getAbsolutePath());
        tomcat.setPort(port);
        tomcat.getConnector();
        File webapp = new File("src/main/webapp").getAbsoluteFile();
        context = tomcat.addWebapp("", webapp.getAbsolutePath());
        // Let Tomcat find the @WebServlet classes compiled by Maven or IntelliJ.
        StandardRoot resources = new StandardRoot(context);
        File classes = new File("target/classes").getAbsoluteFile();
        if (classes.isDirectory()) {
            resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", classes.getAbsolutePath(), "/"));
        }
        context.setResources(resources);
        // Load classes from the normal class path (one copy of each), so tests can reach the running app.
        WebappLoader loader = new WebappLoader();
        loader.setDelegate(true);
        context.setLoader(loader);
        String jspCompiler = System.getProperty("stride.test.jspCompiler");
        if (jspCompiler != null) {
            // Only for environments without the Eclipse compiler (ECJ); Maven builds use ECJ.
            context.addLifecycleListener(event -> {
                if (Lifecycle.CONFIGURE_START_EVENT.equals(event.getType())) {
                    Wrapper jsp = (Wrapper) context.findChild("jsp");
                    if (jsp != null) {
                        jsp.addInitParameter("compilerClassName", jspCompiler);
                    }
                }
            });
        }
        tomcat.start();
        return this;
    }

    public int port() {
        return tomcat.getConnector().getLocalPort();
    }

    public Context context() {
        return context;
    }

    public void stop() throws Exception {
        tomcat.stop();
        tomcat.destroy();
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        DevServer server = new DevServer().start(port);
        System.out.println("Stride is running at http://localhost:" + server.port() + "/  (Ctrl+C to stop)");
        server.tomcat.getServer().await();
    }
}
