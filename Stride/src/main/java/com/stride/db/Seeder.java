package com.stride.db;

import com.stride.config.App;
import com.stride.data.ServiceCatalogue;
import com.stride.model.Assessment;
import com.stride.model.CampusService;
import com.stride.model.CheckIn;
import com.stride.model.Role;
import com.stride.security.PasswordHasher;
import com.stride.service.ConfidenceScorer;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * SAMPLE data only: made-up people, a made-up campus layout and generated attendance. Nothing here
 * is a real student, lecturer or college record. Dates are relative to the day the database is
 * created, so the portal always has recent classes and upcoming deadlines to show.
 */
public final class Seeder {

    private static final String SWD = "CSE305";
    private static final String NET = "CSE310";
    private static final String EMB = "CSE315";
    private static final String SYS = "CSE302";
    private static final String RNI = "CSE206";

    /** Demo student's attendance, matching the figures in the project report (Figure 5.5). */
    private static final Map<String, int[]> DEMO_ATTENDANCE = new LinkedHashMap<>();

    static {
        DEMO_ATTENDANCE.put(RNI, new int[]{7, 8});
        DEMO_ATTENDANCE.put(SYS, new int[]{9, 10});
        DEMO_ATTENDANCE.put(SWD, new int[]{6, 8});
        DEMO_ATTENDANCE.put(NET, new int[]{10, 10});
        DEMO_ATTENDANCE.put(EMB, new int[]{5, 7});
    }

    private static final String[] FIRST = {"Tumelo", "Lesedi", "Kabo", "Onalenna", "Thato", "Boitumelo", "Kagiso",
            "Masego", "Tebogo", "Naledi", "Oratile", "Goitseone", "Refilwe", "Tshepo", "Amantle", "Kelebogile",
            "Mpho", "Onkemetse", "Gaone", "Keabetswe", "Lorato", "Pako", "Wame"};
    private static final String[] LAST = {"Molefe", "Kgosi", "Seretse", "Phiri", "Dube", "Motsumi", "Ntsima",
            "Rapula", "Sechele", "Tau", "Moeng", "Kebonang", "Modise", "Gabaake", "Mmolai", "Ramotswa", "Sebego",
            "Letsholo", "Mosweu", "Kgari", "Pilane", "Tlhako", "Bogatsu"};

    private final App app;
    private final Random random = new Random(20261029L);
    private final LocalDateTime now;

    public Seeder(App app) {
        this.app = app;
        this.now = app.now();
    }

    public void run() throws SQLException {
        app.db.inTransaction(c -> {
            seed(c);
            return null;
        });
    }

