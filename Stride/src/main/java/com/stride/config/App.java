package com.stride.config;

import com.stride.dao.AnnouncementDao;
import com.stride.dao.AttendanceDao;
import com.stride.dao.AuditDao;
import com.stride.dao.CampusDao;
import com.stride.dao.CourseworkDao;
import com.stride.dao.ModuleDao;
import com.stride.dao.SiteDao;
import com.stride.dao.TimetableDao;
import com.stride.dao.UserDao;
import com.stride.db.Database;
import com.stride.db.SchemaInstaller;
import com.stride.db.Seeder;
import com.stride.report.ReportService;
import com.stride.security.NetworkMatcher;
import com.stride.security.OidcClient;
import com.stride.security.PasswordHasher;
import com.stride.security.SignInGuard;
import com.stride.service.AccountService;
import com.stride.service.AnnouncementService;
import com.stride.service.AssistantService;
import com.stride.service.AttendanceService;
import com.stride.service.CampusLocation;
import com.stride.service.CurriculumService;
import com.stride.service.ScheduleService;
import com.stride.service.StudyPlanService;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.logging.Logger;

/**
 * Builds and holds the application's objects: configuration, database, data-access classes and
 * services. Created once at start-up by AppListener; controllers reach it through the
 * ServletContext. Tests build their own with an in-memory database.
 */
public final class App implements AutoCloseable {

    public static final String CONTEXT_KEY = "stride.app";
    private static final Logger LOG = Logger.getLogger(App.class.getName());

    public final AppConfig config;
    public final ZoneId zone;
    public final Clock clock;
    public final Database db;

    public final UserDao users;
    public final ModuleDao modules;
    public final CampusDao campus;
    public final TimetableDao timetable;
    public final AttendanceDao attendanceDao;
    public final AnnouncementDao announcementDao;
    public final CourseworkDao coursework;
    public final SiteDao sites;
    public final AuditDao audit;

    public final AccountService accounts;
    public final ScheduleService schedule;
    public final AttendanceService attendance;
    public final AnnouncementService announcements;
    public final CurriculumService curriculum;
    public final StudyPlanService planner;
    public final AssistantService assistant;
    public final ReportService reports;

    public final SignInGuard signInGuard;
    public final OidcClient sso;
    public final NetworkMatcher campusNetwork;
    public final CampusLocation campusLocation;
    public final int riskPercent;
    public final boolean secureCookies;

    public App(AppConfig config) {
        this.config = config;
        this.zone = ZoneId.of(config.get("campus.zone", "Africa/Gaborone"));
        this.clock = buildClock(config, zone);
        PasswordHasher.setIterations(config.getInt("security.pbkdf2.iterations", PasswordHasher.DEFAULT_ITERATIONS));
        LocalTime open = config.getTime("campus.open", ScheduleService.ROOMS_OPEN);
        LocalTime close = config.getTime("campus.close", ScheduleService.ROOMS_CLOSE);
        this.riskPercent = config.getInt("attendance.risk.percent", 75);
        this.secureCookies = config.getBool("security.cookie.secure", false);

        this.db = new Database(config.get("db.url"), config.get("db.user"), config.get("db.password"),
                config.getInt("db.pool.size", 10));
        this.users = new UserDao(db);
        this.modules = new ModuleDao(db);
        this.campus = new CampusDao(db);
        this.timetable = new TimetableDao(db);
        this.attendanceDao = new AttendanceDao(db);
        this.announcementDao = new AnnouncementDao(db);
        this.coursework = new CourseworkDao(db);
        this.sites = new SiteDao(db);
        this.audit = new AuditDao(db);

        this.campusNetwork = new NetworkMatcher(config.getList("campus.networks"));
        String lat = config.get("campus.latitude");
        String lon = config.get("campus.longitude");
        this.campusLocation = new CampusLocation(lat.isEmpty() ? null : Double.valueOf(lat),
                lon.isEmpty() ? null : Double.valueOf(lon), config.getDouble("campus.radius.metres", 400));

        this.accounts = new AccountService(users, audit, clock, zone);
        this.schedule = new ScheduleService(timetable, campus, open, close);
        this.attendance = new AttendanceService(attendanceDao, modules, timetable, audit, clock, zone, campusNetwork,
                config.getInt("attendance.code.seconds", 30), config.getInt("attendance.session.minutes", 20),
                config.getInt("attendance.late.minutes", 15), config.getInt("attendance.flag.below", 60));
        this.announcements = new AnnouncementService(announcementDao, modules, audit, clock, zone);
        this.curriculum = new CurriculumService(modules, users, campus, timetable, coursework, sites, audit, clock,
                zone, open, close);
        this.planner = new StudyPlanService(coursework, timetable);
        this.assistant = new AssistantService();
        this.reports = new ReportService(attendanceDao, modules, timetable, campus, clock, zone, riskPercent, open,
                close);
        this.signInGuard = new SignInGuard(config.getInt("security.throttle.email.max", 5),
                config.getInt("security.throttle.ip.max", 20),
                config.getInt("security.throttle.global.per.minute", 300),
                Duration.ofMinutes(config.getInt("security.throttle.lock.minutes", 5)), clock);
        this.sso = new OidcClient(config);
    }

    /** Creates the tables and sample data if the database is empty. */
    public App start() {
        try (Connection c = db.get()) {
            if (!SchemaInstaller.isInstalled(c)) {
                LOG.info("Stride: creating database tables");
                SchemaInstaller.install(c);
                if (config.getBool("db.seed", true)) {
                    LOG.info("Stride: loading sample data");
                    new Seeder(this).run();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Stride could not prepare its database: " + e.getMessage(), e);
        }
        return this;
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static Clock buildClock(AppConfig config, ZoneId zone) {
        Clock system = Clock.system(zone);
        String fixed = config.get("clock.fixed");
        if (fixed.isEmpty()) {
            return system;
        }
        // Start the clock at the given moment and let it run from there (for demos and screenshots).
        LocalDateTime start = LocalDateTime.parse(fixed);
        Duration offset = Duration.between(LocalDateTime.now(system), start);
        return Clock.offset(system, offset);
    }

    @Override
    public void close() {
        db.close();
    }
}
