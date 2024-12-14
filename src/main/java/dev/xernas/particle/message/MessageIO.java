package dev.xernas.particle.message;

import dev.xernas.particle.Particle;

public interface MessageIO<I, O> {

    I read(Particle particle) throws Particle.ReadException;

    void write(O message, Particle particle) throws Particle.WriteException;

}
