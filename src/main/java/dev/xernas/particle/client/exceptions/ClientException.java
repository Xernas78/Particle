package dev.xernas.particle.client.exceptions;

import dev.xernas.particle.ParticleException;

public class ClientException extends ParticleException {

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
