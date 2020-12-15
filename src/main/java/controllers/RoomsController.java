package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import main.Main;
import models.Room;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Optional;
import java.util.ResourceBundle;
import static main.Main.out;

public class RoomsController implements Initializable {

    public ListView<Room> listView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        EventHandler<Event> chose = event -> {
            Room room = listView.getSelectionModel().getSelectedItems().get(0);
            if (room.isSecured) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle(room.name);
                dialog.setHeaderText("Password:");
                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {
                    byte[] password = result.get().getBytes(StandardCharsets.UTF_8);
                    out.put((byte) (5 + password.length));
                    out.putInt(room.id);
                    out.put(password);
                    Main.flush();
                    Main.switchOnGame();
                }
            }
        };

        LinkedList<Room> list = new LinkedList<>();
        list.add(new Room(1, 5, 3, "test1", false));
        list.add(new Room(2, 5, 3, "test2", true));
        ObservableList<Room> rooms = FXCollections.observableList(list);
        listView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                chose.handle(event);
            }
        });
        listView.setOnMouseClicked(chose);
        listView.setItems(rooms);
        listView.requestFocus();
    }
}
