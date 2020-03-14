package org.seti.simulate;

public class NoiseGenerator {
    private double amp = 1.0;
    private String name = "";

    public double next() throws Exception {
        return 0.0;
    }

    public String getName() {
        return name;
    }

    public void setName(String aName) {
        name = aName;
    }

    public double getAmp() {
        return amp;
    }

    public void setAmp(double val) {
        amp = val;
    }

    public void close() {
    }
}
