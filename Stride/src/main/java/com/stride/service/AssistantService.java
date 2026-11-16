package com.stride.service;

import com.stride.model.Announcement;
import com.stride.model.Assessment;
import com.stride.model.AttendanceRecord;
import com.stride.model.Building;
import com.stride.model.FreeRoom;
import com.stride.model.Module;
import com.stride.model.Room;
import com.stride.model.TimetableEntry;
import com.stride.model.TodayClass;
import com.stride.model.User;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The study assistant (RQ25). It answers questions in plain language using ONLY the student's
 * own verified data in Stride: timetable, attendance, assessments, notices and rooms. It never
 * invents an answer: a question it cannot ground in that data gets an honest "I can't answer that"
 * and a list of things it can answer. Relevance is checked against a fixed set of test queries
 * (AssistantServiceTest).
 */
public class AssistantService {

    /** The answer, the data it came from, and follow-up questions to try. */
    public static final class Reply {
        private final String intent;
        private final List<String> lines;
        private final String source;
        private final List<String> suggestions;

        Reply(String intent, List<String> lines, String source, List<String> suggestions) {
            this.intent = intent;
            this.lines = lines;
            this.source = source;
            this.suggestions = suggestions;
        }

        public String getIntent() { return intent; }
        public List<String> getLines() { return lines; }
        public String getText() { return String.join(" ", lines); }
        public String getSource() { return source; }
        public List<String> getSuggestions() { return suggestions; }
    }

    /** Everything the assistant may look at for one student. Gathered by the caller. */
    public static final class Context {
        final User student;
        final LocalDateTime now;
        final List<TimetableEntry> timetable;
        final List<AttendanceRecord> attendance;
        final List<Assessment> assessments;
        final List<Announcement> notices;
        final List<FreeRoom> freeRooms;
        final List<Building> buildings;
        final List<Module> modules;
        final int riskPercent;

        public Context(User student, LocalDateTime now, List<TimetableEntry> timetable,
                       List<AttendanceRecord> attendance, List<Assessment> assessments, List<Announcement> notices,
                       List<FreeRoom> freeRooms, List<Building> buildings, List<Module> modules, int riskPercent) {
            this.student = student;
            this.now = now;
            this.timetable = timetable;
            this.attendance = attendance;
            this.assessments = assessments;
            this.notices = notices;
            this.freeRooms = freeRooms;
            this.buildings = buildings;
            this.modules = modules;
            this.riskPercent = riskPercent;
        }
    }

    public static final List<String> STARTERS = List.of(
            "What is my next class?",
            "What do I have today?",
            "What is my attendance in CSE305?",
            "Which subject has my lowest attendance?",
            "When is my next deadline?",
            "Is there a free room right now?",
            "Where is Lab 2?",
            "Any new notices?");

    public Reply answer(String question, Context ctx) {
        String q = normalise(question);
        if (q.isBlank()) {
            return help("Ask me about your classes, attendance, deadlines, notices or rooms.");
        }
        Module module = mentionedModule(q, ctx.modules);
        DayOfWeek day = mentionedDay(q, ctx.now);

        if (has(q, "where is", "where's", "how do i get to", "find room", "which building")) {
            Reply r = whereIs(q, ctx);
            if (r != null) return r;
        }
        if (has(q, "free room", "empty room", "room free", "rooms free", "free rooms", "study space",
                "where can i study", "quiet place", "available room")) {
            return freeRooms(ctx);
        }
        if (has(q, "check in", "checkin", "attendance code", "qr", "how do i sign in to class")) {
            return new Reply("checkin", List.of(
                    "When your lecturer opens attendance, a code and a QR image appear on the screen in class.",
                    "Scan the QR with your phone camera, or open Check in and type the six-character code.",
                    "Codes change every few seconds, so a photo sent to a friend will not work."),
                    "How Stride attendance works", STARTERS.subList(0, 3));
        }
        if (has(q, "attendance", "attended", "missed", "absent", "at risk", "percentage", "how many classes have i")) {
            return attendance(q, module, ctx);
        }
        if (has(q, "deadline", "due", "assignment", "exam", "test", "assessment", "coursework", "project",
                "submit", "hand in")) {
            return deadlines(module, ctx);
        }
        if (has(q, "notice", "announcement", "news", "message from")) {
            return notices(ctx);
        }
        if (has(q, "next class", "next lecture", "what's next", "whats next", "next lesson", "where do i go",
                "what is next")) {
            return nextClass(ctx);
        }
        if (day != null || has(q, "today", "timetable", "schedule", "classes", "lectures", "class")) {
            return classesOn(day == null ? ctx.now.getDayOfWeek() : day, ctx);
        }
        if (has(q, "hello", "hi", "hey", "good morning", "good afternoon", "good evening", "dumela")) {
            return help("Hello, " + ctx.student.getFirstName() + ".");
        }
        return help("I can only answer questions about your own Stride data, and I could not match that question.");
    }

