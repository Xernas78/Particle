package dev.xernas.message;

import dev.xernas.Particle;

public class StringMessageIO implements MessageIO<String, String> {

    @Override
    public String read(Particle particle) throws Particle.ReadException {
        return particle.readString();
    }

    @Override
    public void write(String string, Particle particle) throws Particle.WriteException {
        particle.writeString(string);
    }
}
