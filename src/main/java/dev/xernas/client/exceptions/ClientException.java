package dev.xernas.client.exceptions;

import dev.xernas.ParticleException;

public class ClientException extends ParticleException {

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
