package server.models;

import server.Server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static server.Server.Entry;

public class Room extends Thread {
    private static final double MOVE_PER_TICK = 5;
    private static final long TICK_DELAY = 100;
    private static final double COIN_PLUS_PLAYER_RADIUS = 5 + 10;
    private static final double FIELD_WIDTH = 1000;
    private static final double FIELD_HEIGHT = 1000;
    private static final byte WIN_AMOUNT = 10;


    private static byte[] roomsAsBytes = new byte[]{SignalCode.room.getByte(), 0};
    private static int totalLength = 2;

    private static final TreeMap<Byte, Room> rooms = new TreeMap<>();

    public final byte id;
    public final byte capacity;
    public AtomicInteger size = new AtomicInteger(0);
    public final byte[] name;
    public final Entry password;
    private final AtomicInteger attaching = new AtomicInteger(0);
    public final HashMap<Long, Player> players = new HashMap<>();
    public final Selector selector = Server.getSelector();

    private boolean close = false;
    private Player winner = null;
    private double coinX = 500;
    private double coinY = 500;
    private byte[] out = generateOut();

    public Room(byte id, byte capacity, byte[] name) {
        this(id, capacity, name, null);
    }

    public Room(byte id, byte capacity, byte[] name, byte[] password) {
        this.id = id;
        this.capacity = capacity;
        this.name = name;
        this.password = password != null ? new Entry(password) : null;
        synchronized (roomsAsBytes) {
            rooms.put(id, this);
            totalLength += 5 + name.length;
            roomsAsBytes = generateRoomsAsBytes();
        }
        this.start();
    }

    private static byte[] generateRoomsAsBytes() {
        byte[] result = new byte[totalLength];
        result[0] = SignalCode.room.getByte();
        result[1] = (byte) rooms.size();
        Collection<Room> values = rooms.values();
        int i = 1;
        for (Room room : values) {
            i += 4;
            result[i - 3] = room.id;
            result[i - 2] = room.size.byteValue();
            result[i - 1] = room.capacity;
            result[i] = (byte) (room.password == null ? 0 : 1);
        }
        i++;
        for (Room room : values) {
            result[i++] = (byte) room.name.length;
            System.arraycopy(room.name, 0, result, i, room.name.length);
            i += room.name.length;
        }
        return result;
    }

