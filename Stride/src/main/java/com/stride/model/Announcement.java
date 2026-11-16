package com.stride.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Announcement {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

    private final int id;
    private final String title;
    private final String body;
    private final String author;
    private final String authorRole;
    private final String moduleCode;
    private final boolean pinned;
    private final LocalDateTime postedAt;
    private final boolean read;

    public Announcement(int id, String title, String body, String author, String authorRole, String moduleCode,
                        boolean pinned, LocalDateTime postedAt, boolean read) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.author = author;
        this.authorRole = authorRole;
        this.moduleCode = moduleCode;
        this.pinned = pinned;
        this.postedAt = postedAt;
        this.read = read;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getAuthor() { return author; }
    public String getAuthorRole() { return authorRole; }
    public String getAuthorInitial() { return author == null || author.isEmpty() ? "?" : author.substring(0, 1); }
    public String getModuleCode() { return moduleCode; }
    /** Who the notice is for: a module code, or everyone. */
    public String getAudience() { return moduleCode == null ? "Everyone" : moduleCode; }
    public boolean isPinned() { return pinned; }
    public LocalDateTime getPostedAt() { return postedAt; }
    public String getPostedOn() { return DATE.format(postedAt); }
    public String getPostedAtText() { return DATE_TIME.format(postedAt); }
    public boolean isRead() { return read; }
}
