package apps.simulate;

/**
 * DataSimulator produces complex-valued 8-bit sample data like from ATA beamformer.
 **/

import java.util.Random;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DataSimulator 
{

	private static int mExpectedArgs = 12;
	private static double mDriftDivisor = 1024;


	private static int simulationVersion = 4;
	private static String simulationVersionDate = "15 Dec 2016";

	//final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

  public static void main (String[] args) throws Exception
	{

		// check command line arguments
		if (args.length != mExpectedArgs) PrintHelp();

		int nextarg = 0;
		int sigmaN = Integer.parseInt(args[nextarg++]);
		double deltaPhiRad = Double.parseDouble(args[nextarg++]) / 180.0 * Math.PI;
		double SNR = Double.parseDouble(args[nextarg++]);
		double drift = Double.parseDouble(args[nextarg++]);
		double driftRateDerivate = Double.parseDouble(args[nextarg++]);
		double sigmaSquiggle = Double.parseDouble(args[nextarg++]);
		int outputLength = Integer.parseInt(args[nextarg++]);
		String ampModType = args[nextarg++];
		int ampModPeriod = Integer.parseInt(args[nextarg++]);
		double ampModDuty = Double.parseDouble(args[nextarg++]);
		String signalClass = args[nextarg++];

		String filename = args[nextarg++];

		// test argument values -- tbd

		System.out.println("args:\n" 
			+ "sigmaN = " + sigmaN + "\n"
			+ "deltaPhiRad = " + deltaPhiRad + "\n"
			+ "SNR = " + SNR + "\n"
			+ "drift = " + drift + "\n"
			+ "driftRateDerivate = " + driftRateDerivate + "\n"
			+ "sigmaSquiggle = " + sigmaSquiggle + "\n"
			+ "outputLength = " + outputLength + "\n"
			+ "ampModType = " + ampModType + "\n"
			+ "ampModPeriod (only valid if type != none) = " + ampModPeriod + "\n"
			+ "ampModDuty (only if type = 'square') = " + ampModDuty + "\n"
			+ "signalClass = " + signalClass + "\n"
			+ "filename = " + filename + "\n");

		// generate Random seed
		long seed = System.currentTimeMillis();

		// create output file
		FileOutputStream FOS = new FileOutputStream(new File(filename));
		
		// create & write data
		DataSimulator object = new DataSimulator(
			sigmaN, deltaPhiRad, SNR, drift, driftRateDerivate, sigmaSquiggle, outputLength, ampModType, ampModPeriod, ampModDuty, FOS, signalClass, seed);
 
		// close output file
		FOS.close();
	}

	public DataSimulator(int sigN, double dPhi, double SNR, 
		double drift, double driftRateDerivate, double jitter, int len,
		String ampModType, double ampModPeriod, double ampModDuty, OutputStream OS, String signalClass, long seed) throws IOException
	{
		Random rand = new Random(seed);
		
		// convert SNR from number of STD to counts
		SNR = SNR * sigN;

		// put drift rate and signal jitter into (approximate) Hz/s assuming mDriftDivisor samples in FFT
		// using small angle approximation for sinDrift
		double sinDrift = drift / mDriftDivisor;
		if (Math.abs(sinDrift) > 1) sinDrift = Math.signum(sinDrift);
		double cosDrift = Math.sqrt(1 - sinDrift * sinDrift);

		// dPhi is average phase angle (radians) between complex-valued samples
		// pi radians -> Nyquist frequency
		double cosPhi = Math.cos(dPhi);
		double sinPhi = Math.sin(dPhi);

		// keeps track of signal and sample values from most recent data point
		double signalX = rand.nextGaussian() * sigN;
		double signalY = rand.nextGaussian() * sigN;
		
		double ampPhase = rand.nextFloat();
		double ampPhaseSquare = ampPhase*ampModPeriod;
		double ampPhaseSine = (ampPhase - 0.5)*Math.PI;

		double signalAmpFactor = SNR;

		//Before we simulate, let's write out a line, in JSON, to 
		//capture the conditions

		Map<String, Object> setup = new HashMap<String, Object>();
		setup.put("sigma_noise", sigN);
		setup.put("delta_phi_rad", dPhi);
		setup.put("signal_to_noise_ratio", SNR);
		setup.put("drift",drift);
		setup.put("drift_rate_derivative",driftRateDerivate);
		setup.put("jitter",jitter);
		setup.put("len",len);
		setup.put("amp_modulation_type", ampModType);
		setup.put("amp_modulation_period", ampModPeriod);
		setup.put("amp_modulation_duty", ampModDuty);
		setup.put("amp_phase", ampPhase);
		setup.put("amp_phase_square", ampPhaseSquare);
		setup.put("amp_phase_sine", ampPhaseSine);
		setup.put("signal_classification", signalClass);
		setup.put("current_time", seed);
		setup.put("drift_divisor", mDriftDivisor);
		setup.put("initial_sine_drift", sinDrift);
		setup.put("initial_cosine_drift", cosDrift);
		setup.put("simulator_software_version", simulationVersion);
		setup.put("simulator_software_version_date", simulationVersionDate);
		setup.put("uuid", UUID.randomUUID().toString());
		ObjectMapper mapper = new ObjectMapper();

		String json = mapper.writeValueAsString(setup);
		System.out.println(json);
		OS.write(mapper.writeValueAsBytes(setup));
		OS.write('\n');

		// loop over samples
		for (int i = 0; i < len; ++i)
		{

			//allow for drift rate to change
			sinDrift = sinDrift + (driftRateDerivate/1000000.0) / mDriftDivisor;
			if (Math.abs(sinDrift) > 1) sinDrift = Math.signum(sinDrift);
			cosDrift = Math.sqrt(1 - sinDrift * sinDrift);

			// propagate signal frequency to next value using drift
			double temp = cosPhi * cosDrift - sinPhi * sinDrift;
			sinPhi      = cosPhi * sinDrift + sinPhi * cosDrift;
			cosPhi = temp;

			// propagate signal frequency to next value by adding jitter
			// frequency does random walk
			double sinDelta = 2.0 * (rand.nextDouble() - 0.5) * jitter;
			double cosDelta = Math.sqrt(1 - sinDelta * sinDelta);
			temp   = cosPhi * cosDelta - sinPhi * sinDelta;
			sinPhi = cosPhi * sinDelta + sinPhi * cosDelta;
			cosPhi = temp;

			// normalization (potential optimization here)
			double mag = Math.sqrt(cosPhi * cosPhi + sinPhi * sinPhi);
			cosPhi /= mag;
			sinPhi /= mag;

			// propagate current signal value to next using updated frequency
			double tmpX = signalX * cosPhi - signalY * sinPhi;
			signalY        = signalX * sinPhi + signalY * cosPhi;
			signalX = tmpX;
			mag = Math.sqrt(signalX * signalX + signalY * signalY);
			signalX /= mag;
			signalY /= mag;


			// this creates sidebands in the spectrogram and need to
			// make sure to have a large enough periodicity so that sidebands are not observed
			// 
			signalAmpFactor = SNR;
			if (ampModType.equals("square")){					
					if( (i - ampPhaseSquare) % ampModPeriod > ampModPeriod*ampModDuty ) {
						signalAmpFactor = 0;
					}
			}
			else if (ampModType.equals("sine")) {
				signalAmpFactor = signalAmpFactor * Math.sin(2.0*Math.PI*i/ampModPeriod + ampPhaseSine);
			}

			// generate noise values
			double dNoiseX = rand.nextGaussian() * sigN;
			double dNoiseY = rand.nextGaussian() * sigN;
			
			double Xval = dNoiseX + signalX * signalAmpFactor;
			double Yval = dNoiseY + signalY * signalAmpFactor	;
			//double Xvald = Xval;
			//double Yvald = Yval;

			// if the value hits the rail, then truncate it
			if (Math.abs(Xval) > 127) Xval = Math.signum(Xval) * 127;
			if (Math.abs(Yval) > 127) Yval = Math.signum(Yval) * 127;

			byte X = (byte) Xval;
			byte Y = (byte) Yval;
			byte[] sample = new byte[]{X, Y};
		
			// write sample to OutputStream
			OS.write(sample);

			// if (i < 10) {
			// 	if (i == 0) { 
			// 		System.out.println("Printing out the first 10 samples");
			// 	}
			// 	System.out.println(Xvald + " , " + Yvald + " ; " +  Xval + " , " + Yval  + " ; " +  X + " , " + Y + "; 0x:" + bytesToHex(sample));
			// 	//System.out.println(bytesToHex(sample));
			// }
		}
    }

	public static void PrintHelp()
	{
		
		System.out.println("\n\t" + mExpectedArgs + " arguments expected\n\n"
			+ "\tsigmaNoise deltaPhi SNR  drift sigmaSquiggle outputLength ampModType ampModPeriod ampModDuty signalClass filename\n\n"
			+ "\twhere\n\n"
			+ "\t  sigmaNoise\t (double 0 - 127) noise mean power, 13 is good choice\n"
			+ "\t  deltaPhi\t(double -180 - 180) average phase angle (degrees) between samples\n"
			+ "\t  SNR\t(double) Signal amplitude in terms of sigma_noise\n"
			+ "\t  drift\t(double) Average drift rate of signal\n"
			+ "\t  driftRateDerivate\t(double) Change of drift rate per 1m samples\n"
			+ "\t  sigmaSquiggle\t(double) amplitude of squiggle noise\n"
			+ "\t  outputLength\t(int > 2) number of complex-valued samples to write to output\n"
			+ "\t  ampModType\t(string = 'none','square','sine') specifies how the amplitude is modulated\n"
			+ "\t  ampModPeriod\t(int > 2) periodicity of amplitude modulation, in same units of outputLength\n"
			+ "\t  ampModDuty\t(double betweeen 0 and 1) duty cycle of square wave amplitude modulation.\n"
			+ "\t  signalClass\t(string) a name to classify the signal.\n"
			+ "\t  filename\t(string) output filename for data\n");

		System.exit(0);
	}

// public static String bytesToHex(byte[] bytes) {
 //    char[] hexChars = new char[bytes.length * 2];
 //    for ( int j = 0; j < bytes.length; j++ ) {
 //        int v = bytes[j] & 0xFF;
 //        hexChars[j * 2] = hexArray[v >>> 4];
 //        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
 //    }
 //    return new String(hexChars);
// }

}