    private void seed(Connection c) throws SQLException {
        int rounds = PasswordHasher.getIterations();
        String studentHash = PasswordHasher.hash("student123", rounds);
        String lecturerHash = PasswordHasher.hash("lecturer123", rounds);
        String adminHash = PasswordHasher.hash("admin123", rounds);

        // ---------------------------------------------------------------- people
        int admin = user(c, "Demo", "Administrator", "admin@stride.test", Role.ADMINISTRATOR, null, adminHash);
        int registry = user(c, "Registry", "Office", "registry@stride.test", Role.ADMINISTRATOR, null, null);
        int library = user(c, "Library", "Desk", "library@stride.test", Role.ADMINISTRATOR, null, null);
        int support = user(c, "Student", "Support", "support@stride.test", Role.ADMINISTRATOR, null, null);
        int lecturer = user(c, "Demo", "Lecturer", "lecturer@stride.test", Role.LECTURER, null, lecturerHash);
        int lecturer2 = user(c, "Neo", "Mokoena", "neo.mokoena@stride.test", Role.LECTURER, null, lecturerHash);
        int lecturer3 = user(c, "Kagiso", "Sebina", "kagiso.sebina@stride.test", Role.LECTURER, null, lecturerHash);

        int demo = user(c, "Demo", "Student", "student@stride.test", Role.STUDENT, "CSE24-900", studentHash);
        List<Integer> students = new ArrayList<>();
        students.add(demo);
        for (int i = 0; i < FIRST.length; i++) {
            String email = FIRST[i].toLowerCase() + "." + LAST[i].toLowerCase() + "@stride.test";
            students.add(user(c, FIRST[i], LAST[i], email, Role.STUDENT, String.format("CSE24-%03d", 901 + i),
                    studentHash));
        }

        // ---------------------------------------------------------------- campus (schematic, not to scale)
        int main = building(c, "Main building", "MB", "Reception, Registry, ICT Support and the Lecture Hall.",
                "Step-free entrance at the front. Lift to every floor.", 40, 40, 280, 150);
        int blockA = building(c, "Block A", "BA", "Teaching rooms 101 and 102 on the ground floor.",
                "Step-free entrance from the courtyard.", 360, 40, 170, 110);
        int blockB = building(c, "Block B", "BB", "Teaching rooms 204 and 205 on the second floor.",
                "Lift beside the main stairs.", 570, 40, 150, 110);
        int labs = building(c, "Computer labs", "CL", "Labs 1 to 3. Students may use a lab when no class is booked.",
                "Step-free. Adjustable desks in Lab 3.", 360, 200, 200, 120);
        building(c, "Library", "LB", "Books, quiet study desks and help with the digital library.",
                "Step-free. Quiet floor upstairs, lift available.", 40, 240, 200, 140);
        building(c, "Student Centre", "SC", "Cafeteria, Student Support and the clinic.",
                "Step-free. Accessible toilets on the ground floor.", 590, 200, 150, 180);

        Map<String, Integer> rooms = new LinkedHashMap<>();
        rooms.put("Block A 101", room(c, "Block A 101", blockA, 60));
        rooms.put("Block A 102", room(c, "Block A 102", blockA, 60));
        rooms.put("Block B 204", room(c, "Block B 204", blockB, 40));
        rooms.put("Block B 205", room(c, "Block B 205", blockB, 40));
        rooms.put("Lab 1", room(c, "Lab 1", labs, 30));
        rooms.put("Lab 2", room(c, "Lab 2", labs, 30));
        rooms.put("Lab 3", room(c, "Lab 3", labs, 30));
        rooms.put("Lecture Hall", room(c, "Lecture Hall", main, 150));

        // ---------------------------------------------------------------- modules and enrolments
        Map<String, Integer> modules = new LinkedHashMap<>();
        modules.put(RNI, module(c, RNI, "Research and Innovation",
                "Plan, carry out and report a team research and innovation project.", lecturer3));
        modules.put(SYS, module(c, SYS, "Computer Systems Administration",
                "Install, secure and run Linux and Windows servers and the services on them.", lecturer));
        modules.put(SWD, module(c, SWD, "Advanced Web Development with Java",
                "Jakarta Servlets, JSP, sessions, security and databases for web applications.", lecturer));
        modules.put(NET, module(c, NET, "Computer Networks",
                "Addressing, routing, switching and network services, with hands-on labs.", lecturer2));
        modules.put(EMB, module(c, EMB, "Embedded Systems",
                "Microcontrollers, sensors and real-time programming.", lecturer2));

        Map<String, List<Integer>> enrolled = new LinkedHashMap<>();
        for (String code : modules.keySet()) {
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < students.size(); i++) {
                // Three students are repeating other modules and are not in Research and Innovation.
                if (code.equals(RNI) && i >= students.size() - 3) continue;
                Jdbc.update(c, "INSERT INTO enrolments (student_id, module_id, enrolled_at) VALUES (?, ?, ?)",
                        students.get(i), modules.get(code), now.minusWeeks(8));
                list.add(students.get(i));
            }
            enrolled.put(code, list);
        }

