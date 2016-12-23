package apps.simulate;
import java.util.Random;

public class GaussianNoise extends NoiseGenerator
{
  private Random rand = null;

  public GaussianNoise(long seed) {
    rand = new Random(seed);
  }

  @Override
  public double next() throws Exception
  {
    return rand.nextGaussian() * this.getAmp();
  }
}