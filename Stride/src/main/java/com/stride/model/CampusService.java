package com.stride.model;

/** One of the college's existing sites that Stride links to. */
public class CampusService {

    private final String id;
    private final String name;
    private final String task;
    private final String category;
    private final String description;
    private final String keywords;
    private final String url;
    private final int sortOrder;

    public CampusService(String id, String name, String task, String category,
                         String description, String keywords, String url) {
        this(id, name, task, category, description, keywords, url, 0);
    }

    public CampusService(String id, String name, String task, String category,
                         String description, String keywords, String url, int sortOrder) {
        this.id = id;
        this.name = name;
        this.task = task;
        this.category = category;
        this.description = description;
        this.keywords = keywords;
        this.url = url;
        this.sortOrder = sortOrder;
    }

    public String getId() { return id; }
    /** The site's own name, e.g. "Gradebook". */
    public String getName() { return name; }
    /** What the person wants to do, in plain words, e.g. "Check my results". */
    public String getTask() { return task; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    /** Extra words people might type (e.g. "grades" for Gradebook); used by search only. */
    public String getKeywords() { return keywords; }
    public String getUrl() { return url; }
    public int getSortOrder() { return sortOrder; }
}
