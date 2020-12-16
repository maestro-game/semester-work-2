package controllers;

import javafx.concurrent.Task;
import javafx.fxml.Initializable;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import models.Player;
import models.SignalCode;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;

import static main.Client.*;

public class GameController implements Initializable {
    public static final int RADIUS = 10;

    public Pane pane;

    HashMap<Long, Player> players = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        new Thread(new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    while (true) {
                        while (in.position() < 2) {
                            socket.read(in);
                        }
                        //TODO may be extra
                        switch (SignalCode.getCode(in.get(0))) {
                            default:
                        }
                        int amount = in.get(1);
                        while (in.position() < amount * 25 + 18) {
                            socket.read(in);
                        }
                        System.out.println(Arrays.toString(in.array()));
                        in.flip();
                        in.position(2);
                        for (int i = 0; i < amount; i++) {
                            Long id = in.getLong();
                            Player player = players.get(id);
                            if (player == null) {
                                Circle circle = new Circle(in.getDouble(), in.getDouble(), RADIUS,
                                        //TODO generate random colors
                                        Color.rgb(0, 250, 0));
                                players.put(id, new Player(id, circle.getCenterX(), circle.getCenterY(), in.get(), circle));
                                pane.getChildren().add(circle);
                            } else {
                                player = players.get(id);
                                player.x = in.getDouble();
                                player.y = in.getDouble();
                                player.score = in.get();
                                player.circle.setCenterX(player.x);
                                player.circle.setCenterY(player.y);
                            }
                        }
                        in.clear();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    switchOnEnter();
                    return null;
                }
            }
        });

    }
}
