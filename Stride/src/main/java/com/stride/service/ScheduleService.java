package com.stride.service;

import com.stride.dao.CampusDao;
import com.stride.dao.TimetableDao;
import com.stride.model.FreeRoom;
import com.stride.model.Room;
import com.stride.model.TimetableEntry;
import com.stride.model.TodayClass;
import com.stride.model.User;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Timetable rules: what is on today, and which rooms are free right now (RQ08, RQ09). */
public class ScheduleService {

    public static final LocalTime ROOMS_OPEN = LocalTime.of(7, 0);
    public static final LocalTime ROOMS_CLOSE = LocalTime.of(18, 0);
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final TimetableDao timetable;
    private final CampusDao campus;
    private final LocalTime open;
    private final LocalTime close;

    public ScheduleService(TimetableDao timetable, CampusDao campus, LocalTime open, LocalTime close) {
        this.timetable = timetable;
        this.campus = campus;
        this.open = open;
        this.close = close;
    }

    // ------------------------------------------------------------------ pure rules (unit-tested)

    /** Today's classes in time order, each marked finished, in progress or later. */
    public static List<TodayClass> classesOn(List<TimetableEntry> entries, DayOfWeek day, LocalTime now) {
        List<TodayClass> list = new ArrayList<>();
        for (TimetableEntry e : entries) {
            if (e.getDay() != day) continue;
            TodayClass.Status status = !now.isBefore(e.getEnd()) ? TodayClass.Status.DONE
                    : !now.isBefore(e.getStart()) ? TodayClass.Status.NOW
                    : TodayClass.Status.LATER;
            list.add(new TodayClass(e, status));
        }
        list.sort(Comparator.comparing(TodayClass::getStartText));
        return list;
    }

    public static boolean roomsOpen(LocalTime now) {
        return roomsOpen(now, ROOMS_OPEN, ROOMS_CLOSE);
    }

    public static boolean roomsOpen(LocalTime now, LocalTime open, LocalTime close) {
        return !now.isBefore(open) && now.isBefore(close);
    }

    public static List<FreeRoom> freeRooms(List<Room> rooms, List<TimetableEntry> entries, DayOfWeek day,
                                           LocalTime now) {
        return freeRooms(rooms, entries, day, now, ROOMS_OPEN, ROOMS_CLOSE);
    }

    /**
     * A room is free only when no timetable entry overlaps the current minute.
     * "Free until" is the start of its next booking today, or closing time (Algorithm 4.1).
     */
    public static List<FreeRoom> freeRooms(List<Room> rooms, List<TimetableEntry> entries, DayOfWeek day,
                                           LocalTime now, LocalTime open, LocalTime close) {
        List<FreeRoom> free = new ArrayList<>();
        if (!roomsOpen(now, open, close)) {
            return free;
        }
        LocalTime next = now.withSecond(0).withNano(0);
        for (Room room : rooms) {
            boolean busy = false;
            LocalTime nextStart = close;
            for (TimetableEntry e : entries) {
                if (!e.getRoom().equals(room.getName()) || e.getDay() != day) continue;
                if (e.overlaps(day, next, next.plusMinutes(1))) {
                    busy = true;
                    break;
                }
                if (e.getStart().isAfter(next) && e.getStart().isBefore(nextStart)) {
                    nextStart = e.getStart();
                }
            }
            if (!busy) {
                free.add(new FreeRoom(room.getName(), room.getBuilding(), room.getCapacity(),
                        nextStart.equals(close) ? "closing time" : HM.format(nextStart)));
            }
        }
        return free;
    }

    // ------------------------------------------------------------------ data-backed views

    /** The timetable a person should see: a student's enrolled modules, a lecturer's taught modules, or all. */
    public List<TimetableEntry> entriesFor(User user) {
        switch (user.getRole()) {
            case STUDENT:
                return timetable.forStudent(user.getUserId());
            case LECTURER:
                return timetable.forLecturer(user.getUserId());
            default:
                return timetable.all();
        }
    }

    public List<TodayClass> classesToday(User user, LocalDateTime now) {
        return classesOn(entriesFor(user), now.getDayOfWeek(), now.toLocalTime());
    }

    public List<FreeRoom> freeRoomsNow(LocalDateTime now) {
        return freeRooms(campus.rooms(), timetable.all(), now.getDayOfWeek(), now.toLocalTime(), open, close);
    }

    public boolean roomsOpenNow(LocalDateTime now) {
        return roomsOpen(now.toLocalTime(), open, close);
    }

    public LocalTime getOpen() { return open; }
    public LocalTime getClose() { return close; }

    /** Monday to Saturday, each with its entries in time order (Sunday only if it has classes). */
    public Map<DayOfWeek, List<TimetableEntry>> week(User user) {
        Map<DayOfWeek, List<TimetableEntry>> week = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            if (d != DayOfWeek.SUNDAY) {
                week.put(d, new ArrayList<>());
            }
        }
        for (TimetableEntry e : entriesFor(user)) {
            week.computeIfAbsent(e.getDay(), d -> new ArrayList<>()).add(e);
        }
        week.values().forEach(l -> l.sort(Comparator.comparing(TimetableEntry::getStart)));
        return week;
    }

    /** The next class that has not started yet, looking up to a week ahead; null if none. */
    public static TimetableEntry nextClass(List<TimetableEntry> entries, LocalDateTime now) {
        TimetableEntry best = null;
        long bestMinutes = Long.MAX_VALUE;
        for (TimetableEntry e : entries) {
            int dayGap = (e.getDay().getValue() - now.getDayOfWeek().getValue() + 7) % 7;
            LocalDateTime at = now.toLocalDate().plusDays(dayGap).atTime(e.getStart());
            if (!at.isAfter(now)) {
                at = at.plusDays(7);
            }
            long minutes = java.time.Duration.between(now, at).toMinutes();
            if (minutes < bestMinutes) {
                bestMinutes = minutes;
                best = e;
            }
        }
        return best;
    }
}
