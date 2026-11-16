package com.stride.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** One line of the audit log: who did what, and when. */
public class AuditEntry {
    private static final DateTimeFormatter AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final int id;
    private final LocalDateTime at;
    private final String actorName;
    private final String action;
    private final String detail;
    private final String clientIp;

    public AuditEntry(int id, LocalDateTime at, String actorName, String action, String detail, String clientIp) {
        this.id = id;
        this.at = at;
        this.actorName = actorName;
        this.action = action;
        this.detail = detail;
        this.clientIp = clientIp;
    }

    public int getId() { return id; }
    public LocalDateTime getAt() { return at; }
    public String getAtText() { return AT.format(at); }
    public String getActorName() { return actorName == null ? "System" : actorName; }
    public String getAction() { return action; }
    public String getDetail() { return detail; }
    public String getClientIp() { return clientIp; }
}
