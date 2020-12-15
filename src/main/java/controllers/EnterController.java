package controllers;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import main.Main;
import services.ConnectService;

import java.net.URL;
import java.nio.channels.SocketChannel;
import java.util.ResourceBundle;

public class EnterController implements Initializable, ConnectController {
    public GridPane gridPane;
    public TextField host;
    public TextField port;
    public TextField login;
    public PasswordField password;
    public Button submit;
    public Label lHost;
    public Label lPort;
    public Label lLogin;
    public Label lPassword;
    public Label lSubmit;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Main.out.clear();
        Main.in.clear();
        ConnectService connectService = new ConnectService();
        EventHandler<Event> eventHandler = event -> {
            try {
                boolean error = false;
                if (host.getText().length() == 0) {
                    lHost.setText("Адрес не может быть пустым");
                    lHost.setTextFill(Color.RED);
                    error = true;
                } else {
                    lHost.setText("Адрес сервера");
                    lHost.setTextFill(Color.BLACK);
                }
                if (port.getText().length() == 0) {
                    lPort.setText("Порт не может быть пустым");
                    lPort.setTextFill(Color.RED);
                    error = true;
                } else {
                    lPort.setText("Порт");
                    lPort.setTextFill(Color.BLACK);
                }
                if (login.getText().length() == 0) {
                    lLogin.setText("Логин не может быть пустым");
                    lLogin.setTextFill(Color.RED);
                    error = true;
                } else {
                    lLogin.setText("Логин");
                    lLogin.setTextFill(Color.BLACK);
                }
                if (password.getText().length() == 0) {
                    lPassword.setText("Пароль не может быть пустым");
                    lPassword.setTextFill(Color.RED);
                    error = true;
                } else {
                    lPassword.setText("Пароль");
                    lPassword.setTextFill(Color.BLACK);
                }
                if (error) return;

                connectService.connect(this, host.getText(), Integer.parseInt(port.getText()), login.getText(), password.getText());
            } catch (NumberFormatException e) {
                lPort.setText("Не верный номер порта");
                lPort.setTextFill(Color.RED);
            }
        };
        EventHandler<KeyEvent> keyEventEventHandler = event -> {
            if (event.getCode() == KeyCode.ENTER) {
                eventHandler.handle(event);
            }
        };
        submit.setOnMouseClicked(eventHandler);
        host.setOnKeyPressed(keyEventEventHandler);
        port.setOnKeyPressed(keyEventEventHandler);
        login.setOnKeyPressed(keyEventEventHandler);
        password.setOnKeyPressed(keyEventEventHandler);
    }

    @Override
    public void setMessage(String message, boolean isWarning) {
        Platform.runLater(() -> {
            lSubmit.setText(message);
            lSubmit.setTextFill(isWarning ? Color.RED : Color.BLACK);
        });
    }

    @Override
    public void connected(SocketChannel socket) {
        Platform.runLater(() -> {
            Main.socket = socket;
            Main.switchOnRooms();
        });
    }
}
