package org.seti.simulate;

import java.util.Random;

public class GaussianNoise extends NoiseGenerator {
    private Random rand;

    public GaussianNoise(Random aRand) {
        this.setName("gaussian");
        rand = aRand;
    }

    @Override
    public double next() {
        return rand.nextGaussian() * this.getAmp();
    }
}
