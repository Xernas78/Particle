package dev.xernas.particle.server;

import dev.xernas.particle.Particle;
import dev.xernas.particle.client.Client;
import dev.xernas.particle.client.TCPClient;
import dev.xernas.particle.client.exceptions.ClientException;
import dev.xernas.particle.message.MessageIO;
import dev.xernas.particle.server.exceptions.ServerException;
import dev.xernas.particle.tasks.PingTask;
import dev.xernas.particle.tasks.Task;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class TCPServer<I, O> implements Server<I, O> {

    private final Map<UUID, Client<I, O>> connected = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private boolean running = false;
    private static boolean debug = false;

    @Override
    public final void listen() throws ServerException {
        try (ServerSocket server = new ServerSocket(getPort())) {
            running = true;
            onServerStart();

            PingTask<I, O> pingTask = new PingTask<>(this);
            scheduler.scheduleAtFixedRate(pingTask.asRunnable(), pingTask.getInitialDelay(), pingTask.getPeriod(), pingTask.getTimeUnit());

            getRepeatedTasks().forEach(task -> scheduler.scheduleAtFixedRate(task.asRunnable(), task.getInitialDelay(), task.getPeriod(), task.getTimeUnit()));

            while (isRunning()) {
                TCPClient<I, O> client = TCPClient.wrap(server.accept());
                new Thread(new ClientHandler<>(client.getParticle(), this, client)).start();
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

    public abstract MessageIO<I, O> getMessageIO(UUID clientId);

    public abstract void onClientDisconnect(UUID clientId, Particle particle) throws ServerException;

    @Override
    public final UUID newConnectedClient(Client<I, O> client) {
        UUID clientId;
        do {
            clientId = UUID.randomUUID();
        } while (connected.containsKey(clientId));
        connected.put(clientId, client);
        return clientId;
    }

    @Override
    public final Client<I, O> removeConnectedClient(UUID clientId) {
        return connected.remove(clientId);
    }

    @Override
    public final void forceDisconnect(UUID clientId) throws ServerException {
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
    public final boolean ping(UUID clientId) throws ServerException {
        Client<I, O> client = getClient(clientId);
        if (client == null) {
            throw new ServerException("Client not found");
        }
        try {
            client.getParticle().writeInt(0);
        } catch (Particle.WriteException e) {
            return false;
        }
        return true;
    }

    @Override
    public final void pingAll() throws ServerException {
        List<UUID> toPing = new ArrayList<>(connected.keySet());
        for (UUID clientId : toPing) {
            if (!ping(clientId)) forceDisconnect(clientId);
        }
    }

    @Override
    public final void send(UUID clientId, O message) throws ServerException {
        Client<I, O> client = getClient(clientId);
        if (client == null) {
            System.err.println("Client not found");
            return;
        }
        try {
            getMessageIO(clientId).write(message, client.getParticle());
        } catch (Particle.WriteException e) {
            throw new ServerException("Failed to send message", e);
        }
    }

    @Override
    public final void broadcast(O message) throws ServerException {
        for (UUID clientId : connected.keySet()) send(clientId, message);
    }

    public static void debug(boolean debug) {
        if (debug) {
            System.out.println("Debugging");
        }
        TCPServer.debug = debug;
    }

    @Override
    public final Client<I, O> getClient(UUID clientId) {
        return connected.get(clientId);
    }

    @Override
    public final Map<UUID, Client<I, O>> getConnectedClients()
    {
        return connected;
    }

    public final void stop() {
        running = false;
    }

    public final boolean isRunning() {
        return running;
    }

    public static boolean isDebugEnabled() {
        return debug;
    }
}
