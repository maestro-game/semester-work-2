package server.models;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;

class RoomTest {

    SelectionKey key;
    SocketChannel socket;
    static ByteBuffer byteBuffer;
    Room roomNoPass, roomWithPass;
    static User user1;
    static Player player1;

    @BeforeAll
    static void globalSetUp() {
        byteBuffer = Mockito.mock(ByteBuffer.class);
        user1 = new User(302010, "юзер".getBytes(StandardCharsets.UTF_8), null);
        player1 = new Player(102030, "игрок".getBytes(StandardCharsets.UTF_8), null, 100, 100, (byte) 10, null);
    }

    @BeforeEach
    void setUp() {
        key = Mockito.mock(SelectionKey.class);
        socket = Mockito.mock(SocketChannel.class);
        roomNoPass = new Room((byte) 10, (byte) 2, "c паролем".getBytes(StandardCharsets.UTF_8));
        roomWithPass = new Room((byte) 20, (byte) 100, "без пароля".getBytes(StandardCharsets.UTF_8), "мой пароль".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldRegisterSocket() throws ClosedChannelException {
        Room.attachUser((byte) 10, "anyOrNull".getBytes(StandardCharsets.UTF_8), user1, socket, byteBuffer, key);
        Mockito.verify(socket).register(any(), any());
    }

    @Test
    void shouldCancelKey() throws ClosedChannelException {
        Room.attachUser((byte) 10, "anyOrNull".getBytes(StandardCharsets.UTF_8), user1, socket, byteBuffer, key);
        Mockito.verify(key).cancel();
    }

    @Test
    void shouldNotAccept3dUser() throws ClosedChannelException {
        Room.attachUser((byte) 10, "anyOrNull".getBytes(StandardCharsets.UTF_8), user1, socket, byteBuffer, key);
        Room.attachUser((byte) 10, "anyOrNull".getBytes(StandardCharsets.UTF_8), new User(20, new byte[]{1, 2}, null), socket, byteBuffer, key);
        Room.attachUser((byte) 10, "anyOrNull".getBytes(StandardCharsets.UTF_8), player1, socket, byteBuffer, key);
        Mockito.verify(socket, Mockito.times(2)).register(any(), any());
    }

    @Test
    void shouldRegisterInRoomWithPass() throws ClosedChannelException {
        Room.attachUser((byte) 20, "мой пароль".getBytes(StandardCharsets.UTF_8), user1, socket, byteBuffer, key);
        Mockito.verify(socket).register(any(), any());
    }

    @Test
    void shouldHaveCorrectUsersAmount() throws ClosedChannelException {
        Room.attachUser((byte) 10, "anyOrNull".getBytes(StandardCharsets.UTF_8), user1, socket, byteBuffer, key);
        Room.attachUser((byte) 10, "anyOrNull".getBytes(StandardCharsets.UTF_8), player1, socket, byteBuffer, key);
        assertEquals(2, roomNoPass.players.size());
    }

    @Test
    void shouldReturnAuthErrorCode() throws ClosedChannelException {
        assertEquals(Room.attachUser((byte) 20, "неверный".getBytes(StandardCharsets.UTF_8), user1, socket, byteBuffer, key), SignalCode.authError);
    }

    @Test
    void shouldReturnLeaveRoomCode() throws ClosedChannelException {
        assertEquals(Room.attachUser((byte) 0, "неверный".getBytes(StandardCharsets.UTF_8), user1, socket, byteBuffer, key), SignalCode.leaveRoom);
    }

    @Test
    void shouldReturnFullRoomCode() throws ClosedChannelException {
        Room.attachUser((byte) 10, "anyOrNull".getBytes(StandardCharsets.UTF_8), user1, socket, byteBuffer, key);
        Room.attachUser((byte) 10, "anyOrNull".getBytes(StandardCharsets.UTF_8), new User(20, new byte[]{1, 2}, null), socket, byteBuffer, key);
        assertEquals(Room.attachUser((byte) 10, "неверный".getBytes(StandardCharsets.UTF_8), user1, socket, byteBuffer, key), SignalCode.full);
    }
}