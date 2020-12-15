package server.models;

public class Player extends User{
    public double x;
    public double y;

    public Player(long id, byte[] name, byte[] password, double x, double y, Room room) {
        super(id, name, password);
    }
}
