package dev.xernas.particle;

import dev.xernas.particle.utils.ByteBufferInputStream;
import dev.xernas.particle.utils.Host;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class Particle {

    private final DataInputStream in;
    private final DataOutputStream out;

    public Particle(boolean UDPSystem) {
        if (UDPSystem) {
            this.in = new DataInputStream(System.in);
            this.out = new DataOutputStream(System.out);
        } else {
            this.in = null;
            this.out = null;
        }
    }

    public Particle(DataInputStream in) {
        this.in = in;
        this.out = null;
    }

    public Particle(DataInputStream in, int length) throws ParticleException {
        try {
            // ByteBuffer buffer = ByteBuffer.wrap(in.readNBytes(length));
            byte[] bytes = new byte[length];
            in.readFully(bytes);
            this.in = new DataInputStream(new ByteArrayInputStream(bytes));
            this.out = null;
        } catch (IOException e) {
            throw new ParticleException("Couldn't create length input stream", e);
        }
    }

    public Particle(DataOutputStream out) {
        this.in = null;
        this.out = out;
    }

    public Particle(DataInputStream in, DataOutputStream out) {
        this.in = in;
        this.out = out;
    }

    public DataInputStream in() {
        if (in == null) {
            throw new IllegalStateException("Input stream is null");
        }
        return in;
    }

    public DataOutputStream out() {
        if (out == null) throw new IllegalStateException("Output stream is null");
        return out;
    }

    public void writeInt(int value) throws WriteException {
        try {
            out().writeInt(value);
        } catch (Exception e) {
            throw new WriteException("Failed to write int", e);
        }
    }

    public int readInt() throws ReadException {
        try {
            return in().readInt();
        } catch (Exception e) {
            throw new ReadException("Failed to read int", e);
        }
    }

    public void writeLong(long value) throws WriteException {
        try {
            out().writeLong(value);
        } catch (Exception e) {
            throw new WriteException("Failed to write long", e);
        }
    }

    public long readLong() throws ReadException {
        try {
            return in().readLong();
        } catch (Exception e) {
            throw new ReadException("Failed to read long", e);
        }
    }

    public void writeString(String value) throws WriteException {
        try {
            out().writeUTF(value);
        } catch (Exception e) {
            throw new WriteException("Failed to write string", e);
        }
    }

    public String readString() throws ReadException {
        try {
            return in().readUTF();
        } catch (Exception e) {
            throw new ReadException("Failed to read string", e);
        }
    }

    public void writeShort(short value, boolean signed) throws WriteException {
        try {
            if (signed) {
                out().writeShort(value);
            } else {
                out().writeShort(Short.toUnsignedInt(value));
            }
        } catch (Exception e) {
            throw new WriteException("Failed to write short", e);
        }
    }

    public void writeShort(short value) throws WriteException {
        writeShort(value, true);
    }

    public short readShort(boolean signed) throws ReadException {
        try {
            if (signed) {
                return in().readShort();
            } else {
                return (short) Short.toUnsignedInt(in().readShort());
            }
        } catch (Exception e) {
            throw new ReadException("Failed to read short", e);
        }
    }

    public short readShort() throws ReadException {
        return readShort(true);
    }

    public void writeBoolean(boolean value) throws WriteException {
        try {
            out().writeBoolean(value);
        } catch (IOException e) {
            throw new WriteException("Failed to write boolean", e);
        }
    }

    public boolean readBoolean() throws ReadException {
        try {
            return in().readBoolean();
        } catch (IOException e) {
            throw new ReadException("Failed to read boolean", e);
        }
    }

    public void writeBytes(byte[] bytes) throws WriteException {
        try {
            out().write(bytes);
        } catch (Exception e) {
            throw new WriteException("Failed to write bytes", e);
        }
    }

    public byte[] readBytes(int length) throws ReadException {
        try {
            byte[] bytes = new byte[length];
            in().readFully(bytes);
            return bytes;
        } catch (Exception e) {
            throw new ReadException("Failed to read bytes", e);
        }
    }

    public int readByte(boolean signed) throws ReadException {
        try {
            if (signed) return in().readByte();
            else return in().readUnsignedByte();
        } catch (IOException e) {
            throw new ReadException("Failed to read byte", e);
        }
    }

    public void writeByte(int value) throws WriteException {
        try {
            out().writeByte(value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] readEveryBytes() throws ReadException {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead;
            while ((bytesRead = in().read(buffer.array())) != -1) {
                buffer.position(buffer.position() + bytesRead);
            }
            byte[] result = new byte[buffer.position()];
            System.arraycopy(buffer.array(), 0, result, 0, buffer.position());
            return result;
        } catch (IOException e) {
            throw new ReadException("Failed to read every bytes", e);
        }
    }

    public void flush() throws WriteException {
        try {
            out().flush();
        } catch (Exception e) {
            throw new WriteException("Failed to flush", e);
        }
    }

    public void close() throws ParticleException {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (Exception e) {
            throw new ParticleException("Failed to close", e);
        }
    }

    public static void sendUDP(byte[] data, DatagramSocket socket, Host to) throws WriteException {
        try {
            socket.send(new java.net.DatagramPacket(data, data.length, new InetSocketAddress(to.host(), to.port())));
        } catch (IOException e) {
            throw new WriteException("Failed to send UDP packet", e);
        }
    }

    public static void sendUDP(String data, DatagramSocket socket, Host to) throws WriteException {
        byte[] bytes = data.getBytes();
        sendUDP(bytes, socket, to);
    }

    public static void sendUDP(ByteBuffer data, DatagramSocket socket, Host to) throws WriteException {
        byte[] bytes = new byte[data.remaining()];
        data.get(bytes);
        sendUDP(bytes, socket, to);
    }

    public static void sendUDP(int data, DatagramSocket socket, Host to) throws WriteException {
        ByteBuffer bytes = ByteBuffer.allocate(4).putInt(data);
        sendUDP(bytes, socket, to);
    }

    public static class WriteException extends ParticleException {

        public WriteException(String message) {
            super(message);
        }

        public WriteException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    public static class ReadException extends ParticleException {

        public ReadException(String message) {
            super(message);
        }

        public ReadException(String message, Throwable cause) {
            super(message, cause);
        }

    }


}
