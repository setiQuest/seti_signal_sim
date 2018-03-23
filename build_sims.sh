#!/bin/bash

JAR='/Users/adamcox/softwaredev/cloudant/dev-ad/projects/seti/seti_signal_sim/target/scala-2.11/signalsimulation-assembly-8.0.jar'

echo 'starting...'
echo $JAR

#clean up local directory just in case. 
rm *.dat 

#for i in 0.008 0.01 0.02 0.04 0.06 0.08 0.1 0.2 0.4
#for i in 0.05 0.07 0.09 0.12 0.16
for i in 0.4
#for i in 0.008 0.01 0.02 0.04 0.06 0.08 0.1 0.2 0.4 0.05 0.07 0.09 0.12 0.16
do

  #for j in 'narrowband' 'narrowbanddrd' 'noise' 'squiggle' 'brightpixel' 'squarepulsednarrowband' 'squigglesquarepulsednarrowband'
  for j in 'narrowband' #'noise'
  #for j in 'noise' 
  do  
    echo $i $j $1
    java -jar $JAR training local $1 $j gaussian $i   
  done
  
  dest="snr_${i}/"
  mkdir $dest
  mv *.dat $dest
  #ls $dest
done

#zip and clean up
#zip -r sims.zip snr*
#rm -rf snr*

#java -jar  target/scala-2.11/signalsimulation-assembly-8.0.jar training local 1 narrowband gaussian 0.008