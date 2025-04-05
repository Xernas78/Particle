package dev.xernas.particle.server;

import dev.xernas.particle.Particle;
import dev.xernas.particle.client.Client;
import dev.xernas.particle.client.UDPClient;
import dev.xernas.particle.client.exceptions.ClientException;
import dev.xernas.particle.server.exceptions.ServerException;
import dev.xernas.particle.tasks.PingTask;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class UDPServer<I, O> implements Server<I, O> {

    private final Map<UUID, Client<I, O>> connected = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private DatagramSocket server;
    private boolean running = false;

    @Override
    public void listen() throws ServerException {
        try (DatagramSocket server = new DatagramSocket(getPort())) {
            running = true;
            this.server = server;
            onServerStart();

            PingTask<I, O> pingTask = new PingTask<>(this);
            scheduler.scheduleAtFixedRate(pingTask.asRunnable(), pingTask.getInitialDelay(), pingTask.getPeriod(), pingTask.getTimeUnit());

            getRepeatedTasks().forEach(task -> scheduler.scheduleAtFixedRate(task.asRunnable(), task.getInitialDelay(), task.getPeriod(), task.getTimeUnit()));

            byte[] buffer = new byte[1024];
            while (isRunning()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                server.receive(packet);
                Particle packetParticle = new Particle(new DataInputStream(new ByteArrayInputStream(packet.getData(), 0, packet.getLength())));
                UDPClient<I, O> client = UDPClient.wrap(packet);
                new Thread(new ClientHandler<>(packetParticle, this, client)).start();
            }
        } catch (IOException e) {
            throw new ServerException("Failed to start server", e);
        } finally {
            shutdownScheduler();
            onServerStop();
        }
    }

    private void shutdownScheduler() {
        System.out.println("Shutting down scheduler");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    @Override
    public UUID newConnectedClient(Client<I, O> client) throws ServerException {
        UUID clientId;
        do {
            clientId = UUID.randomUUID();
        } while (connected.containsKey(clientId));
        connected.put(clientId, client);
        return clientId;
    }

    @Override
    public Client<I, O> removeConnectedClient(UUID clientId) throws ServerException {
        return connected.remove(clientId);
    }

    @Override
    public void forceDisconnect(UUID clientId) throws ServerException {
        Client<I, O> client = getClient(clientId);
        if (client == null) {
            throw new ServerException("Client not found");
        }

        try {
            onClientDisconnect(clientId, client.getParticle());
        } catch (ServerException ignore) {}
        removeConnectedClient(clientId);
        try {
            client.disconnect();
        } catch (ClientException e) {
            throw new ServerException("Failed to disconnect client", e);
        }
    }

    @Override
    public boolean ping(UUID clientId) throws ServerException {
        UDPClient<I, O> client = (UDPClient<I, O>) getClient(clientId);
        if (client == null) {
            throw new ServerException("Client not found");
        }
        try {
            Particle.sendUDP(1, server, client.toHost());
            return true;
        } catch (Particle.WriteException e) {
            try {
                forceDisconnect(clientId);
            } catch (ServerException ex) {
                System.out.println(ex.getMessage());
            }
            return false;
        }
    }

    @Override
    public void pingAll() throws ServerException {
        List<UUID> toPing = new ArrayList<>(connected.keySet());
        for (UUID clientId : toPing) {
            if (!ping(clientId)) forceDisconnect(clientId);
        }
    }

    @Override
    public void send(UUID clientId, O message) throws ServerException {
        UDPClient<I, O> client = (UDPClient<I, O>) getClient(clientId);
        if (client == null) {
            System.err.println("Client not found");
            return;
        }
        try {
            ByteArrayOutputStream packetData = new ByteArrayOutputStream(1024);
            Particle packetParticleToSend = new Particle(new DataOutputStream(packetData));
            getMessageIO(clientId).write(message, packetParticleToSend);
            byte[] data = packetData.toByteArray();
            Particle.sendUDP(data, server, client.toHost());
        } catch (Particle.WriteException e) {
            throw new ServerException("Failed to send message", e);
        }
    }

    @Override
    public void broadcast(O message) throws ServerException {
        for (UUID clientId : connected.keySet()) send(clientId, message);
    }

    @Override
    public Client<I, O> getClient(UUID clientId) {
        return connected.get(clientId);
    }

    @Override
    public Map<UUID, Client<I, O>> getConnectedClients() {
        return connected;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
