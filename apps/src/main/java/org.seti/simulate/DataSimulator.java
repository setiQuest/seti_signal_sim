package org.seti.simulate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * DataSimulator produces complex-valued 8-bit sample data like from ATA beamformer.
 **/
public class DataSimulator {

    public static int mExpectedArgs = 13;
    public double mDriftDivisor = 1024;

    public int simulationVersion = 14;
    public String simulationVersionDate = "8 Dec 2017";

    public Random rand;
    public NoiseGenerator noiseGen;
    public double sigN;
    public double dPhi;
    public double dPhiRad;
    public double SNR;
    public double drift;
    public double driftRateDerivate;
    public double jitter;
    public int numberOfDataSamples;
    public String ampModType;
    public double ampModPeriod;
    public double ampModDuty;
    public OutputStream OS = null;
    public String signalClass;
    public long seed;
    public Map<String, Object> privateHeader = null;
    public Map<String, Object> labeledPublicHeader = null;
    public Map<String, Object> unlabeledPublicHeader = null;

    public double sinDrift = 0;
    public double cosDrift = 0;
    public String uuid;

    public double cosPhi = 0;
    public double sinPhi = 0;
    public double signalX = 0;
    public double signalY = 0;
    public double ampPhase = 0;
    public double maxBPPhase = 0.98;
    public double ampPhaseSquare = 0;
    public double ampPhaseSine = 0;
    public double signalAmpFactor = 0;
    public double signalEnergy = 0;
    public double noiseEnergy = 0;

    public int numBeyondDynamicRangeX = 0;
    public int numBeyondDynamicRangeY = 0;

    public DataSimulator(NoiseGenerator anoiseGen, double asigN, double adPhi, double aSNR,
                         double adrift, double adriftRateDerivate, double ajitter, int alen,
                         String aampModType, double aampModPeriod, double aampModDuty, String asignalClass, long aseed, Random arand, String auuid) throws Exception {
        noiseGen = anoiseGen;
        sigN = asigN;
        dPhi = adPhi;
        dPhiRad = dPhi / 180.0 * Math.PI;
        SNR = aSNR;
        drift = adrift;
        driftRateDerivate = adriftRateDerivate;
        jitter = ajitter;
        numberOfDataSamples = alen;
        ampModType = aampModType;
        ampModPeriod = aampModPeriod;
        ampModDuty = aampModDuty;
        signalClass = asignalClass;
        seed = aseed;
        uuid = auuid;
        rand = arand;

        reset();
    }