    @Override
    public void run() {
        System.out.println("started room");
        while (!interrupted()) {
            if (attaching.get() != 0 || players.size() == 0) {
                System.out.println("waiting");
                try {
                    synchronized (players) {
                        players.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            try {
                if (!(selector.select() > -1)) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            long start = System.currentTimeMillis();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            if (close) {
                writeClose(iterator);
                interrupt();
                break;
            }
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (key.isValid()) {
                    try {
                        Player player = (Player) ((Server.Attachment) key.attachment()).user;
                        SocketChannel channel = ((SocketChannel) key.channel());
                        Server.Attachment attachment = ((Server.Attachment) key.attachment());
                        int amount = channel.read(attachment.in);

                        if (amount > 0) {
                            //reading
                            SignalCode code = SignalCode.getCode(attachment.in.position() == 2 ? attachment.in.get(1) : attachment.in.get(0));
                            if (code != null) {
                                switch (code) {
                                    case leaveRoom: {
                                        System.out.println("left room");
                                        removeUser(key);
                                        Server.attachToLobby(attachment.user, channel, attachment.in);
                                        continue;
                                    }
                                    case disconnect: {
                                        System.out.println("closed by room");
                                        removeUser(key);
                                        key.channel().close();
                                        continue;
                                    }
                                }
                            }
                            if (attachment.in.position() > 2) {
                                removeUser(key);
                                key.channel().close();
                                continue;
                            }

                            player.direction = attachment.in.get(0);
                            attachment.in.clear();
                            key.interestOps(SelectionKey.OP_WRITE);
                        } else {
                            if (amount < 0) {
                                removeUser(key);
                                key.channel().close();
                                continue;
                            }
                        }

                        byte[] bytes = new byte[8];
                        switch (player.direction) {
                            case 0:
                                player.y -= MOVE_PER_TICK;
                                ByteBuffer.wrap(bytes).putDouble(player.y);
                                System.arraycopy(bytes, 0, out, player.order * 25 + 34, 8);
                                break;
                            case 1:
                                player.x += MOVE_PER_TICK;
                                ByteBuffer.wrap(bytes).putDouble(player.x);
                                System.arraycopy(bytes, 0, out, player.order * 25 + 26, 8);
                                break;
                            case 2:
                                player.y += MOVE_PER_TICK;
                                ByteBuffer.wrap(bytes).putDouble(player.y);
                                System.arraycopy(bytes, 0, out, player.order * 25 + 34, 8);
                                break;
                            case 3:
                                player.x -= MOVE_PER_TICK;
                                ByteBuffer.wrap(bytes).putDouble(player.x);
                                System.arraycopy(bytes, 0, out, player.order * 25 + 26, 8);
                        }
                        if (coinIsPicked(player)) {
                            if (player.score == WIN_AMOUNT) {
                                close = true;
                                winner = player;
                                break;
                            }
                            out[player.order * 25 + 42] = ++player.score;
                            generateCoin();
                        }
                        //always writeable
                        channel.write(ByteBuffer.wrap(out));
                    } catch (Exception e) {
                        e.printStackTrace();
                        try {
                            removeUser(key);
                            key.channel().close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
            try {
                Thread.sleep(start + TICK_DELAY - System.currentTimeMillis());
            } catch (Exception ignored) {
            }
        }
        System.out.println("closed room");
    }

    public void closeRoom() {
        close = true;
    }

    public void writeClose(Iterator<SelectionKey> iterator) {
        byte[] message;
        if (winner != null) {
            message = new byte[2 + winner.name.length];
            message[0] = SignalCode.checkAlive.getByte();
            message[1] = (byte) winner.name.length;
            System.arraycopy(winner.name, 0, message, 2, winner.name.length);
        } else {
            message = new byte[]{SignalCode.leaveRoom.getByte()};
        }

        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();
            if (key.isValid()) {
                SocketChannel channel = ((SocketChannel) key.channel());
                try {
                    channel.write(ByteBuffer.wrap(message));
                    Server.Attachment attachment = (Server.Attachment)key.attachment();
                    Server.attachToLobby(attachment.user, channel, attachment.in);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private double sqr(double a) {
        return a*a;
    }

    private boolean coinIsPicked(Player player) {
        return Math.sqrt(sqr(player.x - coinX) + sqr(player.y - coinY)) < COIN_PLUS_PLAYER_RADIUS;
    }

    private void generateCoin() {
        synchronized (roomsAsBytes) {
            coinX = Math.random() * FIELD_WIDTH;
            coinY = Math.random() * FIELD_HEIGHT;
            byte[] bytes = new byte[8];
            ByteBuffer.wrap(bytes).putDouble(coinX);
            System.arraycopy(bytes, 0, out, 2, 8);
            ByteBuffer.wrap(bytes).putDouble(coinY);
            System.arraycopy(bytes, 0, out, 10, 8);
        }
    }

    public static SignalCode attachUser(byte roomId, byte[] password, User user, SocketChannel channel, ByteBuffer byteBuffer, SelectionKey key) throws ClosedChannelException {
        Room room = rooms.get(roomId);
        if (room == null) return SignalCode.leaveRoom;
        if (!room.password.hasSameBytes(password)) return SignalCode.authError;
        return room.attach(user, channel, byteBuffer, key);
    }

    private SignalCode attach(User user, SocketChannel channel, ByteBuffer byteBuffer, SelectionKey key) throws ClosedChannelException {
        if (size.get() >= capacity) return SignalCode.full;

        key.cancel();
        Player player;
        synchronized (roomsAsBytes) {
            roomsAsBytes[findRoom(id)] = (byte) size.incrementAndGet();
            player = user.toPlayer(500, 500, (byte) (size.get() - 1), this);
            out = Arrays.copyOf(out, out.length + 25);
            out[1]++;
            System.arraycopy(player.asBytes(), 0, out, out.length - 25, 25);
            players.put(user.id, player);
        }
        System.out.println("registering...");
        attaching.incrementAndGet();
        synchronized (players) {
            channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, new Server.Attachment(byteBuffer, player));
            if (attaching.decrementAndGet() == 0) {
                players.notify();
            }
        }
        System.out.println("registered");
        return SignalCode.game;
    }

    private byte[] generateOut() {
        synchronized (roomsAsBytes) {
            byte[] result = new byte[18 + 25 * size.get()];
            ByteBuffer buffer = ByteBuffer.wrap(result).put(SignalCode.game.getByte()).put((byte) size.get()).putDouble(coinX).putDouble(coinY);
            for (Player player : players.values()) {
                buffer.putDouble(player.id).putDouble(player.x).putDouble(player.y).put(player.score);
            }
            return result;
        }
    }

    private void removeUser(SelectionKey key) {
        key.cancel();
        size.decrementAndGet();
        synchronized (roomsAsBytes) {
            roomsAsBytes[findRoom(id)]--;
            players.remove(((Server.Attachment) key.attachment()).user.id);
            byte i = 0;
            for (Player player : players.values()) {
                player.order = i++;
            }
            out = generateOut();
        }
    }

    private int findRoom(int key) {
        int low = 0;
        int high = rooms.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            byte midVal = roomsAsBytes[mid * 4 + 2];

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid * 4 + 3;
        }
        return -1;
    }

    public static byte[] getAllAsBytes() {
        return roomsAsBytes;
    }
}