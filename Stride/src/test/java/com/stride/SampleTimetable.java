package com.stride;

import com.stride.model.Room;
import com.stride.model.TimetableEntry;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/** The sample rooms and timetable from the report (Section 5.2), for database-free unit tests. */
public final class SampleTimetable {

    public static final List<Room> ROOMS = List.of(
            new Room("Block A 101", "Block A", 60), new Room("Block A 102", "Block A", 60),
            new Room("Block B 204", "Block B", 40), new Room("Block B 205", "Block B", 40),
            new Room("Lab 1", "Computer labs", 30), new Room("Lab 2", "Computer labs", 30),
            new Room("Lab 3", "Computer labs", 30), new Room("Lecture Hall", "Main building", 150));

    public static final List<TimetableEntry> ENTRIES = List.of(
            e(DayOfWeek.MONDAY, "08:00", "10:00", "CSE302", "Computer Systems Administration", "Block A 101"),
            e(DayOfWeek.MONDAY, "10:30", "12:30", "CSE305", "Advanced Web Development with Java", "Lab 2"),
            e(DayOfWeek.MONDAY, "14:00", "16:00", "CSE206", "Research and Innovation", "Block B 204"),
            e(DayOfWeek.TUESDAY, "08:00", "10:00", "CSE305", "Advanced Web Development with Java", "Lab 2"),
            e(DayOfWeek.TUESDAY, "10:30", "12:30", "CSE310", "Computer Networks", "Block A 102"),
            e(DayOfWeek.TUESDAY, "13:30", "15:30", "CSE315", "Embedded Systems", "Lab 1"),
            e(DayOfWeek.WEDNESDAY, "09:00", "11:00", "CSE310", "Computer Networks", "Block A 102"),
            e(DayOfWeek.WEDNESDAY, "11:30", "13:30", "CSE302", "Computer Systems Administration", "Lab 3"),
            e(DayOfWeek.WEDNESDAY, "14:30", "16:30", "CSE206", "Research and Innovation", "Block B 204"),
            e(DayOfWeek.THURSDAY, "08:00", "10:00", "CSE315", "Embedded Systems", "Lab 1"),
            e(DayOfWeek.THURSDAY, "10:30", "12:30", "CSE305", "Advanced Web Development with Java", "Lab 2"),
            e(DayOfWeek.THURSDAY, "14:00", "16:00", "CSE302", "Computer Systems Administration", "Block A 101"),
            e(DayOfWeek.FRIDAY, "08:00", "10:00", "CSE206", "Research and Innovation", "Block B 204"),
            e(DayOfWeek.FRIDAY, "10:30", "12:30", "CSE310", "Computer Networks", "Block A 102"),
            e(DayOfWeek.FRIDAY, "13:00", "15:00", "CSE315", "Embedded Systems", "Lab 1"),
            e(DayOfWeek.SATURDAY, "09:00", "11:00", "CSE305", "Advanced Web Development with Java", "Lab 2"));

    private SampleTimetable() {
    }

    private static TimetableEntry e(DayOfWeek d, String s, String en, String code, String name, String room) {
        return new TimetableEntry(d, LocalTime.parse(s), LocalTime.parse(en), code, name, room);
    }
}