    public static void main(String[] args) throws Exception {

        // check command line arguments
        if (args.length != mExpectedArgs) PrintHelp();

        int nextarg = 0;
        double sigmaN = Double.parseDouble(args[nextarg++]);
        String noiseFile = args[nextarg++];
        double deltaPhiDeg = Double.parseDouble(args[nextarg++]);
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

        String auuid = UUID.randomUUID().toString();
        if (filename.equals("")) {
            filename = auuid + ".dat";
        }
        // test argument values -- tbd

        System.out.println("args:\n"
                + "sigmaN = " + sigmaN + "\n"
                + "noiseFile = " + noiseFile + "\n"
                + "deltaPhiDeg = " + deltaPhiDeg + "\n"
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
        Random randGen = new Random(seed);

        // create output file
        FileOutputStream FOS = new FileOutputStream(new File(filename));
        NoiseGenerator noiseGen;

        if (noiseFile.equals("")) {
            noiseGen = new GaussianNoise(randGen);
            noiseGen.setAmp(sigmaN);
        } else {
            noiseGen = new FileNoise(noiseFile);
            noiseGen.setAmp(1.0);
        }


        // create simulator
        DataSimulator mySimulator = new DataSimulator(
                noiseGen, sigmaN, deltaPhiDeg, SNR, drift, driftRateDerivate, sigmaSquiggle, outputLength, ampModType, ampModPeriod, ampModDuty, signalClass, seed, randGen, auuid);

        //we insert the private and public headers into the output data file.
        ObjectMapper mapper = new ObjectMapper();

        ByteArrayOutputStream dataOS = new ByteArrayOutputStream(2 * outputLength);

        //now do the simulation, inserting the data into the FOS
        mySimulator.run(dataOS);

        mySimulator.updatePrivateHeader();

        String json = mapper.writeValueAsString(mySimulator.privateHeader);
        System.out.println(json);
        FOS.write(mapper.writeValueAsBytes(mySimulator.privateHeader));
        FOS.write('\n');

        json = mapper.writeValueAsString(mySimulator.labeledPublicHeader);
        System.out.println(json);
        FOS.write(mapper.writeValueAsBytes(mySimulator.labeledPublicHeader));
        FOS.write('\n');

        dataOS.writeTo(FOS);

        // close output file
        FOS.close();

        dataOS.close();

        //close noise generator
        noiseGen.close();
    }

    public static void PrintHelp() {

        System.out.println("\n\t" + mExpectedArgs + " arguments expected\n\n"
                + "\tsigmaNoise deltaPhi SNR  drift sigmaSquiggle outputLength ampModType ampModPeriod ampModDuty signalClass filename\n\n"
                + "\twhere\n\n"
                + "\t  sigmaNoise\t (double 0 - 127) noise mean power, 13 is good choice\n"
                + "\t  noiseFile\t (string) path to noise file\n"
                + "\t  deltaPhiDeg\t(double -180 - 180) average phase angle (degrees) between samples\n"
                + "\t  SNR\t(double) Signal amplitude in terms of sigma_noise\n"
                + "\t  drift\t(double) Average drift rate of signal\n"
                + "\t  driftRateDerivate\t(double) Change of drift rate per 1m samples\n"
                + "\t  sigmaSquiggle\t(double) amplitude of squiggle noise\n"
                + "\t  outputLength\t(int > 2) number of complex-valued samples to write to output\n"
                + "\t  ampModType\t(string = 'none','square','brightpixel', or 'sine') specifies how the amplitude is modulated\n"
                + "\t  ampModPeriod\t(int > 2) periodicity of amplitude modulation, in same units of outputLength\n"
                + "\t  ampModDuty\t(double betweeen 0 and 1) duty cycle of square wave amplitude modulation.\n"
                + "\t  signalClass\t(string) a name to classify the signal.\n"
                + "\t  filename\t(string) output filename for data. If \"\", then a unique ID will be used for the file name\n");

        System.exit(0);
    }

    public void updatePrivateHeader() {
        privateHeader.put("total_signal_energy", signalEnergy);
        privateHeader.put("total_noise_energy", noiseEnergy);
    }

    public void reset() {
        //resets all the initial settings so that you can run another simulation
        //the random number generator is NOT reset, so that subsequent simulations
        //will not produce the exact same output.


        //	DO ALL THE INITIALIZATION HERE SO THAT WE CAN EXTRACT THE HEADERS

        // put drift rate and signal jitter into (approximate) Hz/s assuming mDriftDivisor samples in FFT
        // using small angle approximation for sinDrift
        sinDrift = drift / mDriftDivisor;
        if (Math.abs(sinDrift) > 1) sinDrift = Math.signum(sinDrift);
        cosDrift = Math.sqrt(1 - sinDrift * sinDrift);

        // dPhiRad is average phase angle (radians) between complex-valued samples
        // pi radians -> Nyquist frequency
        cosPhi = Math.cos(dPhiRad);
        sinPhi = Math.sin(dPhiRad);

        // keeps track of signal and sample values from most recent data point
        signalX = rand.nextGaussian();
        signalY = rand.nextGaussian();


        //don't let the amplitude modulation start near the edges of the simulation
        ampPhase = 0.07 + (0.93 - 0.07) * rand.nextFloat();

        //we do this to ensure that the bright pixel doesn't happen
        //*right* at the beginning or end of the simulation.
        //This is
        //especially important for it not to start at the end of
        //this simulation, because it would actually wrap around
        //and the signal would be split between the very beginning and
        //very end.
        //typical "brightpixel" signals will last between
        //0.1 second up to 2 seconds.** This is now specifically tailored
        //for simulations of 32 raster lines * 6144 samples/raster line.
        //We prevent the ON phase of
        //amplitude modulation from "starting" within the last
        //2% of the simulated waveform
        //
        //** note: "second" here means the time length of a raster line
        //in ACA files that we're simulating. This does not exactly match
        //the ACA files, but it makes it easier to visualize and discuss.
        // maxBPPhase = 0.93;
        // if (ampModType.equals("brightpixel")){
        // 		ampPhase = ampPhase*maxBPPhase;
        // }

        ampPhaseSquare = ampPhase * ampModPeriod;
        ampPhaseSine = (ampPhase - 0.5) * Math.PI; //convert to radians

        if (sigN != 0) {
            signalAmpFactor = SNR * sigN;
        } else {
            signalAmpFactor = SNR;
            System.out.println("sigN is zero. signal amplitude is : " + signalAmpFactor);
        }


        //now use the initial conditions to build the privateHeader

        privateHeader = new HashMap<>();
        privateHeader.put("sigma_noise", sigN);
        privateHeader.put("noise_name", noiseGen.getName());
        privateHeader.put("delta_phi", dPhi);
        privateHeader.put("signal_to_noise_ratio", SNR);
        privateHeader.put("drift", drift);
        privateHeader.put("drift_rate_derivative", driftRateDerivate);
        privateHeader.put("jitter", jitter);
        privateHeader.put("len", numberOfDataSamples);
        privateHeader.put("amp_modulation_type", ampModType);
        privateHeader.put("amp_modulation_period", ampModPeriod);
        privateHeader.put("amp_modulation_duty", ampModDuty);
        privateHeader.put("amp_phase", ampPhase);
        privateHeader.put("amp_phase_square", ampPhaseSquare);
        privateHeader.put("amp_phase_sine", ampPhaseSine);
        privateHeader.put("signal_classification", signalClass);
        privateHeader.put("seed", seed);
        privateHeader.put("drift_divisor", mDriftDivisor);
        privateHeader.put("initial_sine_drift", sinDrift);
        privateHeader.put("initial_cosine_drift", cosDrift);
        privateHeader.put("simulator_software_version", simulationVersion);
        privateHeader.put("simulator_software_version_date", simulationVersionDate);
        privateHeader.put("uuid", uuid);


        labeledPublicHeader = new HashMap<>();
        labeledPublicHeader.put("signal_classification", signalClass);
        labeledPublicHeader.put("uuid", uuid);

        unlabeledPublicHeader = new HashMap<>();
        unlabeledPublicHeader.put("uuid", uuid);

        signalEnergy = 0;
        noiseEnergy = 0;

    }

    public void run(OutputStream anOS) throws Exception {


        //double prevCosPhi = cosPhi;
        //double prevSinPhi = sinPhi;
        boolean prevSinSign = sinPhi > 0;
        boolean ampOn = true;


        // loop over samples
        for (int i = 0; i < numberOfDataSamples; ++i) {

            if (signalClass.equals("noise")) {
                ampOn = false;
            }

            // this creates sidebands in the spectrogram and need to
            // make sure to have a large enough periodicity so that sidebands are not observed
            //
            if (sigN != 0) {
                signalAmpFactor = SNR * sigN;
            } else {
                signalAmpFactor = SNR;
            }

            switch (ampModType) {
                case "square":
                    if ((i + ampModPeriod - ampPhaseSquare) % ampModPeriod > ampModPeriod * ampModDuty) {
                        signalAmpFactor = 0;
                    }
                    break;
                case "brightpixel":   //note: the code to set signalAmpFactor=0 for 'square' should work for 'brightpixels', but there was a historical bug that created these seperate formula and I don't want to break anything so just going to leave it separate!
                    if (i < ampPhaseSquare || i > ampPhaseSquare + ampModPeriod * ampModDuty) {
                        signalAmpFactor = 0;
                    }
                    break;
                case "sine":
                    signalAmpFactor = signalAmpFactor * Math.sin(2.0 * Math.PI * i / ampModPeriod + ampPhaseSine);
                    break;
            }

            // generate noise values
            double dNoiseX;
            double dNoiseY;
            try {
                dNoiseX = noiseGen.next();
                dNoiseY = noiseGen.next();
            } catch (Exception e) {
                System.out.println("NoiseGen exception at " + i);
                throw e;
            }

            double Xval = dNoiseX;
            double Yval = dNoiseY;

            noiseEnergy += dNoiseX * dNoiseX + dNoiseY * dNoiseY;

            //double Xvald = Xval;
            //double Yvald = Yval;

            if (ampOn) {
                signalEnergy += (signalX * signalAmpFactor) * (signalX * signalAmpFactor) + (signalY * signalAmpFactor) * (signalY * signalAmpFactor);
                Xval += signalX * signalAmpFactor;
                Yval += signalY * signalAmpFactor;
            }

            // if the value hits the rail, then truncate it
            if (Math.abs(Xval) > 127) {
                numBeyondDynamicRangeX += 1;
                Xval = Math.signum(Xval) * 127;
            }
            if (Math.abs(Yval) > 127) {
                numBeyondDynamicRangeY += 1;
                Yval = Math.signum(Yval) * 127;
            }

            byte X = (byte) Xval;
            byte Y = (byte) Yval;
            byte[] sample = new byte[]{X, Y};

            // write sample to OutputStream
            anOS.write(sample);


            //calculate next amplitude


            //allow for drift rate to change
            if (driftRateDerivate != 0) {
                sinDrift = sinDrift + (driftRateDerivate / 1000000.0) / mDriftDivisor;
                if (Math.abs(sinDrift) > 1) sinDrift = Math.signum(sinDrift);
                cosDrift = Math.sqrt(1 - sinDrift * sinDrift);
            }

            // propagate signal frequency to next value using drift
            double temp = cosPhi * cosDrift - sinPhi * sinDrift;
            sinPhi = cosPhi * sinDrift + sinPhi * cosDrift;
            cosPhi = temp;

            // propagate signal frequency to next value by adding jitter
            // frequency does random walk
            double sinDelta = 2.0 * (rand.nextDouble() - 0.5) * jitter;
            double cosDelta = Math.sqrt(1 - sinDelta * sinDelta);
            temp = cosPhi * cosDelta - sinPhi * sinDelta;
            sinPhi = cosPhi * sinDelta + sinPhi * cosDelta;
            cosPhi = temp;

            // normalization (potential optimization here)
            double mag = Math.sqrt(cosPhi * cosPhi + sinPhi * sinPhi);
            cosPhi /= mag;
            sinPhi /= mag;

            // propagate current signal value to next using updated frequency
            double tmpX = signalX * cosPhi - signalY * sinPhi;
            signalY = signalX * sinPhi + signalY * cosPhi;
            signalX = tmpX;
            mag = Math.sqrt(signalX * signalX + signalY * signalY);
            signalX /= mag;
            signalY /= mag;

            // fake anti-aliasing filter...
            if (cosPhi < -0.99) {
                if (prevSinSign != sinPhi > 0) {
                    //we've crossed the boundary, need to flip signal switch
                    ampOn = !ampOn;
                    System.out.format("i: %d, ACA spectrogram line: %d, cosPhi: %.8f, sinPhi: %.8f.%n", i, i / 6144, cosPhi, sinPhi);
                }
            }

            prevSinSign = sinPhi > 0;
            //prevCosPhi = cosPhi;
            //prevSinPhi = sinPhi;

            // if (i < 10) {
            // 	if (i == 0) {
            // 		System.out.println("Printing out the first 10 samples");
            // 	}
            // 	System.out.println(Xvald + " , " + Yvald + " ; " +  Xval + " , " + Yval  + " ; " +  X + " , " + Y + "; 0x:" + bytesToHex(sample));
            // 	//System.out.println(bytesToHex(sample));
            // }


        }

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

