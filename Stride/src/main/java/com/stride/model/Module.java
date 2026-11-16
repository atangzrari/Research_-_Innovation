package com.stride.model;

/** A taught module (course), e.g. CSE305 Advanced Web Development with Java. */
public class Module {
    private final int id;
    private final String code;
    private final String name;
    private final String description;
    private final Integer lecturerId;
    private final String lecturerName;
    private final int studentCount;

    public Module(int id, String code, String name, String description,
                  Integer lecturerId, String lecturerName, int studentCount) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.lecturerId = lecturerId;
        this.lecturerName = lecturerName;
        this.studentCount = studentCount;
    }

    public int getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Integer getLecturerId() { return lecturerId; }
    public String getLecturerName() { return lecturerName; }
    public int getStudentCount() { return studentCount; }
    public String getLabel() { return code + " " + name; }
}
