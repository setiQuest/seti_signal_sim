package apps.simulate;
import java.util.Random;

public class GaussianNoise extends NoiseGenerator
{
  private Random rand = null;

  public GaussianNoise(Random aRand) {
    this.setName("gaussian");
    rand = aRand;
  }

  @Override
  public double next() throws Exception
  {
    return rand.nextGaussian() * this.getAmp();
  }
}