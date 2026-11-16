package com.stride.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/** One scheduled teaching slot: a module in a room at a time on a weekday. */
public class TimetableEntry {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final int id;
    private final int moduleId;
    private final int roomId;
    private final DayOfWeek day;
    private final LocalTime start;
    private final LocalTime end;
    private final String moduleCode;
    private final String moduleName;
    private final String room;

    public TimetableEntry(DayOfWeek day, LocalTime start, LocalTime end,
                          String moduleCode, String moduleName, String room) {
        this(0, 0, 0, day, start, end, moduleCode, moduleName, room);
    }

    public TimetableEntry(int id, int moduleId, int roomId, DayOfWeek day, LocalTime start, LocalTime end,
                          String moduleCode, String moduleName, String room) {
        this.id = id;
        this.moduleId = moduleId;
        this.roomId = roomId;
        this.day = day;
        this.start = start;
        this.end = end;
        this.moduleCode = moduleCode;
        this.moduleName = moduleName;
        this.room = room;
    }

    public int getId() { return id; }
    public int getModuleId() { return moduleId; }
    public int getRoomId() { return roomId; }
    public DayOfWeek getDay() { return day; }
    public LocalTime getStart() { return start; }
    public LocalTime getEnd() { return end; }
    public String getModuleCode() { return moduleCode; }
    public String getModuleName() { return moduleName; }
    public String getRoom() { return room; }
    public String getStartText() { return HM.format(start); }
    public String getEndText() { return HM.format(end); }
    public String getDayLabel() { return day.getDisplayName(TextStyle.FULL, Locale.ENGLISH); }

    /** True if this slot is on {@code d} and overlaps the interval [from, to). */
    public boolean overlaps(DayOfWeek d, LocalTime from, LocalTime to) {
        return day == d && start.isBefore(to) && from.isBefore(end);
    }

    /** True if both slots share a weekday and their half-open intervals overlap. */
    public boolean overlaps(TimetableEntry other) {
        return overlaps(other.day, other.start, other.end);
    }
}