    // ------------------------------------------------------------------ intents

    private Reply nextClass(Context ctx) {
        TimetableEntry next = ScheduleService.nextClass(ctx.timetable, ctx.now);
        if (next == null) {
            return new Reply("next-class", List.of("You have no classes on your timetable."), "Your timetable",
                    STARTERS.subList(3, 6));
        }
        String when = next.getDay() == ctx.now.getDayOfWeek() && next.getStart().isAfter(ctx.now.toLocalTime())
                ? "today" : "on " + next.getDayLabel();
        List<String> lines = new ArrayList<>();
        for (TodayClass c : ScheduleService.classesOn(ctx.timetable, ctx.now.getDayOfWeek(), ctx.now.toLocalTime())) {
            if (c.isNow()) {
                lines.add("Right now you are in " + c.getModuleName() + " (" + c.getModuleCode() + ") in "
                        + c.getRoom() + " until " + c.getEndText() + ".");
            }
        }
        lines.add("Your next class is " + next.getModuleName() + " (" + next.getModuleCode() + ") " + when + " at "
                + next.getStartText() + " in " + next.getRoom() + ".");
        return new Reply("next-class", lines, "Your timetable", List.of("Where is " + next.getRoom() + "?",
                "What do I have tomorrow?", "When is my next deadline?"));
    }

    private Reply classesOn(DayOfWeek day, Context ctx) {
        String dayName = day == ctx.now.getDayOfWeek() ? "today"
                : "on " + day.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        List<TimetableEntry> list = new ArrayList<>();
        for (TimetableEntry e : ctx.timetable) {
            if (e.getDay() == day) list.add(e);
        }
        list.sort(Comparator.comparing(TimetableEntry::getStart));
        if (list.isEmpty()) {
            return new Reply("day", List.of("You have no classes " + dayName + "."), "Your timetable",
                    List.of("What is my next class?", "Is there a free room right now?"));
        }
        List<String> lines = new ArrayList<>();
        lines.add("You have " + list.size() + (list.size() == 1 ? " class " : " classes ") + dayName + ":");
        for (TimetableEntry e : list) {
            lines.add(e.getStartText() + " to " + e.getEndText() + ", " + e.getModuleName() + " (" + e.getModuleCode()
                    + ") in " + e.getRoom() + ".");
        }
        return new Reply("day", lines, "Your timetable", List.of("What is my next class?", "Any new notices?"));
    }

