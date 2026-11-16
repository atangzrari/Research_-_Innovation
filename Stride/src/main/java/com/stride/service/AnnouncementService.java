package com.stride.service;

import com.stride.dao.AnnouncementDao;
import com.stride.dao.AuditDao;
import com.stride.dao.ModuleDao;
import com.stride.model.Announcement;
import com.stride.model.Module;
import com.stride.model.User;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/** Notices (RQ10) and posting them (RQ19). */
public class AnnouncementService {

    private final AnnouncementDao announcements;
    private final ModuleDao modules;
    private final AuditDao audit;
    private final Clock clock;
    private final ZoneId zone;

    public AnnouncementService(AnnouncementDao announcements, ModuleDao modules, AuditDao audit, Clock clock,
                               ZoneId zone) {
        this.announcements = announcements;
        this.modules = modules;
        this.audit = audit;
        this.clock = clock;
        this.zone = zone;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(zone));
    }

    public List<Announcement> feed(User user) {
        return announcements.feedFor(user, 100);
    }

    public List<Announcement> forModule(User user, int moduleId) {
        return announcements.forModule(user, moduleId);
    }

    public int unread(User user) {
        return announcements.unreadCount(user);
    }

    public void markAllRead(User user) {
        announcements.markAllRead(user, now());
    }

    /**
     * Posts a notice. A lecturer may post only to a module they teach; an administrator may post
     * to any module or to everyone, and may pin a notice to the top.
     */
    public int post(User author, String title, String body, Integer moduleId, boolean pinned) {
        if (author == null || author.isStudent()) {
            throw new ServiceException("Only lecturers and administrators can post notices.");
        }
        String t = AccountService.required(title, "title", 160);
        String b = AccountService.required(body, "message", 4000);
        if (author.isLecturer()) {
            if (moduleId == null) {
                throw new ServiceException("Choose which of your modules the notice is for.");
            }
            Module m = modules.find(moduleId).orElseThrow(() -> new ServiceException("That module does not exist."));
            if (m.getLecturerId() == null || m.getLecturerId() != author.getUserId()) {
                throw new ServiceException("You can only post to modules you teach.");
            }
            pinned = false;
        } else if (moduleId != null && modules.find(moduleId).isEmpty()) {
            throw new ServiceException("That module does not exist.");
        }
        int id = announcements.create(t, b, author.getUserId(), moduleId, pinned, now());
        audit.log(now(), author.getUserId(), "NOTICE_POST", t, null);
        return id;
    }

    public void delete(User user, int id) {
        Integer authorId = announcements.authorOf(id).orElseThrow(() -> new ServiceException("That notice does not exist."));
        if (!user.isAdministrator() && authorId != user.getUserId()) {
            throw new ServiceException("You can only remove your own notices.");
        }
        announcements.delete(id);
        audit.log(now(), user.getUserId(), "NOTICE_DELETE", "Notice " + id, null);
    }
}
