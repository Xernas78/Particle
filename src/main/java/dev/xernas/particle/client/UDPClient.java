package dev.xernas.particle.client;

import dev.xernas.particle.Particle;
import dev.xernas.particle.client.exceptions.ClientException;
import dev.xernas.particle.message.MessageIO;
import dev.xernas.particle.tasks.PingTask;

import java.io.*;
import java.net.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class UDPClient<I, O> implements Client<I, O> {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private boolean initialized = false;
    private DatagramSocket socket;
    private Particle particle;

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
        socket.close();
        onDisconnect();
    }

    @Override
    public boolean ping() {
        try {
            Particle.sendUDP(1, socket);
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
            Particle.sendUDP(data, socket);
        } catch (Particle.WriteException e) {
            throw new ClientException("Failed to send message", e);
        }
    }

    @Override
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public Particle getParticle() {
        return particle;
    }

}
