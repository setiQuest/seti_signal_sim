package org.seti.simulate;

public class ArrayNoise extends NoiseGenerator {
    public byte[] vals;
    public int index = 0;

    public ArrayNoise(String aName, byte[] data) {
        this.setName(aName);
        this.vals = data;
    }

    @Override
    public double next() throws ArrayIndexOutOfBoundsException {
        return this.vals[index++] * this.getAmp();
    }

    @Override
    public void close() {
    }
}
