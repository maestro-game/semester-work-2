package services;

import controllers.ConnectController;
import models.SignalCode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static main.Client.*;

public class ConnectService {
    private static final int CONNECTION_TIMEOUT = 3_000;
    private static final int AUTHORISATION_TIMEOUT = 3_000;

    public void connect(ConnectController enterController, String host, int port, String login, String password) {
        new Thread(() -> {
            try {
                SocketChannel socket = SocketChannel.open();
                socket.configureBlocking(false);
                socket.connect(new InetSocketAddress(host, port));
                enterController.setMessage("Подключение к серверу...", false);
                long start = System.currentTimeMillis();
                while (!socket.isConnected()) {
                    if (System.currentTimeMillis() - start > CONNECTION_TIMEOUT) {
                        enterController.setMessage("Время ожидания подключения истекло", true);
                        return;
                    }
                    socket.finishConnect();
                }
                byte[] loginBytes = login.getBytes(StandardCharsets.UTF_8);
                byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
                out.put((byte) (loginBytes.length + passwordBytes.length + 2));
                out.put((byte) loginBytes.length);
                out.put(loginBytes);
                out.put(passwordBytes);
                out.flip();
                socket.write(out);
                enterController.setMessage("Сервер найден, авторизация...", false);
                while (socket.read(in) < 1) {
                    if (System.currentTimeMillis() - start > AUTHORISATION_TIMEOUT) {
                        enterController.setMessage("Время ожидания авторизации истекло", true);
                        closeSocket();
                        return;
                    }
                }
                in.flip();
                switch (SignalCode.getCode(in.get())) {
                    case authError:
                        enterController.setMessage("Неверный логин или пароль", true);
                        closeSocket();
                        in.clear();
                        out.clear();
                        break;
                    case room:
                        System.out.println(Arrays.toString(in.array()));
                        enterController.connected(socket);
                        break;
                    default:
                        enterController.setMessage("Сервер ответил неправильным сообщением", true);
                        closeSocket();
                        in.clear();
                        out.clear();
                }
            } catch (IOException e) {
                enterController.setMessage("Не удалось подключится к серверу", true);
                closeSocket();
            }
        }).start();
    }
}
