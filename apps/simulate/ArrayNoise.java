package apps.simulate;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ArrayNoise extends NoiseGenerator
{
  public byte[] vals = null;
  public int index = 0;

  public ArrayNoise(String aName, byte[] data) throws IOException 
  { 
    this.setName(aName);
    this.vals = data;
  }

  @Override
  public double next() throws ArrayIndexOutOfBoundsException
  {
    return this.vals[index++] * this.getAmp();
  }

  @Override
  public void close() throws IOException 
  {
    return;
  }
}
