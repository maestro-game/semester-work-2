package server;

import server.models.Room;
import server.models.SignalCode;
import server.models.User;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public class Server {

    public static class Entry {
        private final byte[] data;
        private int hashCode;

        public Entry(byte[] data) {
            this.data = data;
            hashCode = Arrays.hashCode(data);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entry entry = (Entry) o;
            return Arrays.equals(data, entry.data);
        }

        public boolean hasSameBytes(byte[] bytes) {
            return Arrays.equals(data, bytes);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    private final static HashMap<Entry, User> database = new HashMap<>();

    static {
        database.put(new Entry("vadim".getBytes(StandardCharsets.UTF_8)), new User(1, "vadim".getBytes(StandardCharsets.UTF_8), "pass".getBytes(StandardCharsets.UTF_8)));
    }

    public final static String HOST = "127.0.0.1";
    public final static int PORT = 9000;
    public final static int BUFFER_SIZE = 1024;
    public final static int CONNECTION_QUEUE = 10;
    public final static Selector selector = getSelector();

    public static class Attachment {
        public ByteBuffer in;
        public User user;

        public Attachment(ByteBuffer in, User user) {
            this.in = in;
            this.user = user;
        }

        public Attachment() {
            in = ByteBuffer.allocate(BUFFER_SIZE);
        }
    }

    public static void main(String[] args) throws IOException {
        //TODO initialise rooms by other way
        new Room((byte)1, (byte)10, "test1".getBytes(StandardCharsets.UTF_8));
        new Room((byte)10, (byte)3, "test2".getBytes(StandardCharsets.UTF_8), "pass".getBytes(StandardCharsets.UTF_8));

        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress(HOST, PORT), CONNECTION_QUEUE);
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, serverSocket.validOps());

        while (selector.select() > -1) {
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (key.isValid()) {
                    try {
                        if (key.isAcceptable()) {
                            SocketChannel newSocket = ((ServerSocketChannel) key.channel()).accept();
                            newSocket.configureBlocking(false);
                            newSocket.register(selector, SelectionKey.OP_READ, new Attachment());
                            System.out.println("acceptable");
                        } else if (key.isReadable()) {
                            SocketChannel channel = ((SocketChannel) key.channel());
                            Attachment attachment = ((Attachment) key.attachment());
                            int amount = channel.read(attachment.in);
                            if (amount < 1) {
                                if (amount < 0) {
                                    close(key);
                                }
                                continue;
                            }
                            System.out.println(Arrays.toString(attachment.in.array()));
                            byte messageLen = attachment.in.get(0);
                            if (attachment.in.position() < messageLen) {
                                continue;
                            }
                            if (attachment.in.position() > messageLen) {
                                close(key);
                                continue;
                            }
                            attachment.in.flip();
                            if (attachment.user != null) {
                                attachment.in.position(1);
                                byte login = attachment.in.get();
                                byte[] password = new byte[attachment.in.remaining()];
                                attachment.in.get(password);
                                attachment.in.clear();

                                //TODO add full signal code
                                if (Room.attachUser(login, password, attachment.user, channel, attachment.in, key)) {
                                    //key.cancel();
                                } else {
                                    channel.write(SignalCode.authError.getBuffer());
                                }
                                continue;
                            }

                            attachment.in.position(1);
                            byte[] login = new byte[attachment.in.get()];
                            attachment.in.get(login);
                            byte[] password = new byte[attachment.in.remaining()];
                            attachment.in.get(password);
                            attachment.in.clear();

                            User user = database.get(new Entry(login));
                            if (user == null || !Arrays.equals(password, user.password)) {
                                channel.write(SignalCode.authError.getBuffer());
                                close(key);
                                continue;
                            }
                            attachment.user = user;
                            channel.write(ByteBuffer.wrap(Room.getAllAsBytes()));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        close(key);
                    }
                }
            }
        }
    }

    public static void close(SelectionKey key) throws IOException {
        key.cancel();
        key.channel().close();
        System.out.println("closed");
    }

    public static void attachToLobby(User user, SocketChannel socketChannel, ByteBuffer byteBuffer) {
        //TODO
    }

    public static Selector getSelector() {
        try {
            return SelectorProvider.provider().openSelector();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}