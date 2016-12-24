package apps.simulate;
import java.io.*;

public class FileNoise extends NoiseGenerator
{
  private FileInputStream data = null;

  public FileNoise(String dataFileName) throws FileNotFoundException 
  { 
    this.setName(dataFileName);
    data = new FileInputStream(new File(dataFileName));
  }

  @Override
  public double next() throws Exception
  {
    int intval = data.read();
    if (intval == -1) {
      throw new EOFException("Noise File EOF.");
    }
    byte val = (byte)intval;
    double retVal = val*this.getAmp();
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