        // ---------------------------------------------------------------- timetable (as in the report)
        List<int[]> slots = new ArrayList<>();
        slot(c, slots, modules, rooms, DayOfWeek.MONDAY, "08:00", "10:00", SYS, "Block A 101");
        slot(c, slots, modules, rooms, DayOfWeek.MONDAY, "10:30", "12:30", SWD, "Lab 2");
        slot(c, slots, modules, rooms, DayOfWeek.MONDAY, "14:00", "16:00", RNI, "Block B 204");
        slot(c, slots, modules, rooms, DayOfWeek.TUESDAY, "08:00", "10:00", SWD, "Lab 2");
        slot(c, slots, modules, rooms, DayOfWeek.TUESDAY, "10:30", "12:30", NET, "Block A 102");
        slot(c, slots, modules, rooms, DayOfWeek.TUESDAY, "13:30", "15:30", EMB, "Lab 1");
        slot(c, slots, modules, rooms, DayOfWeek.WEDNESDAY, "09:00", "11:00", NET, "Block A 102");
        slot(c, slots, modules, rooms, DayOfWeek.WEDNESDAY, "11:30", "13:30", SYS, "Lab 3");
        slot(c, slots, modules, rooms, DayOfWeek.WEDNESDAY, "14:30", "16:30", RNI, "Block B 204");
        slot(c, slots, modules, rooms, DayOfWeek.THURSDAY, "08:00", "10:00", EMB, "Lab 1");
        slot(c, slots, modules, rooms, DayOfWeek.THURSDAY, "10:30", "12:30", SWD, "Lab 2");
        slot(c, slots, modules, rooms, DayOfWeek.THURSDAY, "14:00", "16:00", SYS, "Block A 101");
        slot(c, slots, modules, rooms, DayOfWeek.FRIDAY, "08:00", "10:00", RNI, "Block B 204");
        slot(c, slots, modules, rooms, DayOfWeek.FRIDAY, "10:30", "12:30", NET, "Block A 102");
        slot(c, slots, modules, rooms, DayOfWeek.FRIDAY, "13:00", "15:00", EMB, "Lab 1");
        slot(c, slots, modules, rooms, DayOfWeek.SATURDAY, "09:00", "11:00", SWD, "Lab 2");

        // ---------------------------------------------------------------- past attendance
        Map<Integer, Integer> lecturerOf = Map.of(modules.get(RNI), lecturer3, modules.get(SYS), lecturer,
                modules.get(SWD), lecturer, modules.get(NET), lecturer2, modules.get(EMB), lecturer2);
        double[] reliability = new double[students.size()];
        for (int i = 0; i < students.size(); i++) {
            reliability[i] = i == 0 ? 1 : i % 6 == 0 ? 0.55 + random.nextDouble() * 0.15 : 0.82 + random.nextDouble() * 0.17;
        }
        String[] devices = new String[students.size()];
        for (int i = 0; i < students.size(); i++) {
            byte[] b = new byte[16];
            random.nextBytes(b);
            devices[i] = HexFormat.of().formatHex(b);
        }
        ConfidenceScorer scorer = new ConfidenceScorer(true, app.config.getInt("attendance.flag.below", 60));
        for (Map.Entry<String, int[]> target : DEMO_ATTENDANCE.entrySet()) {
            int moduleId = modules.get(target.getKey());
            List<Occurrence> occurrences = pastOccurrences(slots, moduleId, target.getValue()[1]);
            Set<Integer> demoMisses = spreadMisses(occurrences.size(), occurrences.size() - target.getValue()[0]);
            int index = 0;
            for (Occurrence occ : occurrences) {
                LocalDateTime start = occ.start;
                int sessionId = AttendanceDaoAccess.insertSession(c, moduleId, occ.roomId, occ.entryId,
                        lecturerOf.get(moduleId), start.toLocalDate(), start, start.minusMinutes(2),
                        start.plusMinutes(20), start.plusMinutes(20), secret());
                for (int s = 0; s < students.size(); s++) {
                    int studentId = students.get(s);
                    if (!enrolled.get(target.getKey()).contains(studentId)) continue;
                    boolean attends = s == 0 ? !demoMisses.contains(index) : random.nextDouble() < reliability[s];
                    if (!attends) {
                        if (s != 0 && random.nextDouble() < 0.06) {
                            mark(c, sessionId, studentId, start.plusMinutes(30), CheckIn.Method.MANUAL,
                                    CheckIn.Status.EXCUSED, 100, "Set by lecturer", null, null, false);
                            Jdbc.update(c, "UPDATE check_ins SET correction_reason = ?, corrected_by = ?, corrected_at = ? "
                                            + "WHERE session_id = ? AND student_id = ?", "Medical certificate",
                                    lecturerOf.get(moduleId), start.plusDays(1), sessionId, studentId);
                        }
                        continue;
                    }
                    boolean qr = random.nextDouble() < 0.6;
                    boolean onCampus = s == 0 || random.nextDouble() < 0.93;
                    boolean late = s == 0 ? index == 1 : random.nextDouble() < 0.08;
                    int minute = late ? 16 + random.nextInt(4) : random.nextInt(12);
                    ConfidenceScorer.Score score = scorer.score(qr ? ConfidenceScorer.Token.QR : ConfidenceScorer.Token.CODE,
                            onCampus, false, ConfidenceScorer.Location.NOT_USED);
                    mark(c, sessionId, studentId, start.plusMinutes(minute).plusSeconds(random.nextInt(60)),
                            qr ? CheckIn.Method.QR : CheckIn.Method.CODE, late ? CheckIn.Status.LATE : CheckIn.Status.PRESENT,
                            score.getValue(), score.getFactorText(), onCampus ? "10.20.4." + (10 + s) : "41.75.3." + (20 + s),
                            devices[s], score.isFlagged());
                }
                index++;
            }
        }

