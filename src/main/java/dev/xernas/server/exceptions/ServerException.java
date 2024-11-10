package dev.xernas.server.exceptions;

import dev.xernas.ParticleException;

public class ServerException extends ParticleException {

    public ServerException(String message) {
        super(message);
    }

    public ServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ServerException wrap(Throwable cause) {
        return new ServerException(cause.getMessage(), cause);
    }

}