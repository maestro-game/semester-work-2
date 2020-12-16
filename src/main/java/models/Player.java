package models;

import javafx.scene.shape.Circle;

public class Player {
    public final long id;
    public double x;
    public double y;
    public byte score;
    public Circle circle;

    public Player(long id, double x, double y, byte score, Circle circle) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.score = score;
        this.circle = circle;
    }
}
