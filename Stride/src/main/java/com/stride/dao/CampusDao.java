package com.stride.dao;

import com.stride.db.Database;
import com.stride.db.Jdbc;
import com.stride.model.Building;
import com.stride.model.Room;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Buildings and rooms. */
public class CampusDao extends Dao {

    private static final String ROOMS = "SELECT r.id, r.name, r.building_id, b.name AS building, r.capacity "
            + "FROM rooms r JOIN buildings b ON b.id = r.building_id ";

    public CampusDao(Database db) {
        super(db);
    }

    static Room mapRoom(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("id"), rs.getString("name"), rs.getInt("building_id"), rs.getString("building"),
                rs.getInt("capacity"));
    }

    public List<Room> rooms() {
        return with(c -> Jdbc.query(c, ROOMS + "ORDER BY r.name", CampusDao::mapRoom));
    }

    public Optional<Room> room(int id) {
        return with(c -> Jdbc.one(c, ROOMS + "WHERE r.id = ?", CampusDao::mapRoom, id));
    }

    public int createRoom(String name, int buildingId, int capacity) {
        return with(c -> Jdbc.insert(c, "INSERT INTO rooms (name, building_id, capacity) VALUES (?, ?, ?)",
                name, buildingId, capacity));
    }

    public void updateRoom(int id, String name, int buildingId, int capacity) {
        with(c -> Jdbc.update(c, "UPDATE rooms SET name = ?, building_id = ?, capacity = ? WHERE id = ?",
                name, buildingId, capacity, id));
    }

    /** Buildings in map order, each with its rooms. */
    public List<Building> buildings() {
        return with(c -> {
            Map<Integer, Building> byId = new LinkedHashMap<>();
            Jdbc.query(c, "SELECT * FROM buildings ORDER BY id", rs -> {
                Building b = new Building(rs.getInt("id"), rs.getString("name"), rs.getString("code"),
                        rs.getString("description"), rs.getString("access_note"), rs.getInt("map_x"),
                        rs.getInt("map_y"), rs.getInt("map_w"), rs.getInt("map_h"));
                byId.put(b.getId(), b);
                return b;
            });
            for (Room r : Jdbc.query(c, ROOMS + "ORDER BY r.name", CampusDao::mapRoom)) {
                Building b = byId.get(r.getBuildingId());
                if (b != null) {
                    b.getRooms().add(r);
                }
            }
            return new java.util.ArrayList<>(byId.values());
        });
    }
}
