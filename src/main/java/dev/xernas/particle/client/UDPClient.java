package dev.xernas.particle.client;

import dev.xernas.particle.Particle;
import dev.xernas.particle.client.exceptions.ClientException;
import dev.xernas.particle.message.MessageIO;
import dev.xernas.particle.tasks.PingTask;
import dev.xernas.particle.tasks.Task;
import dev.xernas.particle.utils.Host;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class UDPClient<I, O> implements Client<I, O> {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private boolean initialized = false;
    private Host host;
    private DatagramSocket socket;
    private Particle particle;

    private boolean connected = false;

    @Override
    public void connect() throws ClientException {
        if (initialized) throw new ClientException("Client already initialized");
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(getHost());
            socket.connect(address, getPort());
            this.socket = socket;
            this.particle = new Particle(true);
            boolean success = ping();
            if (!success) {
                throw new ClientException("Failed to ping server");
            }
            this.connected = true;
            onConnect(particle);

            PingTask<I, O> pingTask = new PingTask<>(this);
            scheduler.scheduleAtFixedRate(pingTask.asRunnable(), pingTask.getInitialDelay(), pingTask.getPeriod(), pingTask.getTimeUnit());
            getRepeatedTasks().forEach(task -> scheduler.scheduleAtFixedRate(task.asRunnable(), task.getInitialDelay(), task.getPeriod(), task.getTimeUnit()));

            MessageIO<I, O> messageIO = getMessageIO();
            initialized = true;

            byte[] packetReceiverBuffer = new byte[1024];
            while (isConnected()) {
                try {
                    DatagramPacket packet = new DatagramPacket(packetReceiverBuffer, packetReceiverBuffer.length);
                    socket.receive(packet);
                    Particle packetParticle = new Particle(new DataInputStream(new ByteArrayInputStream(packet.getData(), 0, packet.getLength())));
                    I message = messageIO.read(packetParticle);
                    if (message != null) onMessage(message, packetParticle);
                } catch (Particle.ReadException ignore) {}
            }
        } catch (IOException e) {
            throw new ClientException("Failed to connect to server", e);
        }
        finally {
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
    public void disconnect() throws ClientException {
        if (socket != null) socket.close();
        connected = false;
        onDisconnect();
    }

    @Override
    public boolean ping() {
        try {
            Particle.sendUDP(1, socket, new Host(getHost(), getPort()));
            return true;
        } catch (Particle.WriteException e) {
            try {
                disconnect();
            } catch (ClientException ex) {
                System.out.println(ex.getMessage());
            }
            return false;
        }
    }

    @Override
    public void send(O message) throws ClientException {
        try {
            ByteArrayOutputStream packetData = new ByteArrayOutputStream(1024);
            Particle packetParticleToSend = new Particle(new DataOutputStream(packetData));
            getMessageIO().write(message, packetParticleToSend);
            byte[] data = packetData.toByteArray();
            Particle.sendUDP(data, socket, new Host(getHost(), getPort()));
        } catch (Particle.WriteException e) {
            throw new ClientException("Failed to send message", e);
        }
    }

    @Override
    public boolean isConnected() {
        return (socket != null && socket.isConnected() && !socket.isClosed()) || connected;
    }

    @Override
    public Particle getParticle() {
        return particle;
    }

    public Host toHost() {
        return host;
    }

    public static <I, O> UDPClient<I, O> wrap(DatagramPacket packet) {
        UDPClient<I, O> client = new UDPClient<>() {
            @Override
            public String getHost() {
                return packet.getAddress().getHostName();
            }

            @Override
            public int getPort() {
                return packet.getPort();
            }

            @Override
            public @NotNull List<Task> getRepeatedTasks() {
                return List.of();
            }

            @Override
            public @NotNull MessageIO<I, O> getMessageIO() {
                return new MessageIO<>() {
                    @Override
                    public I read(Particle particle) throws Particle.ReadException {
                        return null;
                    }

                    @Override
                    public void write(O message, Particle particle) throws Particle.WriteException {

                    }
                };
            }

            @Override
            public void onConnect(Particle particle) throws ClientException {
                //Nothing
            }

            @Override
            public void onMessage(Object message, Particle particle) throws ClientException {
                //Nothing
            }

            @Override
            public void onDisconnect() throws ClientException {
                //Nothing
            }
        };
        client.host = new Host(packet.getAddress().getHostName(), packet.getPort());
        client.particle = new Particle(true);
        client.initialized = true;
        return client;
    }

}
