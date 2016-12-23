package apps.simulate;
import java.io.*;

public class FileNoise extends NoiseGenerator
{
  private FileInputStream data = null;

  public FileNoise(String dataFileName) throws FileNotFoundException 
  {
    data = new FileInputStream(new File(dataFileName));
  }

  @Override
  public double next() throws Exception
  {
    double val = data.read();
    if (val == -1) {
      throw new EOFException("Noise File EOF.");
    }
    return val * this.getAmp();
  }

  @Override
  public void close() throws IOException 
  {
    if (data != null) {
      data.close();
    }
  }
}