        // ---------------------------------------------------------------- notices (D2 corrected)
        int n1 = notice(c, "Examination timetable published", "The examination timetable is now available. Arrive 30 "
                + "minutes before each paper and bring your student card.", registry, null, now.minusDays(1).withHour(9));
        notice(c, "Library hours extended for exams", "The library opens an hour earlier and closes an hour later "
                + "until the end of the examination period.", library, null, now.minusDays(2).withHour(11));
        int n3 = notice(c, "Thursday lab: bring your laptop", "The Thursday Advanced Web Development session is in "
                + "Lab 2 as usual. Bring your laptop and charger: we will deploy to Tomcat in class.", lecturer,
                modules.get(SWD), now.minusDays(3).withHour(14));
        int n4 = notice(c, "Careers evening for final-year students", "Meet employers and ask about internships. "
                + "Register at the Student Support desk in the Student Centre.", support, null, now.minusDays(6).withHour(10));
        notice(c, "Subnetting practice sheet", "A practice sheet for the subnetting test is on Moodle. Work through it "
                + "before Friday's lab.", lecturer2, modules.get(NET), now.minusDays(8).withHour(16));
        for (int id : new int[]{n3, n4}) {
            Jdbc.update(c, "INSERT INTO announcement_reads (user_id, announcement_id, read_at) VALUES (?, ?, ?)",
                    demo, id, now.minusDays(1));
        }
        Jdbc.update(c, "INSERT INTO announcement_reads (user_id, announcement_id, read_at) SELECT ?, id, ? FROM "
                + "announcements WHERE posted_at < ?", demo, now.minusDays(1), now.minusDays(7));
        if (n1 < 0) throw new IllegalStateException();

        // ---------------------------------------------------------------- assessments and resources
        LocalDate today = now.toLocalDate();
        assessment(c, modules.get(SWD), "Servlet portfolio", Assessment.Kind.ASSIGNMENT, today.plusDays(4).atTime(23, 59), 30, 6);
        assessment(c, modules.get(SYS), "Linux server lab report", Assessment.Kind.LAB, today.plusDays(5).atTime(17, 0), 15, 4);
        assessment(c, modules.get(NET), "Subnetting test", Assessment.Kind.TEST, today.plusDays(9).atTime(10, 30), 20, 6);
        assessment(c, modules.get(EMB), "Microcontroller project demo", Assessment.Kind.PROJECT, today.plusDays(13).atTime(14, 0), 40, 10);
        assessment(c, modules.get(RNI), "Final report", Assessment.Kind.PROJECT, today.plusDays(16).atTime(23, 59), 50, 10);
        assessment(c, modules.get(NET), "Lab 1 quiz", Assessment.Kind.TEST, today.minusDays(6).atTime(10, 30), 5, 2);
        assessment(c, modules.get(SWD), "Final examination", Assessment.Kind.EXAM, today.plusDays(34).atTime(9, 0), 40, 20);

