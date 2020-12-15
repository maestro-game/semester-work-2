package models;

public class Room {
    public final int id;
    public final String name;
    public int size;
    public final int capacity;
    public final boolean isSecured;

    public Room(int id, int capacity, int size, String name, boolean isSecured) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.capacity = capacity;
        this.isSecured = isSecured;
    }
}