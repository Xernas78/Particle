package dev.xernas.particle.tasks;

import dev.xernas.particle.client.exceptions.ClientException;
import dev.xernas.particle.server.Server;
import dev.xernas.particle.server.exceptions.ServerException;

import java.util.concurrent.TimeUnit;

public interface Task {

    void run() throws ServerException, ClientException;

    int getInitialDelay();

    int getPeriod();

    TimeUnit getTimeUnit();

    default Runnable asRunnable() {
        return () -> {
            try {
                run();
            } catch (ServerException | ClientException e) {
                if (Server.isDebugEnabled()) {
                    e.printStackTrace();
                }
            }
        };
    }

}
