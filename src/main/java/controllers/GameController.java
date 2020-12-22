package controllers;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.concurrent.Task;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

import static main.Client.*;

public class GameController implements Initializable {
    public static final double PLAYER_RADIUS = 10;
    public static final double COIN_RADIUS = 5;
    public static byte[] direction = new byte[]{0};
    public Thread game;

    public GridPane gridPane;
    public Pane pane;
    public ListView<Player> list;
    private double coinX;
    private double coinY;
    private final AtomicBoolean isChanged = new AtomicBoolean(true);
    private byte interrupted;

    LinkedList<Player> players = new LinkedList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        gridPane.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case DELETE:
                    interrupted = 1;
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
        game = new Thread(new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    while (!game.isInterrupted()) {
                        if (interrupted == 2) {
                            in.flip();
                            do {
                                if (in.remaining() == 0) {
                                    in.clear();
                                    do {
                                        socket.read(in);
                                    } while (in.position() == 0);
                                    in.flip();
                                    System.out.println("after flip in loop");
                                    System.out.println(Arrays.toString(in.array()));
                                }
                            } while (in.get() != SignalCode.room.getByte());
                            game.interrupt();
                            Platform.runLater(Client::switchOnRooms);
                            return null;
                        }
                        while (in.position() < 2) {
                            socket.read(in);
                        }
                        switch (SignalCode.getCode(in.get(0))) {
                            case checkAlive:
                                byte[] name = new byte[in.get(1)];
                                while (in.position() < 2 + name.length) socket.read(in);
                                in.position(2);
                                in.get(name);
                                Platform.runLater(() -> new Alert(Alert.AlertType.INFORMATION, "Победил " + new String(name, StandardCharsets.UTF_8)).show());
                                do {
                                    if (in.remaining() == 0) {
                                        in.clear();
                                        do {
                                            socket.read(in);
                                        } while (in.position() == 0);
                                        in.flip();
                                        System.out.println("after flip in loop");
                                        System.out.println(Arrays.toString(in.array()));
                                    }
                                } while (in.get() != SignalCode.room.getByte());
                                game.interrupt();
                                Platform.runLater(Client::switchOnRooms);
                                return null;
                        }
                        int amount = in.get(1);
                        while (in.position() < amount * 25 + 18) {
                            socket.read(in);
                        }
                        in.flip();
                        in.position(2);
                        coinX = in.getDouble();
                        coinY = in.getDouble();
                        long id = in.getLong();
                        ListIterator<Player> it = players.listIterator();
                        int i = 0;
                        while (it.hasNext()) {
                            Player player = it.next();
                            if (player.id != id) {
                                it.remove();
                                pane.getChildren().remove(player.circle);
                            } else {
                                player.x = in.getDouble();
                                player.y = in.getDouble();
                                player.setScore(in.get());
                                if (++i == amount) break;
                                id = in.getLong();
                            }
                        }
                        if (it.hasNext()) {
                            do {
                                pane.getChildren().remove(it.next().circle);
                                it.remove();
                            } while (it.hasNext());
                        } else if (i != amount) {
                            do {
                                Player player = new Player(id, in.getDouble(), in.getDouble(), in.get());
                                player.circle = new Circle(player.x, player.y, PLAYER_RADIUS, Color.GREEN);
                                Platform.runLater(() -> pane.getChildren().add(player.circle));
                                players.add(player);
                                if (++i == amount) break;
                                id = in.getLong();
                            } while (true);
                        }
                        in.clear();
                        if (interrupted != 0) {
                            if (interrupted == 1) {
                                socket.write(SignalCode.leaveRoom.getBuffer());
                                interrupted = 2;
                            }
                        } else {
                            socket.write(ByteBuffer.wrap(direction));
                        }
                    }
                    return null;
                } catch (IOException e) {
                    e.printStackTrace();
                    Platform.runLater(() -> switchOnEnter());
                    return null;
                }
            }
        });
        game.start();

        Circle coin = new Circle(coinX, coinY, COIN_RADIUS, Color.GOLD);
        Platform.runLater(() -> pane.getChildren().add(coin));
        final LongProperty lastUpdateTime = new SimpleLongProperty(0);
        final AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long timestamp) {
                if (lastUpdateTime.get() > 0) {
                    coin.setCenterX(coinX);
                    coin.setCenterY(coinY);
                    for (Player player : players) {
                        player.circle.setCenterX(player.x);
                        player.circle.setCenterY(player.y);
                    }
                }
                lastUpdateTime.set(timestamp);
            }

        };
        timer.start();
        list.setItems(Player.leader);
    }
}
