package controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import main.Client;
import models.Player;
import models.SignalCode;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.ResourceBundle;

import static main.Client.*;

public class GameController implements Initializable {
    public static final double PLAYER_RADIUS = 10;
    public static final double COIN_RADIUS = 5;
    public static byte[] direction = new byte[]{0};
    public Thread game;

    public GridPane gridPane;
    public Pane pane;
    public ListView<Player> list;
    public Circle coin = new Circle(100, 100, COIN_RADIUS, Color.GOLD);

    HashMap<Long, Player> players = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        gridPane.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case DELETE:
                    game.interrupt();
                    in.clear();
                    out.clear();
                    out.put(SignalCode.leaveRoom.getByte());
                    flush();
                    Client.switchOnRooms();
                    break;
                case W:
                    GameController.direction[0] = 0;
                    break;
                case S:
                    GameController.direction[0] = 2;
                    break;
                case A:
                    GameController.direction[0] = 3;
                    break;
                case D:
                    GameController.direction[0] = 1;
            }
        });
        pane.getChildren().add(coin);
        game = new Thread(new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    while (!game.isInterrupted()) {
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
                        in.flip();
                        in.position(2);
                        coin.setCenterX(in.getDouble());
                        coin.setCenterY(in.getDouble());
                        for (int i = 0; i < amount; i++) {
                            Long id = in.getLong();
                            Player player = players.get(id);
                            if (player == null) {
                                Circle circle = new Circle(in.getDouble(), in.getDouble(), PLAYER_RADIUS,
                                        //TODO generate random colors
                                        Color.rgb(0, (int) (100*(id)), 0));
                                players.put(id, new Player(id, circle.getCenterX(), circle.getCenterY(), in.get(), circle));
                                Platform.runLater(() -> pane.getChildren().add(circle));
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
                        socket.write(ByteBuffer.wrap(direction));
                    }
                    return null;
                } catch (IOException e) {
                    game.interrupt();
                    e.printStackTrace();
                    switchOnEnter();
                    return null;
                }
            }
        });
        game.start();
    }
}
