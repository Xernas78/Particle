package dev.xernas.particle.client;

import dev.xernas.particle.Particle;
import dev.xernas.particle.client.exceptions.ClientException;
import dev.xernas.particle.message.MessageIO;
import dev.xernas.particle.tasks.Task;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Client<I, O> {

    void connect() throws ClientException;

    void disconnect() throws ClientException;

    void ping();

    void send(O message) throws ClientException;

    boolean isConnected();

    Particle getParticle();

    String getHost();

    int getPort();

    @NotNull
    List<Task> getRepeatedTasks();

    @NotNull
    MessageIO<I, O> getMessageIO();

    void onConnect(Particle particle) throws ClientException;

    void onMessage(I message, Particle particle) throws ClientException;

    void onDisconnect() throws ClientException;
}
