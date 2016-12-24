package apps.simulate;

/**
 * DataSimulator produces complex-valued 8-bit sample data like from ATA beamformer.
 **/

import java.util.Random;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

import java.util.UUID;

import apps.simulate.NoiseGenerator;
import apps.simulate.GaussianNoise;
import apps.simulate.FileNoise;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DataSimulator 
{

	private static int mExpectedArgs = 13;
	private static double mDriftDivisor = 1024;


	private static int simulationVersion = 7;
	private static String simulationVersionDate = "23 Dec 2016";

	//final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

  public static void main (String[] args) throws Exception
	{

		// check command line arguments
		if (args.length != mExpectedArgs) PrintHelp();

		int nextarg = 0;
		int sigmaN = Integer.parseInt(args[nextarg++]);
		String noiseFile = args[nextarg++];
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
			+ "noiseFile = " + noiseFile + "\n"
			+ "deltaPhiRad = " + deltaPhiRad + "\n"
			+ "SNR = " + SNR + "\n"
			+ "drift = " + drift + "\n"
			+ "driftRateDerivate = " + driftRateDerivate + "\n"
			+ "sigmaSquiggle = " + sigmaSquiggle + "\n"
			+ "outputLength = " + outputLength + "\n"
			+ "ampModType = " + ampModType + "\n"
			+ "ampModPeriod (only valid if type != none) = " + ampModPeriod + "\n"
			+ "ampModDuty (only if type = 'square' or 'brightpixel') = " + ampModDuty + "\n"
			+ "signalClass = " + signalClass + "\n"
			+ "filename = " + filename + "\n");

		// generate Random seed
		long seed = System.currentTimeMillis();

		// create output file
		FileOutputStream FOS = new FileOutputStream(new File(filename));
		NoiseGenerator noiseGen = null;

		if (noiseFile.equals("")){
			noiseGen = new GaussianNoise(System.currentTimeMillis());
			noiseGen.setAmp(sigmaN);
		}
		else {
			noiseGen = new FileNoise(noiseFile);
			noiseGen.setAmp(1.0);
		}
		

		// create & write data
		DataSimulator object = new DataSimulator(
			noiseGen, sigmaN, deltaPhiRad, SNR, drift, driftRateDerivate, sigmaSquiggle, outputLength, ampModType, ampModPeriod, ampModDuty, FOS, signalClass, seed);
 
		// close output file
		FOS.close();

		//close noise generator
		noiseGen.close();
	}

	public DataSimulator(NoiseGenerator noiseGen, int sigN, double dPhi, double SNR, 
		double drift, double driftRateDerivate, double jitter, int len,
		String ampModType, double ampModPeriod, double ampModDuty, OutputStream OS, String signalClass, long seed) throws Exception
	{
		Random rand = new Random(seed);


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
		double signalX = rand.nextGaussian();
		double signalY = rand.nextGaussian();
		
		double ampPhase = rand.nextFloat();
		//we do this to ensure that the bright pixel doesn't happen 
		//*right* at the beginning or end of the simulation.
		//This is
		//especially important for it not to start at the end of
		//this simulation, because it would actually wrap around
		//and the signal would be split between the very beginning and
		//very end.
		//typical "brightpixel" signals will last between
		//0.1 second up to 2 seconds.** This is now specifically tailored
		//for typical ACA-sized simulations (128 raster lines * 6144 samples/raster line). 
		//We prevent the ON phase of 
		//amplitude modulation from "starting" within the last
		//2% of the simulated waveform 
		//
		//** note: "second" here means the time length of a raster line
		//in ACA files that we're simulating. This does not exactly match
		//the ACA files, but it makes it easier to visualize and discuss. 
		double maxBPPhase = 0.98;
		if (ampModType.equals("brightpixel")){
				ampPhase = ampPhase*maxBPPhase;
		}

		double ampPhaseSquare = ampPhase*ampModPeriod;
		double ampPhaseSine = (ampPhase - 0.5)*Math.PI;

		double signalAmpFactor = SNR * sigN;

		//Before we simulate, let's write out a line, in JSON, to 
		//capture the conditions

		Map<String, Object> privateHeader = new HashMap<String, Object>();
		privateHeader.put("sigma_noise", sigN);
		privateHeader.put("noise_name", noiseGen.getName());
		privateHeader.put("delta_phi_rad", dPhi);
		privateHeader.put("signal_to_noise_ratio", SNR);
		privateHeader.put("drift",drift);
		privateHeader.put("drift_rate_derivative",driftRateDerivate);
		privateHeader.put("jitter",jitter);
		privateHeader.put("len",len);
		privateHeader.put("amp_modulation_type", ampModType);
		privateHeader.put("amp_modulation_period", ampModPeriod);
		privateHeader.put("amp_modulation_duty", ampModDuty);
		privateHeader.put("amp_phase", ampPhase);
		privateHeader.put("amp_phase_square", ampPhaseSquare);
		privateHeader.put("amp_phase_sine", ampPhaseSine);
		privateHeader.put("signal_classification", signalClass);
		privateHeader.put("current_time", seed);
		privateHeader.put("drift_divisor", mDriftDivisor);
		privateHeader.put("initial_sine_drift", sinDrift);
		privateHeader.put("initial_cosine_drift", cosDrift);
		privateHeader.put("simulator_software_version", simulationVersion);
		privateHeader.put("simulator_software_version_date", simulationVersionDate);
		privateHeader.put("uuid", UUID.randomUUID().toString());
		ObjectMapper mapper = new ObjectMapper();

		String json = mapper.writeValueAsString(privateHeader);
		System.out.println(json);
		OS.write(mapper.writeValueAsBytes(privateHeader));
		OS.write('\n');


		Map<String, Object> publicHeader = new HashMap<String, Object>();
		publicHeader.put("signal_classification", signalClass);
		publicHeader.put("uuid", privateHeader.get("uuid"));

		json = mapper.writeValueAsString(publicHeader);
		System.out.println(json);
		OS.write(mapper.writeValueAsBytes(publicHeader));
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
			signalAmpFactor = SNR * sigN;
			if (ampModType.equals("square") || ampModType.equals("brightpixel")){					
					if( (i - ampPhaseSquare) % ampModPeriod > ampModPeriod*ampModDuty ) {
						signalAmpFactor = 0;
					}
			}
			else if (ampModType.equals("sine")) {
				signalAmpFactor = signalAmpFactor * Math.sin(2.0*Math.PI*i/ampModPeriod + ampPhaseSine);
			}

			// generate noise values
			double dNoiseX = 0;
			double dNoiseY = 0;
			try {
				dNoiseX = noiseGen.next();
				dNoiseY = noiseGen.next();
			}
			catch (Exception e) {
				System.out.println("NoiseGen exception at " + i);
				throw e;
			}

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
			+ "\t  noiseFile\t (string) path to noise file\n"
			+ "\t  deltaPhi\t(double -180 - 180) average phase angle (degrees) between samples\n"
			+ "\t  SNR\t(double) Signal amplitude in terms of sigma_noise\n"
			+ "\t  drift\t(double) Average drift rate of signal\n"
			+ "\t  driftRateDerivate\t(double) Change of drift rate per 1m samples\n"
			+ "\t  sigmaSquiggle\t(double) amplitude of squiggle noise\n"
			+ "\t  outputLength\t(int > 2) number of complex-valued samples to write to output\n"
			+ "\t  ampModType\t(string = 'none','square','brightpixel', or 'sine') specifies how the amplitude is modulated\n"
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

