package server.models;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Room2Test {
    SelectionKey key;
    SocketChannel socket;
    Room roomNoPass, roomWithPass;
    static ByteBuffer byteBuffer;
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
    }

    @AfterEach
    void clear() {
        Room.ONLY_FOR_TEST();
    }

    @Test
    void shouldReturnCorrectBytesWithNoRooms() {
        assertTrue(Arrays.equals(new byte[]{-122, 0}, Room.getAllAsBytes()));
    }

    @Test
    void shouldReturnCorrectBytesWithRoomWithPass() {
        roomWithPass = new Room((byte) 20, (byte) 100, "без пароля".getBytes(StandardCharsets.UTF_8), "мой пароль".getBytes(StandardCharsets.UTF_8));
        System.out.println(Arrays.toString(Room.getAllAsBytes()));
        assertTrue(Arrays.equals(new byte[]{-122, 1, 20, 0, 100, 1, 19, -48, -79, -48, -75, -48, -73, 32, -48, -65, -48, -80, -47, -128, -48, -66, -48, -69, -47, -113}, Room.getAllAsBytes()));
    }

    @Test
    void shouldReturnCorrectBytesWithRoomWithoutPass() {
        roomNoPass = new Room((byte) 10, (byte) 2, "c паролем".getBytes(StandardCharsets.UTF_8));
        assertTrue(Arrays.equals(new byte[]{-122, 1, 10, 0, 2, 0, 16, 99, 32, -48, -65, -48, -80, -47, -128, -48, -66, -48, -69, -48, -75, -48, -68}, Room.getAllAsBytes()));
    }

    @Test
    void shouldReturnCorrectBytesWithBothRooms() {
        roomNoPass = new Room((byte) 10, (byte) 2, "c паролем".getBytes(StandardCharsets.UTF_8));
        roomWithPass = new Room((byte) 20, (byte) 100, "без пароля".getBytes(StandardCharsets.UTF_8), "мой пароль".getBytes(StandardCharsets.UTF_8));
        System.out.println(Arrays.toString(Room.getAllAsBytes()));
        assertTrue(Arrays.equals(new byte[]{-122, 2, 10, 0, 2, 0, 20, 0, 100, 1, 16, 99, 32, -48, -65, -48, -80, -47, -128, -48, -66, -48, -69, -48, -75, -48, -68, 19, -48, -79, -48, -75, -48, -73, 32, -48, -65, -48, -80, -47, -128, -48, -66, -48, -69, -47, -113}, Room.getAllAsBytes()));
    }
}
