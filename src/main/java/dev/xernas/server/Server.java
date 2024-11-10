package dev.xernas.server;

import dev.xernas.Particle;
import dev.xernas.client.Client;
import dev.xernas.client.exceptions.ClientException;
import dev.xernas.message.MessageIO;
import dev.xernas.server.exceptions.ServerException;
import dev.xernas.tasks.PingTask;
import dev.xernas.tasks.Task;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class Server<I, O> {

    private final Map<UUID, Client<I, O>> connected = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private boolean running = false;
    private static boolean debug = false;

    public final void listen() throws ServerException {
        try (ServerSocket server = new ServerSocket(getPort())) {
            running = true;
            onServerStart();

            PingTask<I, O> pingTask = new PingTask<>(this);
            scheduler.scheduleAtFixedRate(pingTask.asRunnable(), pingTask.getInitialDelay(), pingTask.getPeriod(), pingTask.getTimeUnit());

            getRepeatedTasks().forEach(task -> scheduler.scheduleAtFixedRate(task.asRunnable(), task.getInitialDelay(), task.getPeriod(), task.getTimeUnit()));

            while (isRunning()) {
                Client<I, O> client = Client.wrap(server.accept());
                new Thread(new ClientHandler<>(client.getParticle(), this, client)).start();
            }

            onServerStop();
        } catch (IOException e) {
            throw new ServerException("Failed to start server", e);
        } finally {
            shutdownScheduler();
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

    public abstract int getPort();

    @NotNull
    public abstract List<Task> getRepeatedTasks();

    @NotNull
    public abstract MessageIO<I, O> getMessageIO(UUID clientId);

    public abstract void onServerStart() throws ServerException;

    public abstract void onClientConnect(UUID clientId, Particle particle) throws ServerException;

    public abstract void onMessage(UUID clientId, I message, Particle particle) throws ServerException;

    public abstract void onClientDisconnect(UUID clientId, Particle particle) throws ServerException;

    public abstract void onClientConnectionEnd(UUID clientId, Client<I, O> disconnectedClient) throws ServerException;

    public abstract void onServerStop() throws ServerException;

    public final UUID newConnectedClient(Client<I, O> client) {
        UUID clientId;
        do {
            clientId = UUID.randomUUID();
        } while (connected.containsKey(clientId));
        connected.put(clientId, client);
        return clientId;
    }

    public final Client<I, O> removeConnectedClient(UUID clientId) {
        return connected.remove(clientId);
    }

    public final void forceDisconnectClient(UUID clientId) throws ServerException {
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

    public final void ping() {
        List<UUID> toPing = new ArrayList<>(connected.keySet());
        for (UUID clientId : toPing) {
            try {
                if (!ping(clientId)) forceDisconnectClient(clientId);
            } catch (ServerException e) {
                System.out.println(e.getMessage());
            }
        }
    }

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

    public final void broadcast(O message) throws ServerException {
        for (UUID clientId : connected.keySet()) send(clientId, message);
    }

    public static void debug(boolean debug) {
        if (debug) {
            System.out.println("Debugging");
        }
        Server.debug = debug;
    }

    public final Client<I, O> getClient(UUID clientId) {
        return connected.get(clientId);
    }

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
