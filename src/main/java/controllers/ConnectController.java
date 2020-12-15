package controllers;

import java.nio.channels.SocketChannel;

public interface ConnectController {
    void setMessage(String message, boolean isWarning);
    void connected(SocketChannel socket);
}
