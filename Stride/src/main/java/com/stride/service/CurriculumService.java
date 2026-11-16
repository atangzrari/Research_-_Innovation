package com.stride.service;

import com.stride.dao.AuditDao;
import com.stride.dao.CampusDao;
import com.stride.dao.CourseworkDao;
import com.stride.dao.ModuleDao;
import com.stride.dao.SiteDao;
import com.stride.dao.TimetableDao;
import com.stride.dao.UserDao;
import com.stride.model.Assessment;
import com.stride.model.Building;
import com.stride.model.CampusService;
import com.stride.model.Module;
import com.stride.model.Resource;
import com.stride.model.Role;
import com.stride.model.Room;
import com.stride.model.TimetableEntry;
import com.stride.model.User;

import java.net.URI;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Modules, enrolments, rooms, the timetable, coursework and the college-sites catalogue
 * (RQ21, RQ22, RQ23 data, RQ24 data, D3).
 */
public class CurriculumService {

    private final ModuleDao modules;
    private final UserDao users;
    private final CampusDao campus;
    private final TimetableDao timetable;
    private final CourseworkDao coursework;
    private final SiteDao sites;
    private final AuditDao audit;
    private final Clock clock;
    private final ZoneId zone;
    private final LocalTime open;
    private final LocalTime close;

