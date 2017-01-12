# seti_siginal_sim

Yes, "siginal" is a typo. :)

## Compile

### Using SBT

Find links to install SBT on Mac, Linux and Windows here: http://www.scala-sbt.org/release/docs/Setup.html

This one command will download the dependecies, compile the code and package it into an
uber jar file. 

```
sbt assembly
```

### OLD way, no SBT

Note, without using SBT, these instructions will not create an uber jar and it's unclear 
if this will run on a spark cluster (the dependecies must be found on all worker nodes). 
However, for local development this should work. 

Also, the old way requires that all dependency libraries to be downloaded and installed in the "dependcies"
folder. As of this writing, we are only dependent upon the Jackson tools for generating JSON. 


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
`target/signalsimulation-assembly-1.0-SNAPSHOT.jar`

This jar file is exeucted like any other jar file

```
java -jar <jar file> <parameters>
```


```
java -jar target/signalsimulation-assembly-1.0-SNAPSHOT.jar 13 "" 100 0.4 -0.0001 -0.0002 0.0001 792576 square 61440 .5 squiggle_pulsed test.data
```


### OLD way, no SBT

If you did not package the compiled .class files into a jar file, you can call the 
main class directly. 

```
source setup.sh #if not done already. Only need to to this once.
java apps.simulate.DataSimulator <parameters>
```


#### Unpackaged .class file

```
java apps.simulate.DataSimulator 13 "" 100 0.3 -0.0001 -0.0002 0.0001 792576 square 61440 .5 squiggle_pulsed test.data
```

#### Manual jar file

Alternatively 

```
java -jar  setisimulator.jar 13 "" 100 0.3 -0.0001 -0.0002 0.0001 792576 square 61440 .5 squiggle_pulsed test.data
```

### Description of above simulation

To get 129 raster lines with 6144 frequency bins, which is the size of an archive-compamp file with the
over-sampled frequencies removed (aka, a waterfall plot), the output length of data is a product of these two numbers
129 * 6144 = 792576.

Also, in this example, I've added a square wave amplitude modulation with a periodicity of 61440
samples (equivalent to 10 raster lines) with a duty cycle of 0.5.  One can also add a sine wave
amplitude modulation (in the case of a `sine` modulation, the duty cycle value is ignored.)


## Create Spectrogram 

The output file contains two headers, all contained within the first two lines. They are in JSON format. The
first header is called the "private" header, and the second header is the "public" header. When these data
are published, the information from the private header will be saved to a database and removed from the simulation
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
tail -n +3 test.data | sqsample -l $len | sqwindow -l $len | sqfft -l $len | sqabs -l $len | sqreal -l $len | sqpnm -c $len -r 129 -p > wf1.pgm
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
