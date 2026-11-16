package com.stride.model;

public class Room {
    private final int id;
    private final String name;
    private final String building;
    private final int buildingId;
    private final int capacity;

    public Room(String name, String building, int capacity) {
        this(0, name, 0, building, capacity);
    }

    public Room(int id, String name, int buildingId, String building, int capacity) {
        this.id = id;
        this.name = name;
        this.buildingId = buildingId;
        this.building = building;
        this.capacity = capacity;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getBuilding() { return building; }
    public int getBuildingId() { return buildingId; }
    public int getCapacity() { return capacity; }
}
