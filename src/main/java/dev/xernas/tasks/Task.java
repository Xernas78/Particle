package dev.xernas.tasks;

import dev.xernas.client.exceptions.ClientException;
import dev.xernas.server.Server;
import dev.xernas.server.exceptions.ServerException;

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
                if (Server.isDebugEnabled()) {
                    System.out.println("Task executed");
                }
            } catch (ServerException | ClientException e) {
                if (Server.isDebugEnabled()) {
                    e.printStackTrace();
                }
            }
        };
    }

}
