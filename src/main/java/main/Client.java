package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
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
        stage.setScene(load("fxml/game.fxml"));
        stage.centerOnScreen();
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
        System.out.println("in switch on rooms");
        stage.setWidth(500);
        stage.setHeight(500);
        stage.setResizable(true);
        stage.setTitle("Rooms");
        stage.setScene(load("fxml/rooms.fxml"));
        stage.centerOnScreen();
        System.out.println("out switch on rooms");
    }

    public static void flush() {
        try {
            out.flip();
            socket.write(out);
            out.clear();
        } catch (IOException e) {
            e.printStackTrace();
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