    private Reply attendance(String q, Module module, Context ctx) {
        if (ctx.attendance.isEmpty()) {
            return new Reply("attendance", List.of("No classes have been held for your modules yet."),
                    "Your attendance records", STARTERS.subList(0, 2));
        }
        if (module != null) {
            for (AttendanceRecord r : ctx.attendance) {
                if (r.getModuleCode().equalsIgnoreCase(module.getCode())) {
                    List<String> lines = new ArrayList<>();
                    lines.add("Your attendance in " + r.getModuleName() + " (" + r.getModuleCode() + ") is "
                            + r.getPercent() + "%: " + r.getAttended() + " of " + r.getTotal() + " classes.");
                    if (r.isBelow(ctx.riskPercent)) {
                        lines.add("That is below " + ctx.riskPercent + "%. Try not to miss the next classes, and "
                                + "speak to your lecturer if something is stopping you from attending.");
                    }
                    return new Reply("attendance", lines, "Your attendance records",
                            List.of("Which subject has my lowest attendance?", "When is my next class?"));
                }
            }
        }
        List<AttendanceRecord> sorted = new ArrayList<>(ctx.attendance);
        sorted.sort(Comparator.comparingInt(AttendanceRecord::getPercent));
        int attended = 0;
        int total = 0;
        for (AttendanceRecord r : sorted) {
            attended += r.getAttended();
            total += r.getTotal();
        }
        int overall = total == 0 ? 0 : Math.round(100f * attended / total);
        List<String> lines = new ArrayList<>();
        if (has(q, "lowest", "worst", "least", "risk")) {
            AttendanceRecord low = sorted.get(0);
            lines.add("Your lowest attendance is " + low.getModuleName() + " (" + low.getModuleCode() + ") at "
                    + low.getPercent() + "%, " + low.getAttended() + " of " + low.getTotal() + " classes.");
        }
        lines.add("Overall you have attended " + attended + " of " + total + " classes (" + overall + "%).");
        List<String> risky = new ArrayList<>();
        for (AttendanceRecord r : sorted) {
            if (r.isBelow(ctx.riskPercent)) risky.add(r.getModuleCode() + " " + r.getPercent() + "%");
        }
        lines.add(risky.isEmpty() ? "No subject is below " + ctx.riskPercent + "%."
                : "Below " + ctx.riskPercent + "%: " + String.join(", ", risky) + ".");
        return new Reply("attendance", lines, "Your attendance records",
                List.of("What is my attendance in " + sorted.get(0).getModuleCode() + "?", "What is my next class?"));
    }

    private Reply deadlines(Module module, Context ctx) {
        List<Assessment> list = new ArrayList<>();
        for (Assessment a : ctx.assessments) {
            if (a.getDueAt().isAfter(ctx.now) && (module == null || a.getModuleId() == module.getId())) list.add(a);
        }
        list.sort(Comparator.comparing(Assessment::getDueAt));
        String scope = module == null ? "" : " for " + module.getCode();
        if (list.isEmpty()) {
            return new Reply("deadlines", List.of("You have no deadlines" + scope + " in the next three weeks."),
                    "Assessment dates from your lecturers", List.of("What is my next class?"));
        }
        List<String> lines = new ArrayList<>();
        Assessment first = list.get(0);
        long days = java.time.temporal.ChronoUnit.DAYS.between(ctx.now.toLocalDate(), first.getDueAt().toLocalDate());
        lines.add("Your next deadline" + scope + " is " + first.getModuleCode() + " " + first.getTitle() + " ("
                + first.getKind().getLabel().toLowerCase(Locale.ROOT) + ", " + first.getWeight() + "%), due "
                + first.getDueText() + (days == 0 ? ", today." : days == 1 ? ", tomorrow." : ", in " + days + " days."));
        if (list.size() > 1) {
            List<String> rest = new ArrayList<>();
            for (Assessment a : list.subList(1, Math.min(list.size(), 4))) {
                rest.add(a.getModuleCode() + " " + a.getTitle() + " (" + a.getDueText() + ")");
            }
            lines.add("After that: " + String.join("; ", rest) + ".");
        }
        lines.add("Open Study planner to see when to work on each one.");
        return new Reply("deadlines", lines, "Assessment dates from your lecturers",
                List.of("Which subject has my lowest attendance?", "Is there a free room right now?"));
    }

    private Reply notices(Context ctx) {
        List<Announcement> unread = new ArrayList<>();
        for (Announcement a : ctx.notices) {
            if (!a.isRead()) unread.add(a);
        }
        List<String> lines = new ArrayList<>();
        if (ctx.notices.isEmpty()) {
            lines.add("There are no notices for you.");
        } else if (unread.isEmpty()) {
            Announcement latest = ctx.notices.get(0);
            lines.add("No unread notices. The latest is \"" + latest.getTitle() + "\" from " + latest.getAuthor()
                    + " (" + latest.getPostedOn() + ").");
        } else {
            lines.add("You have " + unread.size() + " unread " + (unread.size() == 1 ? "notice" : "notices") + ":");
            for (Announcement a : unread.subList(0, Math.min(3, unread.size()))) {
                lines.add("\"" + a.getTitle() + "\" from " + a.getAuthor() + " (" + a.getAudience() + ", "
                        + a.getPostedOn() + ").");
            }
        }
        return new Reply("notices", lines, "Notices posted in Stride", List.of("When is my next deadline?"));
    }

