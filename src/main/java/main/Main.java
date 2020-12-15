package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import models.SignalCode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class Main extends Application {
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
    public void stop() throws Exception {
        try {
            socket.write(SignalCode.disconnect.getBuffer());
        } finally {
            System.out.println("closed");
            socket.close();
        }
    }

    private static Scene load(String path) {
        try {
            return new Scene(FXMLLoader.load(Main.class.getClassLoader().getResource(path)));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void switchOnGame() {
        stage.setWidth(1000);
        stage.setHeight(1000);
        stage.setResizable(true);
        stage.setTitle("Game");
        Scene game = load("fxml/game.fxml");
        stage.setScene(game);
        stage.centerOnScreen();
        game.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                in.clear();
                out.clear();
                out.put(SignalCode.leaveRoom.getByte());
                flush();
                Main.switchOnRooms();
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
                out.clear();
                out.put(SignalCode.disconnect.getByte());
                flush();
                Main.switchOnEnter();
            }
        });
    }

    public static void flush() {
        try {
            socket.write(out);
        } catch (IOException e) {
            try {
                socket.close();
                System.out.println("error during flush in main");
                switchOnEnter();
            } catch (IOException ignored) {
            }

        }
    }
}