    public CurriculumService(ModuleDao modules, UserDao users, CampusDao campus, TimetableDao timetable,
                             CourseworkDao coursework, SiteDao sites, AuditDao audit, Clock clock, ZoneId zone,
                             LocalTime open, LocalTime close) {
        this.modules = modules;
        this.users = users;
        this.campus = campus;
        this.timetable = timetable;
        this.coursework = coursework;
        this.sites = sites;
        this.audit = audit;
        this.clock = clock;
        this.zone = zone;
        this.open = open;
        this.close = close;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(zone));
    }

    // ------------------------------------------------------------------ modules (RQ21)

    public List<Module> allModules() {
        return modules.all();
    }

    public List<Module> modulesFor(User user) {
        switch (user.getRole()) {
            case STUDENT: return modules.forStudent(user.getUserId());
            case LECTURER: return modules.forLecturer(user.getUserId());
            default: return modules.all();
        }
    }

    public Optional<Module> module(String code) {
        return modules.findByCode(code);
    }

    public Optional<Module> module(int id) {
        return modules.find(id);
    }

    /** May this person open the module's page? Students: enrolled. Lecturers: any (to coordinate). */
    public boolean canView(User user, Module m) {
        return !user.isStudent() || modules.isEnrolled(user.getUserId(), m.getId());
    }

    public boolean teaches(User user, Module m) {
        return user.isLecturer() && m.getLecturerId() != null && m.getLecturerId() == user.getUserId();
    }

    public int createModule(User admin, String code, String name, String description, Integer lecturerId) {
        AccountService.requireAdmin(admin);
        String c = AccountService.required(code, "module code", 20).toUpperCase(Locale.ROOT);
        if (!c.matches("[A-Z]{2,5}\\d{3,4}[A-Z]?")) {
            throw new ServiceException("Module codes look like CSE305.");
        }
        if (modules.findByCode(c).isPresent()) {
            throw new ServiceException(c + " already exists.");
        }
        checkLecturer(lecturerId);
        int id = modules.create(c, AccountService.required(name, "module name", 120),
                AccountService.blankToNull(description), lecturerId);
        audit.log(now(), admin.getUserId(), "MODULE_CREATE", c, null);
        return id;
    }

    public void updateModule(User admin, int id, String name, String description, Integer lecturerId) {
        AccountService.requireAdmin(admin);
        Module m = modules.find(id).orElseThrow(() -> new ServiceException("That module does not exist."));
        checkLecturer(lecturerId);
        modules.update(id, m.getCode(), AccountService.required(name, "module name", 120),
                AccountService.blankToNull(description), lecturerId);
        audit.log(now(), admin.getUserId(), "MODULE_UPDATE", m.getCode() + " lecturer " + lecturerId, null);
    }

    private void checkLecturer(Integer lecturerId) {
        if (lecturerId != null) {
            User l = users.find(lecturerId).orElseThrow(() -> new ServiceException("That lecturer does not exist."));
            if (!l.isLecturer() || !l.isActive()) {
                throw new ServiceException("Choose an active lecturer account.");
            }
        }
    }

    public List<User> students(int moduleId) {
        return modules.students(moduleId);
    }

    /**
     * Enrols students listed one per line or separated by commas, by e-mail or student number.
     * Returns a summary of what happened to each.
     */
    public List<String> enrolMany(User admin, int moduleId, String list) {
        AccountService.requireAdmin(admin);
        Module m = modules.find(moduleId).orElseThrow(() -> new ServiceException("That module does not exist."));
        List<String> report = new ArrayList<>();
        int added = 0;
        for (String raw : (list == null ? "" : list).split("[,;\\s]+")) {
            String key = raw.trim();
            if (key.isEmpty()) continue;
            Optional<User> u = key.contains("@") ? users.findByEmail(AccountService.normalise(key))
                    : users.findByStudentNumber(key);
            if (u.isEmpty()) {
                report.add(key + ": no such student");
            } else if (!u.get().isStudent()) {
                report.add(key + ": not a student account");
            } else if (modules.enrol(u.get().getUserId(), moduleId, now())) {
                added++;
            } else {
                report.add(key + ": already enrolled");
            }
        }
        report.add(0, added + (added == 1 ? " student" : " students") + " enrolled in " + m.getCode() + ".");
        audit.log(now(), admin.getUserId(), "ENROL", m.getCode() + ": " + added + " added", null);
        return report;
    }

    public void unenrol(User admin, int moduleId, int studentId) {
        AccountService.requireAdmin(admin);
        modules.unenrol(studentId, moduleId);
        audit.log(now(), admin.getUserId(), "UNENROL", "student " + studentId + " from module " + moduleId, null);
    }

    public List<User> lecturers() {
        List<User> out = new ArrayList<>();
        for (User u : users.list(Role.LECTURER, null)) {
            if (u.isActive()) out.add(u);
        }
        return out;
    }

    // ------------------------------------------------------------------ rooms and map (RQ24)

    public List<Room> rooms() {
        return campus.rooms();
    }

    public List<Building> buildings() {
        return campus.buildings();
    }

    public void addRoom(User admin, String name, int buildingId, int capacity) {
        AccountService.requireAdmin(admin);
        String n = AccountService.required(name, "room name", 60);
        if (capacity < 1 || capacity > 2000) {
            throw new ServiceException("Seats must be between 1 and 2000.");
        }
        for (Room r : campus.rooms()) {
            if (r.getName().equalsIgnoreCase(n)) {
                throw new ServiceException("A room called " + n + " already exists.");
            }
        }
        campus.createRoom(n, buildingId, capacity);
        audit.log(now(), admin.getUserId(), "ROOM_CREATE", n, null);
    }

    // ------------------------------------------------------------------ timetable (RQ22)

    public List<TimetableEntry> timetable() {
        return timetable.all();
    }

    /** Problems that stop a new slot from being added; empty if it can be added. */
    public List<String> clashes(int moduleId, int roomId, DayOfWeek day, LocalTime start, LocalTime end) {
        List<String> problems = new ArrayList<>();
        if (start == null || end == null || !start.isBefore(end)) {
            problems.add("The class must end after it starts.");
            return problems;
        }
        if (start.isBefore(open) || end.isAfter(close)) {
            problems.add("Classes must be between " + open + " and " + close + ".");
        }
        Module m = modules.find(moduleId).orElse(null);
        Room room = campus.room(roomId).orElse(null);
        if (m == null || room == null) {
            problems.add("Choose a module and a room.");
            return problems;
        }
        TimetableEntry proposed = new TimetableEntry(0, moduleId, roomId, day, start, end, m.getCode(), m.getName(),
                room.getName());
        Set<Integer> mine = new HashSet<>();
        for (User s : modules.students(moduleId)) mine.add(s.getUserId());
        for (TimetableEntry e : timetable.all()) {
            if (!e.overlaps(proposed)) continue;
            String when = e.getDayLabel() + " " + e.getStartText() + "-" + e.getEndText();
            if (e.getRoomId() == roomId) {
                problems.add(room.getName() + " is already booked for " + e.getModuleCode() + " on " + when + ".");
            }
            if (e.getModuleId() == moduleId) {
                problems.add(m.getCode() + " already has a class on " + when + ".");
                continue;
            }
            Module other = modules.find(e.getModuleId()).orElse(null);
            if (other != null && m.getLecturerId() != null && m.getLecturerId().equals(other.getLecturerId())) {
                problems.add("The lecturer already teaches " + other.getCode() + " on " + when + ".");
            }
            int shared = 0;
            for (User s : modules.students(e.getModuleId())) {
                if (mine.contains(s.getUserId())) shared++;
            }
            if (shared > 0) {
                problems.add(shared + (shared == 1 ? " student is" : " students are") + " also in "
                        + e.getModuleCode() + " on " + when + ".");
            }
        }
        if (room.getCapacity() < modules.students(moduleId).size()) {
            problems.add(room.getName() + " seats " + room.getCapacity() + " but " + m.getCode() + " has "
                    + modules.students(moduleId).size() + " students.");
        }
        return problems;
    }

    public void addSlot(User admin, int moduleId, int roomId, DayOfWeek day, LocalTime start, LocalTime end) {
        AccountService.requireAdmin(admin);
        List<String> problems = clashes(moduleId, roomId, day, start, end);
        if (!problems.isEmpty()) {
            throw new ServiceException(String.join(" ", problems));
        }
        timetable.create(moduleId, roomId, day, start, end);
        audit.log(now(), admin.getUserId(), "TIMETABLE_ADD", "module " + moduleId + " " + day + " " + start, null);
    }

    public void removeSlot(User admin, int entryId) {
        AccountService.requireAdmin(admin);
        TimetableEntry e = timetable.find(entryId).orElseThrow(() -> new ServiceException("That slot does not exist."));
        timetable.delete(entryId);
        audit.log(now(), admin.getUserId(), "TIMETABLE_REMOVE",
                e.getModuleCode() + " " + e.getDayLabel() + " " + e.getStartText(), null);
    }

    // ------------------------------------------------------------------ coursework

    public List<Assessment> assessments(int moduleId) {
        return coursework.forModule(moduleId);
    }

    public List<Resource> resources(int moduleId) {
        return coursework.resources(moduleId);
    }

    public void addAssessment(User lecturer, Module m, String title, Assessment.Kind kind, LocalDateTime due,
                              int weight, Integer hours) {
        requireTeachesOrAdmin(lecturer, m);
        if (kind == null) throw new ServiceException("Choose the kind of assessment.");
        if (due == null) throw new ServiceException("Enter the due date and time.");
        if (weight < 0 || weight > 100) throw new ServiceException("Weight must be 0 to 100 per cent.");
        int h = hours == null ? kind.getDefaultHours() : hours;
        if (h < 1 || h > 200) throw new ServiceException("Study hours must be 1 to 200.");
        coursework.createAssessment(m.getId(), AccountService.required(title, "title", 160), kind, due, weight, h);
        audit.log(now(), lecturer.getUserId(), "ASSESSMENT_ADD", m.getCode() + ": " + title, null);
    }

    public void removeAssessment(User lecturer, Module m, int id) {
        requireTeachesOrAdmin(lecturer, m);
        coursework.deleteAssessment(id, m.getId());
    }

    public void addResource(User lecturer, Module m, String title, String url, String kind) {
        requireTeachesOrAdmin(lecturer, m);
        coursework.createResource(m.getId(), AccountService.required(title, "title", 160), safeUrl(url),
                kind == null || kind.isBlank() ? "Link" : kind.trim());
    }

    public void removeResource(User lecturer, Module m, int id) {
        requireTeachesOrAdmin(lecturer, m);
        coursework.deleteResource(id, m.getId());
    }

    private void requireTeachesOrAdmin(User user, Module m) {
        if (!(user.isAdministrator() || teaches(user, m))) {
            throw new ServiceException("You can only change modules you teach.");
        }
    }

    /** Only http and https links, so a stored link can never run script (javascript: URLs). */
    static String safeUrl(String url) {
        String u = AccountService.required(url, "web address", 500);
        try {
            URI uri = new URI(u);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("https") && !scheme.equals("http") || uri.getHost() == null) {
                throw new ServiceException("Links must start with https:// (or http://).");
            }
        } catch (java.net.URISyntaxException e) {
            throw new ServiceException("That web address is not valid.");
        }
        return u;
    }

    // ------------------------------------------------------------------ college sites (D3)

    public List<CampusService> sites() {
        return sites.all();
    }

    public void updateSite(User admin, String slug, String name, String task, String description, String keywords,
                           String url) {
        AccountService.requireAdmin(admin);
        CampusService s = sites.find(slug).orElseThrow(() -> new ServiceException("That site does not exist."));
        sites.update(slug, AccountService.required(name, "name", 60), AccountService.required(task, "task", 60),
                AccountService.required(description, "description", 300),
                keywords == null ? "" : keywords.trim(), safeUrl(url));
        audit.log(now(), admin.getUserId(), "SITE_UPDATE", s.getName() + " -> " + url, null);
    }
}
