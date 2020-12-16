package models;

public class Room {
    public final byte id;
    public String name;
    public byte size;
    public final byte capacity;
    public final boolean isSecured;

    public Room(byte id, byte size, byte capacity, String name, boolean isSecured) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.capacity = capacity;
        this.isSecured = isSecured;
    }

    @Override
    public String toString() {
        return name + (isSecured ? " (secured)" : " (free)") + "\n" + size + "/" + capacity;
    }
}