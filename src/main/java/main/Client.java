package main;

import controllers.GameController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import models.SignalCode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client extends Application {
    public static ByteBuffer out = ByteBuffer.allocate(1024);
    public static ByteBuffer in = ByteBuffer.allocate(1024);
    public static SocketChannel socket;
    private static Stage stage;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        switchOnEnter();
        primaryStage.show();
    }

    @Override
    public void stop() {
        closeSocket();
    }

    private static Scene load(String path) {
        try {
            return new Scene(FXMLLoader.load(Client.class.getClassLoader().getResource(path)));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void switchOnGame() {
        stage.setWidth(1300);
        stage.setHeight(1000);
        stage.setResizable(true);
        stage.setTitle("Game");
        Scene game = load("fxml/game.fxml");
        stage.setScene(game);
        stage.centerOnScreen();
        game.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER:
                    in.clear();
                    out.clear();
                    out.put(SignalCode.leaveRoom.getByte());
                    flush();
                    Client.switchOnRooms();
                    break;
                case UP:
                    GameController.direction.put(0, (byte) 0);
                    break;
                case DOWN:
                    GameController.direction.put(0, (byte) 2);
                    break;
                case LEFT:
                    GameController.direction.put(0, (byte) 3);
                    break;
                case RIGHT:
                    GameController.direction.put(0, (byte) 1);
            }
        });
    }

    public static void switchOnEnter() {
        stage.setWidth(500);
        stage.setHeight(500);
        stage.setResizable(false);
        stage.setTitle("Enter");
        stage.setScene(load("fxml/enter.fxml"));
        stage.centerOnScreen();
    }

    public static void switchOnRooms() {
        stage.setWidth(500);
        stage.setHeight(500);
        stage.setResizable(true);
        stage.setTitle("Rooms");
        Scene rooms = load("fxml/rooms.fxml");
        stage.setScene(rooms);
        stage.centerOnScreen();
        rooms.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                closeSocket();
                Client.switchOnEnter();
            }
        });
    }

    public static void flush() {
        try {
            out.flip();
            socket.write(out);
        } catch (IOException e) {
            closeSocket();
            System.out.println("error during flush in main");
            switchOnEnter();
        }
    }

    public static void closeSocket() {
        if (socket != null) {
            try {
                socket.write(SignalCode.disconnect.getBuffer());
            } catch (Exception ignored) {
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
