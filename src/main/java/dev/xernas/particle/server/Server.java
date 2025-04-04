package dev.xernas.particle.server;

import dev.xernas.particle.Particle;
import dev.xernas.particle.client.Client;
import dev.xernas.particle.client.TCPClient;
import dev.xernas.particle.message.MessageIO;
import dev.xernas.particle.server.exceptions.ServerException;
import dev.xernas.particle.tasks.Task;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface Server<I, O> {

    void listen() throws ServerException;

    int getPort();

    @NotNull
    List<Task> getRepeatedTasks();

    MessageIO<I, O> getMessageIO(UUID clientId);

    void onServerStart() throws ServerException;

    void onClientConnect(UUID clientId, Particle particle) throws ServerException;

    void onMessage(UUID clientId, I message, Particle particle) throws ServerException;

    void onClientDisconnect(UUID clientId, Particle particle) throws ServerException;

    void onClientConnectionEnd(UUID clientId, Client<I, O> disconnectedClient) throws ServerException;

    void onServerStop() throws ServerException;

    UUID newConnectedClient(Client<I, O> client) throws ServerException;

    Client<I, O> removeConnectedClient(UUID clientId) throws ServerException;

    void forceDisconnect(UUID clientId) throws ServerException;

    boolean ping(UUID clientId) throws ServerException;

    void pingAll() throws ServerException;

    void send(UUID clientId, O message) throws ServerException;

    void broadcast(O message) throws ServerException;

    Client<I, O> getClient(UUID clientId);

    Map<UUID, Client<I, O>> getConnectedClients();

    boolean isRunning();

}
