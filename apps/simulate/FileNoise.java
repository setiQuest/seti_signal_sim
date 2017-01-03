package apps.simulate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileNoise extends NoiseGenerator
{
  private byte[] vals = null;
  private int index = 0;

  public FileNoise(String dataFileName) throws IOException 
  { 
    this.setName(dataFileName);
    vals = Files.readAllBytes((Paths.get(dataFileName)));
  }

  @Override
  public double next() throws ArrayIndexOutOfBoundsException
  {
    byte val = vals[index++];
    double retVal = val*this.getAmp();
    return val * this.getAmp();
  }

  @Override
  public void close() throws IOException 
  {
    return;
  }
}
