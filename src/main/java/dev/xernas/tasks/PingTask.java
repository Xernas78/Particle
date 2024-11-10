package dev.xernas.tasks;

import dev.xernas.client.Client;
import dev.xernas.server.Server;
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
    public void run() {
        if (server != null) server.ping();
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
