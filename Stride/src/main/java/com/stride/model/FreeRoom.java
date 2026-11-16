package com.stride.model;

public class FreeRoom {
    private final String name;
    private final String building;
    private final int capacity;
    private final String freeUntil;

    public FreeRoom(String name, String building, int capacity, String freeUntil) {
        this.name = name;
        this.building = building;
        this.capacity = capacity;
        this.freeUntil = freeUntil;
    }

    public String getName() { return name; }
    public String getBuilding() { return building; }
    public int getCapacity() { return capacity; }
    /** "14:00", or "closing time" when nothing else is booked today. */
    public String getFreeUntil() { return freeUntil; }
}
