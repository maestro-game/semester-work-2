package server.models;

import java.nio.ByteBuffer;

public class Player extends User{
    public double x;
    public double y;
    public byte direction;
    public byte order;
    public byte score;
    public Room room;

    public Player(long id, byte[] name, byte[] password, double x, double y, byte order, Room room) {
        super(id, name, password);
        this.x = x;
        this.y = y;
        this.order = order;
        this.room = room;
    }

    public byte[] asBytes(){
        byte[] bytes = new byte[25];
        ByteBuffer.wrap(bytes).putLong(id).putDouble(x).putDouble(y).put(score);
        return bytes;
    }
}
