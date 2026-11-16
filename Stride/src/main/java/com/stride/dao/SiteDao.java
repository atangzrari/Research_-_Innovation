package com.stride.dao;

import com.stride.db.Database;
import com.stride.db.Jdbc;
import com.stride.model.CampusService;

import java.util.List;
import java.util.Optional;

/** The college sites catalogue (RQ05-RQ07), editable by an administrator. */
public class SiteDao extends Dao {

    public SiteDao(Database db) {
        super(db);
    }

    public List<CampusService> all() {
        return with(c -> Jdbc.query(c, "SELECT * FROM college_sites ORDER BY sort_order, name",
                rs -> new CampusService(rs.getString("slug"), rs.getString("name"), rs.getString("task"),
                        rs.getString("category"), rs.getString("description"), rs.getString("keywords"),
                        rs.getString("url"), rs.getInt("sort_order"))));
    }

    public Optional<CampusService> find(String slug) {
        return all().stream().filter(s -> s.getId().equals(slug)).findFirst();
    }

    public void update(String slug, String name, String task, String description, String keywords, String url) {
        with(c -> Jdbc.update(c, "UPDATE college_sites SET name = ?, task = ?, description = ?, keywords = ?, url = ? "
                + "WHERE slug = ?", name, task, description, keywords, url, slug));
    }

    public void insert(CampusService s) {
        with(c -> Jdbc.update(c, "INSERT INTO college_sites (slug, name, task, category, description, keywords, url, "
                        + "sort_order) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", s.getId(), s.getName(), s.getTask(),
                s.getCategory(), s.getDescription(), s.getKeywords(), s.getUrl(), s.getSortOrder()));
    }
}
