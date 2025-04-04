package dev.xernas.particle.tasks;

import dev.xernas.particle.client.Client;
import dev.xernas.particle.client.TCPClient;
import dev.xernas.particle.server.Server;
import dev.xernas.particle.server.TCPServer;
import dev.xernas.particle.server.exceptions.ServerException;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class PingTask<I, O> implements Task {

    private Server<I, O> server;
    private Client<I, O> client;

    public PingTask(Server<I, O> server) {
        this.server = server;
    }

    public PingTask(Client<I, O> client) {
        this.client = client;
    }

    @Override
    public void run() throws ServerException {
        if (server != null) server.pingAll();
        if (client != null) client.ping();
    }

    @Override
    public int getInitialDelay() {
        return 100;
    }

    @Override
    public int getPeriod() {
        return 1000;
    }

    @NotNull
    @Override
    public TimeUnit getTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }
}
