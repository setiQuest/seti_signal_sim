package apps.simulate;

public class NoiseGenerator 
{
  private double amp = 1.0;

  public double next() throws Exception
  {
    return 0.0;
  }

  public void setAmp(double val) 
  {
    amp = val;
  }
  public double getAmp(){
    return amp;
  }

  public void close() throws Exception 
  {
    return;
  }
}