        resource(c, modules.get(SWD), "Jakarta Servlet 6.1 specification", "https://jakarta.ee/specifications/servlet/6.1/", "Reference");
        resource(c, modules.get(SWD), "Apache Tomcat 11 documentation", "https://tomcat.apache.org/tomcat-11.0-doc/", "Reference");
        resource(c, modules.get(SWD), "OWASP Cheat Sheet Series", "https://cheatsheetseries.owasp.org/", "Security");
        resource(c, modules.get(SYS), "Ubuntu Server documentation", "https://documentation.ubuntu.com/server/", "Reference");
        resource(c, modules.get(SYS), "Microsoft Learn: Windows Server", "https://learn.microsoft.com/en-us/windows-server/", "Reference");
        resource(c, modules.get(NET), "RFC 791: Internet Protocol", "https://www.rfc-editor.org/rfc/rfc791", "Standard");
        resource(c, modules.get(NET), "RFC 4632: CIDR", "https://www.rfc-editor.org/rfc/rfc4632", "Standard");
        resource(c, modules.get(EMB), "Arduino language reference", "https://docs.arduino.cc/language-reference/", "Reference");
        resource(c, modules.get(RNI), "Digital library (EBSCO)", "https://research.ebsco.com/c/4blnds/search", "Library");
        for (Map.Entry<String, Integer> m : modules.entrySet()) {
            resource(c, m.getValue(), "Course page on Moodle", "https://moodle.bac.ac.bw/my/courses.php", "Moodle");
        }

        // ---------------------------------------------------------------- college sites
        for (CampusService s : ServiceCatalogue.all()) {
            Jdbc.update(c, "INSERT INTO college_sites (slug, name, task, category, description, keywords, url, sort_order) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", s.getId(), s.getName(), s.getTask(), s.getCategory(),
                    s.getDescription(), s.getKeywords(), s.getUrl(), s.getSortOrder());
        }

