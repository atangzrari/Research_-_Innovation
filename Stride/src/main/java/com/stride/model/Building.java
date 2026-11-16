package com.stride.model;

import java.util.ArrayList;
import java.util.List;

/** A campus building, with its position on the schematic campus map (RQ24). */
public class Building {
    private final int id;
    private final String name;
    private final String code;
    private final String description;
    private final String accessNote;
    private final int mapX;
    private final int mapY;
    private final int mapW;
    private final int mapH;
    private final List<Room> rooms = new ArrayList<>();
    private int freeNow;

    public Building(int id, String name, String code, String description, String accessNote,
                    int mapX, int mapY, int mapW, int mapH) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.accessNote = accessNote;
        this.mapX = mapX;
        this.mapY = mapY;
        this.mapW = mapW;
        this.mapH = mapH;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public String getAccessNote() { return accessNote; }
    public int getMapX() { return mapX; }
    public int getMapY() { return mapY; }
    public int getMapW() { return mapW; }
    public int getMapH() { return mapH; }
    /** Centre of the building on the map, for its label. */
    public int getLabelX() { return mapX + mapW / 2; }
    public int getLabelY() { return mapY + mapH / 2; }
    public List<Room> getRooms() { return rooms; }
    public int getFreeNow() { return freeNow; }
    public void setFreeNow(int freeNow) { this.freeNow = freeNow; }
    public String getAnchor() { return "b-" + code.toLowerCase(); }
}
