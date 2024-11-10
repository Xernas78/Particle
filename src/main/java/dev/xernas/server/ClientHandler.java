package dev.xernas.server;

import dev.xernas.Particle;
import dev.xernas.client.Client;
import dev.xernas.message.MessageIO;
import dev.xernas.server.exceptions.ServerException;

import java.util.UUID;

public class ClientHandler<I, O> implements Runnable {

    private final Particle particle;
    private final Server<I, O> server;
    private final Client<I, O> client;

    public ClientHandler(Particle particle, Server<I, O> server, Client<I, O> client) {
        this.particle = particle;
        this.server = server;
        this.client = client;
    }

    @Override
    public void run() {
        try {
            UUID clientId = server.newConnectedClient(client);
            MessageIO<I, O> messageIO = server.getMessageIO(clientId);
            server.onClientConnect(clientId, particle);
            try {
                while (client.isConnected()) {
                    try {
                        I message = messageIO.read(particle);
                        if (message != null) server.onMessage(clientId, message, particle);
                    } catch (Particle.ReadException e) {
                        if (Server.isDebugEnabled()) {
                            System.out.println("Failed to read message: " + e.getMessage());
                        }
                    }
                }
            } finally {
                Client<I, O> disconnectedClient = server.removeConnectedClient(clientId);
                if (disconnectedClient != null) server.onClientConnectionEnd(clientId, disconnectedClient);
            }
        } catch (ServerException e) {
            System.out.println(e.getMessage());
        }
    }
}