        Jdbc.update(c, "INSERT INTO audit_log (logged_at, actor_id, action, detail, client_ip) VALUES (?, NULL, ?, ?, NULL)",
                now, "SEED", "Sample data loaded: " + students.size() + " students, 3 lecturers, 5 modules");
        if (admin < 0) throw new IllegalStateException();
    }

    // ------------------------------------------------------------------ helpers

    private int user(Connection c, String first, String last, String email, Role role, String number, String hash)
            throws SQLException {
        return Jdbc.insert(c, "INSERT INTO users (first_name, last_name, email, role, student_number, password_hash, "
                + "must_change_password, active, created_at, last_login_at) VALUES (?, ?, ?, ?, ?, ?, FALSE, TRUE, ?, NULL)",
                first, last, email, role, number, hash, now.minusWeeks(9));
    }

    private static int building(Connection c, String name, String code, String description, String access,
                                int x, int y, int w, int h) throws SQLException {
        return Jdbc.insert(c, "INSERT INTO buildings (name, code, description, access_note, map_x, map_y, map_w, map_h) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", name, code, description, access, x, y, w, h);
    }

    private static int room(Connection c, String name, int buildingId, int capacity) throws SQLException {
        return Jdbc.insert(c, "INSERT INTO rooms (name, building_id, capacity) VALUES (?, ?, ?)", name, buildingId, capacity);
    }

    private static int module(Connection c, String code, String name, String description, int lecturerId)
            throws SQLException {
        return Jdbc.insert(c, "INSERT INTO modules (code, name, description, lecturer_id) VALUES (?, ?, ?, ?)",
                code, name, description, lecturerId);
    }

    /** Records each slot as {entryId, moduleId, roomId, day, startSecond, endSecond}. */
    private static void slot(Connection c, List<int[]> slots, Map<String, Integer> modules, Map<String, Integer> rooms,
                             DayOfWeek day, String start, String end, String code, String room) throws SQLException {
        LocalTime s = LocalTime.parse(start);
        LocalTime e = LocalTime.parse(end);
        int id = Jdbc.insert(c, "INSERT INTO timetable_entries (module_id, room_id, day_of_week, start_time, end_time) "
                + "VALUES (?, ?, ?, ?, ?)", modules.get(code), rooms.get(room), day.getValue(), s, e);
        slots.add(new int[]{id, modules.get(code), rooms.get(room), day.getValue(), s.toSecondOfDay(), e.toSecondOfDay()});
    }

    /** One past class: when it started, and which timetable slot and room it was. */
    private static final class Occurrence {
        final LocalDateTime start;
        final int entryId;
        final int roomId;

        Occurrence(LocalDateTime start, int entryId, int roomId) {
            this.start = start;
            this.entryId = entryId;
            this.roomId = roomId;
        }
    }

    /** The most recent {@code count} finished classes of a module, oldest first. */
    private List<Occurrence> pastOccurrences(List<int[]> slots, int moduleId, int count) {
        List<Occurrence> found = new ArrayList<>();
        LocalDate day = now.toLocalDate();
        while (found.size() < count) {
            for (int i = slots.size() - 1; i >= 0 && found.size() < count; i--) {
                int[] s = slots.get(i);
                if (s[1] != moduleId || s[3] != day.getDayOfWeek().getValue()) continue;
                LocalDateTime start = day.atTime(LocalTime.ofSecondOfDay(s[4]));
                LocalDateTime end = day.atTime(LocalTime.ofSecondOfDay(s[5]));
                if (!end.isAfter(now)) {
                    found.add(new Occurrence(start, s[0], s[2]));
                }
            }
            day = day.minusDays(1);
        }
        List<Occurrence> oldestFirst = new ArrayList<>();
        for (int i = found.size() - 1; i >= 0; i--) oldestFirst.add(found.get(i));
        return oldestFirst;
    }

    /** Spreads {@code misses} absences evenly over {@code total} sessions, never the very first. */
    static Set<Integer> spreadMisses(int total, int misses) {
        Set<Integer> out = new HashSet<>();
        for (int k = 1; k <= misses; k++) {
            out.add(Math.min(total - 1, (int) Math.round(k * (double) total / (misses + 1))));
        }
        return out;
    }

    private static void mark(Connection c, int sessionId, int studentId, LocalDateTime at, CheckIn.Method method,
                             CheckIn.Status status, int confidence, String factors, String ip, String device,
                             boolean flagged) throws SQLException {
        Jdbc.update(c, "INSERT INTO check_ins (session_id, student_id, checked_in_at, method, status, confidence, "
                        + "factors, client_ip, device_hash, flagged) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                sessionId, studentId, at, method, status, confidence, factors, ip, device, flagged);
    }

    private static int notice(Connection c, String title, String body, int author, Integer module, LocalDateTime at)
            throws SQLException {
        return Jdbc.insert(c, "INSERT INTO announcements (title, body, author_id, module_id, pinned, posted_at) "
                + "VALUES (?, ?, ?, ?, FALSE, ?)", title, body, author, module, at.withMinute(0).withSecond(0).withNano(0));
    }

    private static void assessment(Connection c, int module, String title, Assessment.Kind kind, LocalDateTime due,
                                   int weight, int hours) throws SQLException {
        Jdbc.update(c, "INSERT INTO assessments (module_id, title, kind, due_at, weight, est_hours) VALUES (?, ?, ?, ?, ?, ?)",
                module, title, kind, due, weight, hours);
    }

    private static void resource(Connection c, int module, String title, String url, String kind) throws SQLException {
        Jdbc.update(c, "INSERT INTO resources (module_id, title, url, kind) VALUES (?, ?, ?, ?)", module, title, url, kind);
    }

    private String secret() {
        byte[] b = new byte[24];
        random.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    /** Bridge to the package-private insert in AttendanceDao (same SQL, same connection/transaction). */
    private static final class AttendanceDaoAccess {
        static int insertSession(Connection c, int moduleId, Integer roomId, Integer entryId, int lecturerId,
                                 LocalDate date, LocalDateTime startsAt, LocalDateTime openedAt, LocalDateTime closesAt,
                                 LocalDateTime closedAt, String secret) throws SQLException {
            return Jdbc.insert(c, "INSERT INTO attendance_sessions (module_id, room_id, timetable_entry_id, lecturer_id, "
                            + "session_date, starts_at, opened_at, closes_at, closed_at, secret) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    moduleId, roomId, entryId, lecturerId, date, startsAt, openedAt, closesAt, closedAt, secret);
        }
    }
}
