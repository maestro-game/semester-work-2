package models;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.shape.Circle;

import java.util.Comparator;
import java.util.LinkedList;

public class Player {
    public static final ObservableList<Player> leader = FXCollections.observableList(new LinkedList<>());

    public final long id;
    public double x;
    public double y;
    private byte score;
    public Circle circle;

    public byte getScore() {
        return score;
    }

    public void setScore(byte score) {
        if (this.score != score) {
            this.score = score;
            Platform.runLater(() -> leader.sort(Comparator.comparingInt(Player::getScore)));
        }
    }

    public Player(long id, double x, double y, byte score) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.score = score;
        Platform.runLater(() -> {
            leader.add(this);
            leader.sort(Comparator.comparingInt(Player::getScore));
        });
    }

    @Override
    public String toString() {
        return id + " " + score;
    }
}
