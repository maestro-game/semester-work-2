package controllers;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import main.Client;
import models.Room;
import models.SignalCode;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;
import java.util.ResourceBundle;

import static main.Client.*;

public class RoomsController implements Initializable {

    public ListView<Room> listView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        out.clear();
        EventHandler<Event> chose = event -> {
            Room room = listView.getSelectionModel().getSelectedItems().get(0);
            if (room == null) return;
            if (room.isSecured) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle(room.name);
                dialog.setHeaderText("Password:");
                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {
                    byte[] password = result.get().getBytes(StandardCharsets.UTF_8);
                    out.put((byte) (2 + password.length));
                    out.put(room.id);
                    out.put(password);
                }
            } else {
                out.put((byte) 2);
                out.put(room.id);
            }

            System.out.println(out.position() + " " + out.limit() + " " + Arrays.toString(out.array()));
            Client.flush();
            try {
                while (socket.read(in) < 1) ;
            } catch (IOException e) {
                closeSocket();
                Client.switchOnEnter();
            }
            String message = null;
            switch (SignalCode.getCode(in.get(0))) {
                case game:
                    switchOnGame();
                    break;
                case full:
                    message = "Комната полная";
                    break;
                case authError:
                    message = "Неверный пароль";
                    break;
                default:
                    message = "Сервер ответил неправильным сообщением";
            }
            in.clear();
            if (message != null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Не удалось подключиться к комнате");
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
            }
        };
        listView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                chose.handle(event);
            } else if (event.getCode() == KeyCode.DELETE) {
                closeSocket();
                Client.switchOnEnter();
            }
        });
        listView.setOnMouseClicked(chose);
        listView.requestFocus();

        new Thread(new Task<Void>() {
            @Override
            protected Void call() {
                LinkedList<Room> list = new LinkedList<>();
                try {
                    while (in.remaining() < 1) socket.read(in);
                    byte amount = in.get();
                    while (in.remaining() < amount * 4) {
                        socket.read(in);
                    }
                    for (int i = 0; i < amount; i++) {
                        list.add(new Room(in.get(), in.get(), in.get(), null, in.get() == 1));
                    }
                    for (Room room : list) {
                        byte length = in.get();
                        while (in.remaining() < length) socket.read(in);
                        room.name = new String(in.array(), in.position(), length);
                        in.position(in.position() + length);
                    }
                } catch (IOException e) {
                    switchOnEnter();
                }
                System.out.println("list size " + list.size());
                listView.setItems(FXCollections.observableList(list));
                in.clear();
                return null;
            }
        }).start();
    }
}
