package dev.xernas.particle.utils;

import org.jetbrains.annotations.NotNull;

public record Host(String host, int port) {

    @Override
    public @NotNull String toString() {
        return "Host{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }

}