    private Reply freeRooms(Context ctx) {
        if (ctx.freeRooms.isEmpty()) {
            return new Reply("free-rooms", List.of("No rooms are free right now. Rooms are listed between opening "
                    + "and closing time, and only when no class is booked in them."), "The published timetable",
                    List.of("What is my next class?"));
        }
        List<FreeRoom> rooms = new ArrayList<>(ctx.freeRooms);
        rooms.sort(Comparator.comparing(FreeRoom::getFreeUntil).reversed());
        List<String> names = new ArrayList<>();
        for (FreeRoom r : rooms.subList(0, Math.min(4, rooms.size()))) {
            names.add(r.getName() + " (" + r.getBuilding() + ", free until " + r.getFreeUntil() + ")");
        }
        return new Reply("free-rooms", List.of(rooms.size() + (rooms.size() == 1 ? " room is" : " rooms are")
                + " free right now: " + String.join("; ", names) + "."), "The published timetable",
                List.of("Where is " + rooms.get(0).getName() + "?", "What is my next class?"));
    }

    private Reply whereIs(String q, Context ctx) {
        for (Building b : ctx.buildings) {
            List<Room> rooms = new ArrayList<>(b.getRooms());
            rooms.sort(Comparator.comparingInt((Room r) -> r.getName().length()).reversed());
            for (Room r : rooms) {
                if (q.contains(r.getName().toLowerCase(Locale.ROOT))) {
                    List<String> lines = new ArrayList<>();
                    lines.add(r.getName() + " is in " + b.getName() + ". " + b.getDescription());
                    if (b.getAccessNote() != null) lines.add(b.getAccessNote());
                    return new Reply("where", lines, "The campus map", List.of("Is there a free room right now?"));
                }
            }
        }
        for (Building b : ctx.buildings) {
            if (q.contains(b.getName().toLowerCase(Locale.ROOT))) {
                return new Reply("where", List.of(b.getName() + ": " + b.getDescription()), "The campus map",
                        List.of("Is there a free room right now?"));
            }
        }
        return null;
    }

    private Reply help(String opening) {
        List<String> lines = new ArrayList<>();
        lines.add(opening);
        lines.add("I answer from your own Stride data only: your timetable, attendance, deadlines, notices and "
                + "the campus rooms. Try one of the questions below.");
        return new Reply("help", lines, null, STARTERS);
    }

    // ------------------------------------------------------------------ matching helpers

    /** Lower case, punctuation to spaces, padded with spaces so phrases match at word starts. */
    static String normalise(String question) {
        if (question == null) return " ";
        String t = question.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9' ]", " ").replaceAll("\\s+", " ").trim();
        return " " + t + " ";
    }

    /** True if any phrase starts at a word boundary, so "test" does not match inside "latest". */
    private static boolean has(String q, String... phrases) {
        for (String p : phrases) {
            if (q.contains(" " + p)) return true;
        }
        return false;
    }

    /** A module named by code ("cse305") or by a distinctive word of its name ("networks"). */
    static Module mentionedModule(String q, List<Module> modules) {
        String compact = q.replace(" ", "");
        for (Module m : modules) {
            if (compact.contains(m.getCode().toLowerCase(Locale.ROOT))) return m;
        }
        List<Module> byName = new ArrayList<>();
        for (Module m : modules) {
            for (String word : m.getName().toLowerCase(Locale.ROOT).split("[^a-z]+")) {
                if (word.length() >= 5 && !COMMON_WORDS.contains(word) && q.contains(word)) {
                    byName.add(m);
                    break;
                }
            }
        }
        return byName.size() == 1 ? byName.get(0) : null;
    }

    private static final List<String> COMMON_WORDS = Collections.unmodifiableList(List.of(
            "advanced", "systems", "computer", "development", "introduction", "principles", "with"));

    static DayOfWeek mentionedDay(String q, LocalDateTime now) {
        if (q.contains("tomorrow")) return now.getDayOfWeek().plus(1);
        if (q.contains("yesterday")) return now.getDayOfWeek().minus(1);
        for (DayOfWeek d : DayOfWeek.values()) {
            if (q.contains(d.getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase(Locale.ROOT))) return d;
        }
        return null;
    }
}
