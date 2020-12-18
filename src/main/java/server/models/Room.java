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
    private static final double MOVE_PER_TICK = 2;
    private static final long TICK_DELAY = 500;

    private static byte[] roomsAsBytes = new byte[]{SignalCode.room.getByte(), 0};
    private static int totalLength = 2;

    private static final TreeMap<Byte, Room> rooms = new TreeMap<>();

    public final byte id;
    public final byte capacity;
    public AtomicInteger size = new AtomicInteger(0);
    public final byte[] name;
    public final Entry password;
    //TODO
    private boolean started;
    private final AtomicInteger attaching = new AtomicInteger(0);
    public final HashMap<Long, Player> players = new HashMap<>();
    public final Selector selector = Server.getSelector();
    //TODO
    private byte[] out = new byte[18];{
        ByteBuffer.wrap(out).put(SignalCode.game.getByte()).put((byte) 0).putDouble(500).putDouble(500);
    }

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
        while (true) {
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
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (key.isValid()) {
                    try {
                        Player player = (Player) ((Server.Attachment) key.attachment()).user;
                        if (key.isReadable()) {
                            SocketChannel channel = ((SocketChannel) key.channel());
                            Server.Attachment attachment = ((Server.Attachment) key.attachment());
                            int amount = channel.read(attachment.in);
                            if (amount < 1) {
                                if (amount < 0) {
                                    removeUser(key);
                                    key.channel().close();
                                    continue;
                                }
                                continue;
                            }
                            switch (attachment.in.position() == 2 ? SignalCode.getCode(attachment.in.get(1)) : SignalCode.getCode(attachment.in.get(0))) {
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
                            if (attachment.in.position() > 2) {
                                removeUser(key);
                                key.channel().close();
                                continue;
                            }

                            player.direction = attachment.in.get(0);
                            attachment.in.clear();
                            System.out.println(player.direction);
                            key.interestOps(SelectionKey.OP_WRITE);
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

                        //always writeable
                        ((SocketChannel) key.channel()).write(ByteBuffer.wrap(out));
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
    }

    public static boolean attachUser(byte roomId, byte[] password, User user, SocketChannel channel, ByteBuffer byteBuffer, SelectionKey key) throws ClosedChannelException {
        Room room = rooms.get(roomId);
        if (room == null || room.started || !room.password.hasSameBytes(password)) return false;
        return room.attach(user, channel, byteBuffer, key);
    }

    private boolean attach(User user, SocketChannel channel, ByteBuffer byteBuffer, SelectionKey key) throws ClosedChannelException {
        if (size.get() >= capacity) return false;

        key.cancel();
        Player player;
        synchronized (roomsAsBytes) {
            roomsAsBytes[findRoom(id)] = (byte) size.incrementAndGet();
            player = user.toPlayer(500, 500, (byte) players.size(), this);
            out = Arrays.copyOf(out, out.length + 25);
            out[1]++;
            System.arraycopy(player.asBytes(), 0, out, out.length - 25, 25);
            players.put(user.id, player);
        }
        System.out.println("registering...");
        attaching.incrementAndGet();
        synchronized (players) {
            channel.register(selector, SelectionKey.OP_WRITE, new Server.Attachment(byteBuffer, player));
            if (attaching.decrementAndGet() == 0) {
                players.notify();
            }
        }
        System.out.println("registered");
        return true;
    }

    private void removeUser(SelectionKey key) {
        synchronized (roomsAsBytes) {
            roomsAsBytes[findRoom(id)]--;
        }
        //TODO update players order
        //TODO correct out
        size.decrementAndGet();
        key.cancel();
        players.remove(((Server.Attachment) key.attachment()).user.id);
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