package com.stride.model;

/** A link to learning material for a module (RQ24). */
public class Resource {
    private final int id;
    private final int moduleId;
    private final String title;
    private final String url;
    private final String kind;

    public Resource(int id, int moduleId, String title, String url, String kind) {
        this.id = id;
        this.moduleId = moduleId;
        this.title = title;
        this.url = url;
        this.kind = kind;
    }

    public int getId() { return id; }
    public int getModuleId() { return moduleId; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getKind() { return kind; }
}
