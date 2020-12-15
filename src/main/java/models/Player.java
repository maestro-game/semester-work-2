package models;

public class Player {
    public final long id;
    public final String username;
    public double x;
    public double y;

    public Player(long id, String username, double x, double y) {
        this.id = id;
        this.username = username;
        this.x = x;
        this.y = y;
    }
}
