package dev.xernas;

import dev.xernas.utils.ByteBufferInputStream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Particle {

    private final DataInputStream in;
    private final DataOutputStream out;

    public Particle(DataInputStream in) {
        this.in = in;
        this.out = null;
    }

    public Particle(DataInputStream in, int length) throws ParticleException {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(in.readNBytes(length));
            this.in = new DataInputStream(new ByteBufferInputStream(buffer));
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
