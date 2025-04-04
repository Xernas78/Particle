package dev.xernas.particle.server;

import dev.xernas.particle.Particle;
import dev.xernas.particle.client.Client;
import dev.xernas.particle.client.TCPClient;
import dev.xernas.particle.message.MessageIO;
import dev.xernas.particle.server.exceptions.ServerException;

import java.io.IOException;
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
            server.onClientConnect(clientId, particle);
            MessageIO<I, O> messageIO = server.getMessageIO(clientId);
            try {
                while (client.isConnected()) {
                    try {
                        if (particle.in().available() > 0) {
                            I message = messageIO.read(particle);
                            if (message != null) server.onMessage(clientId, message, particle);
                        }
                    } catch (Particle.ReadException e) {
                        if (TCPServer.isDebugEnabled()) {
                            e.printStackTrace();
                            System.out.println("Failed to read message: " + e.getMessage());
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
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
