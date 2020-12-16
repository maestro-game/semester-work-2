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
import static server.Server.close;

public class Room extends Thread {
    private static byte[] roomsAsBytes = new byte[]{SignalCode.room.getByte(), 0};
    private static int totalLength = 2;

    private static final TreeMap<Byte, Room> rooms = new TreeMap<>();

    public final byte id;
    public final byte capacity;
    public AtomicInteger size = new AtomicInteger(0);
    public final byte[] name;
    public final Entry password;
    public final HashMap<Long, Player> players = new HashMap<>();
    public final Selector selector = Server.getSelector();
    private byte[] out = new byte[]{SignalCode.game.getByte(), 0};

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
            if (players.size() == 0) {
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
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (key.isValid()) {
                    try {
                        if (key.isReadable()) {
                            SocketChannel channel = ((SocketChannel) key.channel());
                            Server.Attachment attachment = ((Server.Attachment) key.attachment());
                            int amount = channel.read(attachment.in);
                            if (amount < 1) {
                                if (amount < 0) {
                                    close(key);
                                }
                                continue;
                            }
                            System.out.println(Arrays.toString(attachment.in.array()));
                            switch (attachment.in.position() == 18 ? SignalCode.getCode(attachment.in.get(17)) : SignalCode.getCode(attachment.in.get(0))) {
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
                            if (attachment.in.position() < 17) {
                                continue;
                            }
                            if (attachment.in.position() > 18) {
                                close(key);
                                continue;
                            }
                            attachment.in.flip();
                            attachment.in.position(1);
                            double x = attachment.in.getDouble();
                            double y = attachment.in.getDouble();
                            attachment.in.clear();
                            System.out.println((int) x + " " + (int) y);
                            //TODO correct out
                            key.interestOps(SelectionKey.OP_WRITE);
                        } else if (key.isWritable()) {
                            ((SocketChannel) key.channel()).write(ByteBuffer.wrap(out));
                            key.interestOps(0);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        try {
                            close(key);
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        }
    }

    public static boolean attachUser(byte roomId, byte[] password, User user, SocketChannel channel, ByteBuffer byteBuffer, SelectionKey key) throws ClosedChannelException {
        Room room = rooms.get(roomId);
        if (room == null) return false;
        if (!room.password.hasSameBytes(password)) return false;
        return room.attach(user, channel, byteBuffer, key);
    }

    private boolean attach(User user, SocketChannel channel, ByteBuffer byteBuffer, SelectionKey key) throws ClosedChannelException {
        if (size.get() >= capacity) return false;

        key.cancel();
        synchronized (roomsAsBytes) {
            roomsAsBytes[findRoom(id)] = (byte) size.incrementAndGet();
        }
        //TODO correct out
        Player player = user.toPlayer(500, 500, this);
        System.out.println("registering...");
        synchronized (players) {
            players.notify();
        }
        channel.register(selector, SelectionKey.OP_WRITE, new Server.Attachment(byteBuffer, player));
        System.out.println("registered");
        players.put(user.id, player);
        return true;
    }

    private void removeUser(SelectionKey key) {
        synchronized (roomsAsBytes) {
            roomsAsBytes[findRoom(id)]--;
        }
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