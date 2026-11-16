package com.stride.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** A piece of assessed work with a due date; feeds the study planner (RQ23). */
public class Assessment {

    public enum Kind {
        ASSIGNMENT("Assignment", 8), TEST("Test", 6), EXAM("Exam", 12), PROJECT("Project", 15), LAB("Lab report", 4);

        private final String label;
        private final int defaultHours;

        Kind(String label, int defaultHours) {
            this.label = label;
            this.defaultHours = defaultHours;
        }

        public String getLabel() { return label; }
        public String getName() { return name(); }
        public int getDefaultHours() { return defaultHours; }
    }

    private static final DateTimeFormatter DUE = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.ENGLISH);

    private final int id;
    private final int moduleId;
    private final String moduleCode;
    private final String moduleName;
    private final String title;
    private final Kind kind;
    private final LocalDateTime dueAt;
    private final int weight;
    private final int estHours;

    public Assessment(int id, int moduleId, String moduleCode, String moduleName, String title, Kind kind,
                      LocalDateTime dueAt, int weight, int estHours) {
        this.id = id;
        this.moduleId = moduleId;
        this.moduleCode = moduleCode;
        this.moduleName = moduleName;
        this.title = title;
        this.kind = kind;
        this.dueAt = dueAt;
        this.weight = weight;
        this.estHours = estHours;
    }

    public int getId() { return id; }
    public int getModuleId() { return moduleId; }
    public String getModuleCode() { return moduleCode; }
    public String getModuleName() { return moduleName; }
    public String getTitle() { return title; }
    public Kind getKind() { return kind; }
    public LocalDateTime getDueAt() { return dueAt; }
    public String getDueText() { return DUE.format(dueAt); }
    public int getWeight() { return weight; }
    public int getEstHours() { return estHours; }
}
