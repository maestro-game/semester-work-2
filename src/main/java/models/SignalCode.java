package models;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public enum SignalCode {
    authError(-127),
    checkAlive(-121),
    ddosAlert(-128),
    disconnect(-125),
    game(-123),
    full(-126),
    leaveRoom(-124),
    room(-122);

    byte[] b;
    static private final Map<Byte, SignalCode> map = new HashMap<>();

    static {
        for (SignalCode code : SignalCode.values()) {
            map.put(code.b[0], code);
        }
    }

    public byte getByte() {
        return b[0];
    }

    public ByteBuffer getBuffer() {
        return ByteBuffer.wrap(b);
    }

    public static SignalCode getCode(byte b) {
        return map.get(b);
    }

    SignalCode(int value) {
        b = new byte[]{(byte) value};
    }
}
