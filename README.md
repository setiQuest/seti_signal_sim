# SETI Simulation Signals

This code generates various classes of complex-valued time-series signals that are similar to signals observed at the 
Allen Telescope Array, operated by the SETI Institute. You can see what some of these signal classes look like by [reading this blog post](https://medium.com/ibm-watson-data-lab/using-artificial-intelligence-to-search-for-extraterrestrial-intelligence-ec19169e01af). We have also [published a paper](http://xxx.lanl.gov/abs/1803.08624) that contains information on the analytical model of these signals, along with work done to use convolutional neural networks to identify the different signal classes from their spectrogram representations. 

The output simulation files (named `<uuid>.dat`) are simple: A JSON header, followed by a newline (`\n`), an optional 2nd JSON header followed by a newline, and then some number of bytes
that hold the complex-valued time-series data. Each time-step comes in 2-byte pairs where the first byte is the real value
and the second byte is the imaginary value. These data files can be read with the [`ibmseti` Python package](https://github.com/ibm-watson-data-lab/ibmseti). That python package can also be used to do some basic signal processing and calculate spectrogram.

This code is in relatively poor shape an was built in a somewhat ad-hoc manner. You could call it "research-level code" (i.e. not consumer-friendly and no unit tests) and there are no guarantees. We really only know that it works on our local systems and an external Apache Spark (2.1.0) cluster.  
*Please do not hesitate to contact the authors, submit Issues, or PRs if you have problems!* 

This code was developed on a Mac with JDK SE version 8 and and Scala Build Tool (SBT) version 0.13.

## Operation Overview 

Once compiled, the resulting executable `.jar` file will produce data files that contain a simulated SETI signal. This section first describes the different environments where you may run this executable. Then, brief instructions for compiling the code are described, followed by a mostly complete documentation of the different signal classes and parameters that can be passed to the executable to control the output simulations. 

This code was built to execute on local development machines or Apache Spark clusters, and store the data either locally or in the cloud. There are three "modes" under which this code can be run: `spark`, `serial` or `local`. If you're just starting to use this code, you should first get this working in `local` mode and move on from there. 

### Spark mode

In `spark` mode, the code should be executed on a Spark cluster. An RDD is created and `.map` functions are used to farm out the 
simulations to the executor nodes in order to parallelize the task. The `<uuid>.dat` simulation files are stored in an OpenStack Swift Object Store and meta-data for the simulation are stored separately in an IBM DB2 database. Credentials for DB2 should be set in the `resources/simulation.properties` file. Credentials and container names for Object Storage should be set in the same file. See the `example_spark_submit.sh` script.  


### Serial mode
In `serial` mode, Spark is not used and all simulations are run in one thread. The data are still stored into the external
Object Storage and DB2 systems. 


### Local mode

In  `local` mode, neither Spark nor the Object Store and DB2 systems are used. All data are stored locally. A second, "private", 
JSON header is included in the output `<uuid>.dat` file. Despite not using Object Storage or DB2, you will still need a `resources` file
because the code tries to open it anyways (another casualty of "research-level" coding and motivation to fix all things). 
Of course, you don't need to set any values to the credentials, just `cp resources/simulation.properties.template resources/simulation.properties`.  The simulations are performed in one local thread and stored to the local file system. 

Also, to note: the `<SNR>` setting (see below) is only available in the `local` mode, as I didn't have time to add it to the `serial` 
and `spark` modes and test it out. 



## Compile

To compile the entire code base, you'll need to install the Scala Build Tool. Instructions to install SBT: http://www.scala-sbt.org/release/docs/Setup.html


Before compilation, **you must first create the file `resources/simulation.properties`**. There is a `.template` file
in the `resources` folder.

If you are running the simulations in `local` mode, you will not need to fill in the 
values and the simplest option is to just copy

```
cp resources/simulation.properties.template resources/simulation.properties
```

Otherwise, fill in the values in the `simulation.properties` file if you are planning to store the output data files in OpenStack Object Storage 
and IBM DB2 tables. [You'll need to build a table in DB2. The structure of the IBM DB2 table is described below](#ibm-db2-table-structure). The `simulations.properties` file will be packaged into the
resulting `.jar` file and opened during run time. 

Once you've created the `resources/simulation.properies` file, the following command should perform the entire build.

```
sbt clean assembly
```


The `build.sbt` file specifies the Scala and Spark versions for which the code is 
compiled, which are 2.11.8 and 2.1.0, respectively. Of course, you should change these values 
as needed. If you do not have access to a Spark cluster and only plan to run these 
in `local` mode, then you don't need to modify the Spark version. Note that the `build.sbt` file
will not download the Apache Hadoop or Spark libraries (by using the 
[`provided` option](build.sbt#L42-L44)). It assumes the path to those `.jar`s will be
provided by the system. Again, if you're running in `local` mode, then you don't need
to worry about this. 

## Run

If you've used `sbt` to package the code, the resulting jar file is 
`target/scala-2.11/signalsimulation-assembly-8.0.jar`.

The main class for this jar file is [spark/SETISim.scala](spark/SETISim.scala)

```
java -jar <jar file> <parameters>
```

#### A simple example

In the example below, a `narrowband` signal class is simulated. The range of simulation parameters for each class is hard-coded in the [classes here](spark/signaldef). (This is less than ideal coding practice, but worked for our purposes.)

The `training` option tells the program to report the signal class in the public header and specifies 
a particular range of signal amplitudes to that may be simulated (the `basic` option would use a larger range of amplitudes). 

Two (2) simulations will be peformed. 

The noise will be `gaussian`, defined by the GaussianNoise.java class.  (You'll almost always use this as your 
noise model unless you have a data file that can be read with the FileNoise class, in which case 
you can pass in the name of the file that holds that data.)

```
java -jar  target/scala-2.11/signalsimulation-assembly-8.0.jar training serial 2 narrowband gaussian
```


### Full Set of Parameters

The set of parameters that you can use are briefly described below.   


```
java -jar  target/scala-2.11/signalsimulation-assembly-8.0.jar <data_class> <mode> <number_of_partitions> <number_of_simulations> <signal_class> <noise> <SNR>

```

 * `<data_class>` one of `training`, `test`, `basic`, `basictest`, `private`. You should probably just use `training`, `basic` or `test`. In `test` mode, the output data files do not contain the signal class in the first **public** header (though the class name does exist in the second **private** header when in `local` mode.)  In `basic` and `basictest`, the range of signal amplitudes of some signal classes are significantly larger, making them easier to classify. Confusingly, the `private` mode is similar to `training` except that it saves output  data files into a different Object Store container, as specified in the properties file. It has no effect when in `local` mode. :/
 * `<mode>` either `local`, `serial` or `spark`, as explained above.
 * `<number_of_partitions>` number of Spark partitions to use IF `mode=spark`, otherwise DO NOT INCLUDE this value in command
 * `<number_of_simulations>` number of signals to simulate
 * `<signal_class>` See [SignalDefFactory.scala](spark/signaldef/SignalDefFactory.scala) for list of available classes.
 * `<noise>` one of `gaussian`, `sunnoise` or the path to a file. If `sunnoise`, will attempt to access Object Storage instance for data file.
 * `<SNR>` If `mode=local`, then one can specify a fixed SNR value to use for all simulations.  This ONLY works in `local` mode. If this is not specified, a range of SNR values will be simulated. 

Note that SNR is defined as the amplitude of the signal relative to the standard deviation of the noise amplitude. For gaussian white noise, that amplitude is fixed at a value of 13.0 for both the real and imaginary components. The signal amplitude is the amplitude of the sine wave that is added to the white noise at each time sample. You should use SNRs in the range from 0.05 to 0.75, depending on the signal class. 

#### Available Signal Classes.

The different signal classes that [have been defined]((spark/signaldef/SignalDefFactory.scala) so far are:

* `narrowband`
* `squarepulsednarrowband`
* `sinepulsednarrowband`
* `squigglesquarepulsednarrowband`
* `squigglesinepulsednarrowband`
* `squiggle` 
* `narrowbanddrd`
* `squigglesquarepulsednarrowbanddrd`
* `squigglesinepulsednarrowbanddrd`
* `brightpixel`
* `noise`




#### Examples

##### Spark Mode

The following examples assume the code is running on a system with Apache Spark 2.0 or greater installed. 


Generate 1,000 test narrowband signals with sun noise, and run on Spark with 20 separate partitions.

```
java -jar  target/scala-2.11/signalsimulation-assembly-8.0.jar test spark 20 1000 narrowband sunnnoise
```

The `sunnoise` is a special case. We created noise files that were created by observing the Sun for a number of hours. These
noise files were stored in Object Storage and retrieved at run time (object storage container set in the properties file). 
Unless you work at the SETI Institute, you probably won't use this option!


Generate 1,000 training narrowband signals with gaussian white noise, and run on Spark with 20 separate partitions.


```
java -jar  target/scala-2.11/signalsimulation-assembly-8.0.jar training spark 20 1000 narrowband gaussian
```

##### Local Mode

Generate 10 "basic" narrowband simulations, all with a fixed signal amplitude of 0.15

```
java -jar  target/scala-2.11/signalsimulation-assembly-8.0.jar basic local 10 narrowband gaussian 0.15
```

Generate 10 "training" narrowband simulations with a fixed signal amplitude of 0.2.
 
```
java -jar  target/scala-2.11/signalsimulation-assembly-8.0.jar training local 10 narrowband gaussian 0.2
```

Generate 10 "training" narrowband simulations with a range of signal amplitudes.
 
```
java -jar  target/scala-2.11/signalsimulation-assembly-8.0.jar training local 10 narrowband gaussian
```

Generate 10 "training" squiggle simulations with a range of signal amplitudes.
 
```
java -jar  target/scala-2.11/signalsimulation-assembly-8.0.jar training local 10 squiggle gaussian
```



##### Submitting to IBM Spark Cluster

We used an IBM Spark Enterprise service (30 executor cluster) to perform our simulations. We leave this
example command here for documentation. Note that the `spark-submit.sh` script here is 
[the shell script from IBM to run code on the IBM Spark service](https://console.bluemix.net/docs/services/AnalyticsforApacheSpark/using_spark-submit.html#running-a-spark-application-using-the-spark-submit-sh-script) 
and *not* the `spark-submit` script included in the Apache Spark distribution.

```
./spark-submit.sh --vcap vcap.enterprise.json --deploy-mode cluster --conf spark.service.spark_version=2.0 --class org.seti.simulator.SETISim target/scala-2.11/signalsimulation-assembly-8.0.jar training spark 20 1000 narrowband gaussian
```



## Create Spectrogram 

The output file contains one or two JSON headers separated by a newline (`\n`). The
first header is called the "public" header, and the second header is the "private" header. In spark or serial mode, 
the information from the private header will be saved to a database and removed from the simulation
file and the public header will remain. In `test` mode, the signal class name will be removed from the public header. 

### With `ibmseti`

This is the easiest and recommended way to read and analyze these data.

The [`ibmseti` Python package](https://github.com/ibm-watson-data-lab/ibmseti) can read these simulation data files and calculate spectrogram. 

### Batch Convert to Spectrogram

Before the `ibmseti` package supported these file types, some python scripts were added to this repository that 
utilize numpy, scipy and matplotlib to generate spectrogram. These are found in the `python` directory. The `convert_all_to_png.py`
script may be helpful to quickly convert all of your `.dat` files to spectrogram

At minimum, you just need to specify in the directory where the `.dat` files are located and an output directory for the `.png` files. 
```
> ./convert_all_to_png -i <input dir> -o <output dir>
```

### With SETI Command Line tools


From the command-line, one can skip both headers and stream the remainder of the data with 
the `tail` command. Then pipe the data into the standard [SETI command-line tools](https://github.com/setiQuest/Algorithms).

If the data files were created in `local` mode, then be sure to `tail -n +3` to skip both headers. If there's is 
only one JSON header in the data, then `tail -n +2` to skip just one header.

```
len=6144  
tail -n +3 test.data | sqsample -l $len | sqwindow -l $len | sqfft -l $len | sqabs -l $len | sqreal -l $len | sqpnm -c $len -r 32 -p > wf1.pgm
```

#### View PGM file

XView will display the PGM file by simply

```
xv wf.pgm
```

In python,

```
from __future__ import print_function
from PIL import Image, ImageFilter
 
im = Image.open('wf1.pgm')
im.show()
```

## Modifying The Signals

You may come to a point where you'd like to modify the output signals. Unfortunately, the signal definitions are somewhat
hard-coded in the `spark/signaldef` folder. Ideally, signal definitions would have been written as simple documents and 
loaded at run time. But assuming that you don't want to do the work to modify the code that significantly, here are the
few things you're most likely to be interested in:

  * Signal Length: You can shorten (or lengthen) the signal length for all signals by modifying [this line in the SignalDef base class](spark/signaldef/SignalDef.scala#L15).
  * Signal Amplitude Range: Each signal class has its own specified range of signal amplitude (SNR). So, you'll have to change each signal class separately. For example, for `narrowband` signals, you can change the [range of signal amplitudes here](spark/signaldef/NarrowBand.scala#L18). 
  * Similar to the signal amplitude, for each class you can modify the various properties, such as the `drift`, `deltaPhiRad` (initial starting frequency), `ampModDuty` (duty factor for amplitude modulation), `ampModPeriod` (periodicity of amplitude modulation), `ampModType` (amplitude modulation type: either square or sine), and `sigmaSquiggle` (amplitude of amount of "squiggle" in the signal). 



## Etc.

The documentation below includes instructions to compile and run just the `.java` code.  
Also, if running in `spark` mode, the necessary structure for the IBM DB2 table is below.


### Just Java

These instructions are here in order to support the original authors of the core `.java` code. This will compile
the just the `.java` classes and let you run them locally.

The `setup.sh` script adds that folder to the CLASSPATH envar, allowing for external `.jar` dependencies to be 
included in that directory, manually.
As of this writing, the java code is only dependent upon the Jackson tools for generating JSON. 


#### Compile

```
source setup.sh   #adds dependencies to CLASSPATH
javac apps/simulate/*.java
```

##### Build Jar (optional)

```
jar cfm setisimulator.jar MANIFEST.MF apps/simulate/*.class
```


#### Run


```
source setup.sh  
java apps.simulate.DataSimulator <all individual parameters>
```

For example,

```
java apps.simulate.DataSimulator 13 "" 100 0.4 -0.0001 -0.0002 0.0001 792576 square 61440 .5 squiggle_pulsed test.data
```

##### From Jar

Alternatively 

```
java -jar  setisimulator.jar 13 "" 100 0.3 -0.0001 -0.0002 0.0001 792576 square 61440 .5 squiggle_pulsed test.data
```

###### Description of above simulation

To get 129 raster lines with 6144 frequency bins, which is the size of an archive-compamp file with the
over-sampled frequencies removed (aka, a waterfall plot), the output length of data is a product of these two numbers
129 * 6144 = 792576.

Also, in this example, I've added a square wave amplitude modulation with a periodicity of 61440
samples (equivalent to 10 raster lines) with a duty cycle of 0.5.  One can also add a sine wave
amplitude modulation (in the case of a `sine` modulation, the duty cycle value is ignored.)

Most likely you'll need to read the DataSimulator code class to decipher all of these values. :)


### IBM DB2 table strucuture 

When running in either `spark` or `serial` mode, the code expects the existence of an IBM DB2 database table with the following 
structure. 

```
create table setiusers.simsignal (
uuid VARCHAR(128) not null,
sigma_noise DECIMAL(31,10),
noise_name  VARCHAR(128),
delta_phi DECIMAL(31,10),
signal_to_noise_ratio DECIMAL(31,10),
drift DECIMAL(31,10),
drift_rate_derivative DECIMAL(31,10),
jitter DECIMAL(31,10),
len BIGINT,
amp_modulation_type VARCHAR(128),
amp_modulation_period DECIMAL(31,10),
amp_modulation_duty DECIMAL(31,10),
amp_phase DECIMAL(31,10),
amp_phase_square DECIMAL(31,10),
amp_phase_sine DECIMAL(31,10),
signal_classification VARCHAR(128),
seed BIGINT,
drift_divisor DECIMAL(31,10),
initial_sine_drift DECIMAL(31,10),
initial_cosine_drift DECIMAL(31,10),
simulator_software_version INT, 
simulator_software_version_date VARCHAR(128),
date_created TIMESTAMP(10),
container VARCHAR(128),
objectname VARCHAR(128),
etag VARCHAR(256),
noise_file_uuid VARCHAR(128)
);
```

## License

All documentation and software in this repository is licensed under the [Apache License, Version 2.0](LICENSE).
