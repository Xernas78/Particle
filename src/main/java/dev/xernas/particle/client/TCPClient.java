package dev.xernas.particle.client;

import dev.xernas.particle.Particle;
import dev.xernas.particle.client.exceptions.ClientException;
import dev.xernas.particle.message.MessageIO;
import dev.xernas.particle.tasks.PingTask;
import dev.xernas.particle.tasks.Task;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class TCPClient<I, O> implements Client<I, O> {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private boolean initialized = false;
    private Socket socket;
    private Particle particle;

    @Override
    public final void connect() throws ClientException {
        if (initialized) throw new ClientException("Client already initialized");
        try (Socket socket = new Socket(getHost(), getPort())) {
            this.socket = socket;
            this.particle = new Particle(new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
            onConnect(particle);

            PingTask<I, O> pingTask = new PingTask<>(this);
            scheduler.scheduleAtFixedRate(pingTask.asRunnable(), pingTask.getInitialDelay(), pingTask.getPeriod(), pingTask.getTimeUnit());

            getRepeatedTasks().forEach(task -> scheduler.scheduleAtFixedRate(task.asRunnable(), task.getInitialDelay(), task.getPeriod(), task.getTimeUnit()));

            MessageIO<I, O> messageIO = getMessageIO();
            initialized = true;
            while (isConnected()) {
                try {
                    I message = messageIO.read(particle);
                    if (message != null) onMessage(message, particle);
                } catch (Particle.ReadException ignore) {}
            }
        } catch (IOException e) {
            throw new ClientException("Failed to connect to server", e);
        } finally {
            // Shutdown the scheduler
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
    }

    @Override
    public final void disconnect() throws ClientException {
        try {
            socket.close();
            onDisconnect();
        } catch (IOException e) {
            throw new ClientException("Failed to disconnect from server", e);
        }
    }

    @Override
    public final void ping() {
        try {
            particle.writeInt(0);
        } catch (Particle.WriteException e) {
            try {
                disconnect();
            } catch (ClientException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    @Override
    public final void send(O message) throws ClientException {
        try {
            getMessageIO().write(message, particle);
        } catch (Particle.WriteException e) {
            throw new ClientException("Failed to send message", e);
        }
    }

    @Override
    public final boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public Particle getParticle() {
        if (!initialized) throw new IllegalStateException("Client connection not initialized");
        return particle;
    }

    public String getIPAddress() {
        return socket.getInetAddress().getHostAddress();
    }

    public String getRemoteIPAddress() {
        return ((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress().getHostName();
    }

    public static <I, O> TCPClient<I, O> wrap(Socket socket) throws IOException {
        TCPClient<I, O> client = new TCPClient<>() {
            @Override
            public String getHost() {
                return socket.getInetAddress().getHostName();
            }

            @Override
            public int getPort() {
                return socket.getPort();
            }

            @Override
            public @NotNull List<Task> getRepeatedTasks() {
                return List.of();
            }

            @Override
            public @NotNull MessageIO<I, O> getMessageIO() {
                return new MessageIO<>() {
                    @Override
                    public I read(Particle particle) {
                        return null;
                    }

                    @Override
                    public void write(O message, Particle particle) {

                    }
                };
            }

            @Override
            public void onConnect(Particle particle) throws ClientException {
                // Nothing
            }

            @Override
            public void onMessage(I message, Particle particle) throws ClientException {
                // Nothing
            }

            @Override
            public void onDisconnect() throws ClientException {
                // Nothing
            }
        };
        client.socket = socket;
        client.particle = new Particle(new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
        client.initialized = true;
        return client;
    }

}
