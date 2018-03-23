# SETI Simulation Signals

This code generates various types of complex-valued time-series signals that are similar to signals observed at the 
Allen Telescope Array, operated by the SETI Instititue. 

This code is in very poor shape! It's certainy research-level code and there are no guarantees. Please contact the
authers or submit Issues if you have problems. This is not consumer-friendly code and we do not have unit tests
to ensure full functionality. We know that it works on our developer's systems. 

You'll at least need to have a recent Java SDK installed. You *should* be able to complile the core Java code with just Java. 
However, it is recommended to also install the Scala Build Tool (SBT), which should make the usage easier. Why did I mix
Scala in with Java? Simply because I wanted to learn a bit about Scala and SBT. It is what it is. 

The output simulation files (named `<uuid>.dat`) are simple: A JSON header, followed by a newline (`\n`), an optional 2nd JSON header followed by a newline, and then some number of bytes
that hold the complex-valued time-series data. Each time-step comes in 2-byte pairs where the first byte is the real value
and the second byte is the imaginary value. These data files can be read with the [`ibmseti` Python package](https://github.com/ibm-watson-data-lab/ibmseti). That python package can also be used to do some basic signal processing and caclulate spectrogram.


### Operation Overview 

There are three "modes" under which this code can be run: spark, serial or local. If you're just starting out to use this code, you should first get this working in "local" mode and move on from there. 

#### Spark mode

In `spark` mode, the code should be running on a Spark cluster. An RDD is created and `.map` functions are used to farm out the 
simulations to the executor nodes in order to parallelize the task. The `<uuid>.dat` simulation files are stored in an OpenStack Swift Object Store. The parameters that control the simulation are stored separately in an IBM DB2 database. Credentials for DB2 should be set in the `resources/simulation.properties` file. Credentials and container names for Object Storage should be set in the same file. See the `example_spark_submit.sh` script.  


#### Serial mode
In `serial` mode, Spark is not used and all simulations are run in one thread. The data are still stored into the external
Object Storage and DB2 systems. 


#### Local mode

In  `local` mode, neither Spark nor the Object Store and DB2 systems are used. All data are stored locally. A 2nd, "private", 
JSON header is included in the output `<uuid>.dat` file. Despite not using Object Storage or DB2, you will still need a `resources` file
because the code tries to open it anyways (another casualty of "research-level" coding and motivation to fix all things). 
Of course, you don't need to set any values to the credentials, just `cp resources/simulation.properties.template resources/simulation.properties`.  The simulations are performed in one local thread and stored to the local file system. 

Also, to note: the `<SNR>` setting (see below) is only available in the `local` mode, as I didn't have time to add it to the `serial` 
and `spark` modes and test it out. 



## Compile

### Using SBT

Find links to install SBT on Mac, Linux and Windows here: http://www.scala-sbt.org/release/docs/Setup.html

This one command will download the dependecies, compile the code and package it into an
uber jar file. 

However, before you do, **you must create the file `resources/simulation.properties`**. There is a `.template` file
in the `resources` folder, which will tell you which fields you need to create. 
If you are running the simulations in `local` mode, you will not need to fill in the 
values. The `simulations.properties` file will be packaged into the
resulting `.jar` file and opened during run time. 

```
sbt clean assembly
```

### OLD way, no SBT

Note, without using SBT, these instructions will not create an uber jar and it's unlikely 
this will run on a spark cluster (the dependecies must be found on all worker nodes). 

However, if you really just want to use Java only, this might work. 

Also, the old way requires that all dependency libraries to be downloaded and installed in the "dependencies"
folder. As of this writing, the java code is only dependent upon the Jackson tools for generating JSON. 


##### Compile

```
source setup.sh   #adds dependencies to CLASSPATH
javac apps/simulate/*.java
```

##### Build Jar (optional)

```
jar cfm setisimulator.jar MANIFEST.MF apps/simulate/*.class
```


## Run

### Using SBT 

If you've used `sbt` to package the code, the resulting jar file is 
`target/scala-x.YY/signalsimulation-assembly-8.0.jar`

The main class for this jar file, however, is now [spark/SETISim.scala](spark/SETISim.scala)

```
java -jar <jar file> <parameters>
```


##### Properties

You must create the file `resources/simulation.properties`. A template with all of the necessary
property values is in the repository. You should `cp resources/simulation.properties.template resources/simulation.properties` and then fill in the values if you are planning to store the output data files in OpenStack Object Storage and IBM DB2 tables. 

##### First, an example

In the example below, the `narrowband` parameter tells the SignalDefFactory to simulate that signal
class. The range of simulation parameters for each class is hard-coded in the [classes here](spark/signaldef). (This is less than ideal coding practice, but worked for our purposed.)

The `training` option tells the program to put the signal class in the public header and specifies 
a particular range of signal amplitudes to use (the `basic` option would use a larger range of amplitudes). 

Two (2) simulations will be peformed. 

The noise will be `gaussian`, defined by the GaussianNoise.java class.  (You'll almost always use this as your 
noise model unless you have a data file that can be read with the FileNoise class, in which case 
you can pass in the name of the file that holds that data.)

```
java -jar  target/scala-2.11/signalsimulation-assembly-8.0.jar training serial 2 narrowband gaussian
```



#### Full Set of Parameters


```
java -jar  target/scala-2.11/signalsimulation-assembly-8.0.jar <data_class> <mode> <number_of_partitions> <number_of_simulations> <signal_class> <noise> <SNR>

```

 * `<data_class>` one of `training`, `test`, `basic`, `basictest`, `private`. You should probably just use `training`, `basic` or `test`. In `test` mode, the output data files do not contain the signal class in the first **public** header (though the class name does exist in the second **private** header when in `local` mode.) Read the code carefully and do some tests to figure out what the other options do. :/  
 * `<mode>` either `local`, `serial` or `spark`, as explained above.
 * `<number_of_partitions>` number of Spark partitions to use IF `mode=spark`, otherwise DO NOT INCLUDE this value in command
 * `<number_of_simulations>` number of signals to simulate
 * `<signal_class>` See [SignalDefFactory.scala](spark/signaldef/SignalDefFactory.scala) for list of available classes.
 * `<noise>` one of `gaussian`, `sunnoise` or the path to a file. If `sunnoise`, will attempt to access Object Storage instance for data file.
 * `<SNR>` If `mode=local`, then one can specify a fixed SNR value to use for all simulations.  This ONLY works in `local` mode. If this is not specified, a range of SNR values will be simulated. 
```


##### Examples

###### Spark Mode

Generate 1,000 test narrowband signals with sun noise on spark with 20 separate partitions.

```
java -jar  target/scala-2.11/signalsimulation-assembly-8.0.jar test spark 20 1000 narrowband sunnnoise
```

The `sunnoise` is a special case. We had noise files that were created by observing the Sun for a number of hours. These
noise files were stored in Object Storage and retrieved at run time. Unless you work at the SETI Instutite, you probably won't 
use this option!


Generate 1,000 training narrowband signals with gaussian white nose on spark with 20 separate partitions.


```
java -jar  target/scala-2.11/signalsimulation-assembly-8.0.jar training spark 20 1000 narrowband gaussian
```

###### Local Mode

Generate 10 "basic" narrowband simulations, all with a fixed SNR of 0.15

```
java -jar  target/scala-2.11/signalsimulation-assembly-8.0.jar basic local 10 narrowband gaussian 0.15
```

Generate 10 "training" narrowband simulations with a range of SNR values.
 
```
java -jar  target/scala-2.11/signalsimulation-assembly-8.0.jar training local 10 narrowband gaussian
```

Generate 10 "training" squiggle simulations with a range of SNR values.
 
```
java -jar  target/scala-2.11/signalsimulation-assembly-8.0.jar training local 10 squiggle gaussian
```



#### Submitting to IBM Spark Cluster

```
./spark-submit.sh --vcap vcap.enterprise.json --deploy-mode cluster --conf spark.service.spark_version=2.0 --class org.seti.simulator.SETISim target/scala-2.11/signalsimulation-assembly-8.0.jar training spark 20 1000 narrowband gaussian
```


### OLD way, no SBT

If you did not package the compiled .class files into a jar file, you can call the 
main class directly. 

```
source setup.sh #if not done already. Only need to to this once.
java apps.simulate.DataSimulator <all individual parameters>

//example
java apps.simulate.DataSimulator 13 "" 100 0.4 -0.0001 -0.0002 0.0001 792576 square 61440 .5 squiggle_pulsed test.data
```

You'll need to read the DataSimulator code class to decipher all of these values. :)

#### Manual jar file

Alternatively 

```
java -jar  setisimulator.jar 13 "" 100 0.3 -0.0001 -0.0002 0.0001 792576 square 61440 .5 squiggle_pulsed test.data
```

##### Description of above simulation

To get 129 raster lines with 6144 frequency bins, which is the size of an archive-compamp file with the
over-sampled frequencies removed (aka, a waterfall plot), the output length of data is a product of these two numbers
129 * 6144 = 792576.

Also, in this example, I've added a square wave amplitude modulation with a periodicity of 61440
samples (equivalent to 10 raster lines) with a duty cycle of 0.5.  One can also add a sine wave
amplitude modulation (in the case of a `sine` modulation, the duty cycle value is ignored.)


## Create Spectrogram 

The output file contains one or two headers, all contained within the first two lines. They are in JSON format. The
first header is called the "public" header, and the second header is the "private" header. In spark or serial mode, 
the information from the private header will be saved to a database and removed from the simulation
file and the public header will remain. 

### With Python
See `python/read_sim.py`.  It contains example functions to read the output data files and product spectrogram.

From iPython shell one can

```
%load python/read_sim.py
```

and then

```
read_and_show()
```


### With SETI Command Line tools



From the command-line, one can skip both headers and stream the remainder of the data with 
the `tail` command. Then pipe the data into the standard SETI command-line tools.

```
len=6144  
tail -n +3 test.data | sqsample -l $len | sqwindow -l $len | sqfft -l $len | sqabs -l $len | sqreal -l $len | sqpnm -c $len -r 32 -p > wf1.pgm
```

SETI command line tools are here: https://github.com/setiQuest/Algorithms


#### View PGM file

XView will display the PGM file by simply

```
xv wf.pgm
```

In python, I do

```
from __future__ import print_function
from PIL import Image, ImageFilter
 
im = Image.open('wf1.pgm')
im.show()
```

### Creating DASDHDB tables

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
