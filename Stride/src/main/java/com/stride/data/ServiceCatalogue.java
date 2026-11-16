package com.stride.data;

import com.stride.model.CampusService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The college sites Stride unifies, and the search over them (RQ05-RQ07).
 * The list below seeds the database; after that an administrator edits the sites in Stride.
 */
public final class ServiceCatalogue {

    private static final List<CampusService> SERVICES = List.of(
            new CampusService("gradebook", "Gradebook", "Check my results", "Academic record",
                    "Your results, registrations, attendance, letters and statements.",
                    "grades marks results transcript registration attendance statement letters policies profile",
                    "https://gradebook.bac.ac.bw/yame/dashboard", 1),
            new CampusService("moodle", "Moodle", "Open my courses", "Learning",
                    "Your courses, notes, activities and course announcements.",
                    "courses lectures notes assignments class material learning",
                    "https://moodle.bac.ac.bw/my/courses.php", 2),
            new CampusService("timetable", "PrimeTimetable", "See the full timetable", "Planning",
                    "The published teaching timetable for every programme and year.",
                    "timetable schedule classes lectures rooms venue calendar",
                    "https://primetimetable.com/publish/?id=8773fc8b-11d4-4bd1-92df-e8886bdf9fd5"
                            + "#id=8773fc8b-11d4-4bd1-92df-e8886bdf9fd5"
                            + "&groupId=4514b2fc-c797-4428-bfd7-31a809a9e1b3&view=1", 3),
            new CampusService("library", "Digital library", "Find a book or article", "Research",
                    "Search articles, books and journals the college subscribes to.",
                    "books articles journals ebsco research papers references",
                    "https://research.ebsco.com/c/4blnds/search", 4),
            new CampusService("thuto", "ThutoAcademics", "Apply to study", "Admissions",
                    "Apply to a programme and follow the progress of your application.",
                    "apply application admission enrol enrolment programme",
                    "https://thitoacademics.bac.ac.bw/", 5));

    private ServiceCatalogue() {
    }

    /** The default sites (used to seed the database and by tests). */
    public static List<CampusService> all() {
        return SERVICES;
    }

    public static List<CampusService> search(String query) {
        return search(SERVICES, query);
    }

    /** Every word typed must appear in the name, task, category, description or keywords. */
    public static List<CampusService> search(List<CampusService> services, String query) {
        if (query == null || query.isBlank()) {
            return services;
        }
        String[] terms = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        List<CampusService> hits = new ArrayList<>();
        for (CampusService s : services) {
            String hay = (s.getName() + " " + s.getTask() + " " + s.getCategory() + " "
                    + s.getDescription() + " " + s.getKeywords()).toLowerCase(Locale.ROOT);
            boolean all = true;
            for (String t : terms) {
                if (!hay.contains(t)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                hits.add(s);
            }
        }
        return hits;
    }
}
