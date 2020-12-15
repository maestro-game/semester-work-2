package server.models;

public class User {
    public final long id;
    public final byte[] name;
    public final byte[] password;

    public User(long id, byte[] name, byte[] password) {
        this.id = id;
        this.name = name;
        this.password = password;
    }

    public Player toPlayer(double x, double y, Room room) {
        return new Player(id, name, password, x, y, room);
    }
}
