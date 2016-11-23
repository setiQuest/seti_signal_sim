# seti_siginal_sim

## Compile

```
source setup.sh   #adds dependencies to CLASSPATH
javac apps/simulate/DataSimulator
```

## Simulate

```
source setup.sh #if not done in shell already
java apps.simulate.DataSimulator 13 100 0.3 -0.0002 0.0001 792576 square 61440 .5 squiggle_pulsed test.data
```

If you just call `java apps.simulate.DataSimulator`, the output should describe all of the
command-line input parameters.

To get 129 raster lines with 6144 frequency bins, which is the size of an archive-compamp file with the
over-sampled frequencies removed (aka, a waterfall plot), the output length of data is a product of these two numbers
129 * 6144 = 792576.

Also, in this example, I've added a square wave amplitude modulation with a periodicity of 61440
samples (equivalent to 10 raster lines) with a duty cycle of 0.5.  One can also add a sine wave
amplitude modulation (in the case of a `sine` modulation, the duty cycle value is ignored.)

## Convert to PGM file

The output file contains a header, all contained within the first line. It is in JSON format. 
From the command-line, one can skip the header and stream the remainder of the data with 
the `tail` command. Then pipe the data into the standard SETI command-line tools.

```
len=6144  
tail -n +2 test.data | sqsample -l $len | sqwindow -l $len | sqfft -l $len | sqabs -l $len | sqreal -l $len | sqpnm -c $len -r 129 -p > wf1.pgm
```

See this repo for the SETI command line tools: https://github.com/setiQuest/Algorithms

## View PGM file

XView will display the PGM file by simply

```
xv wf.pgm
```

In python, I do

```
from __future__ import print_function
from PIL import Image, ImageFilter
 
im = Image.open(‘wf1.pgm’)
im.show()
```
