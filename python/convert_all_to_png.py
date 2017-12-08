#!/usr/bin/python
from __future__ import print_function
from __future__ import division

import json
import numpy as np
import matplotlib.pyplot as plt

import sys, getopt
import read_sim
import glob
import os


usagestring = 'convert_all_to_png.py -i <indir> -o <outdir> -l <logOpt> -s <skip> -m <spectrogram height factor>'


def localLog(spectrogram, logOpt):
   if logOpt:
      
      #remove all zero values and replace them with the minimum 
      spectrogram_pos = spectrogram[spectrogram > 0]
      spectrogram[spectrogram <= 0] = spectrogram_pos.min()

      return np.log(spectrogram)
   else:
      return spectrogram

def main(argv):
   
   outdir = '.'
   indir = '.'
   logOpt = True
   skipLines = 2
   m = 12
   #noise = None

   fig, ax = plt.subplots()

   try:
      opts, args = getopt.getopt(argv,"hi:o:l:s:m:",["indir=","outdir=","log=","skip=","m="])
   except getopt.GetoptError:
      print(usagestring)
      sys.exit(2)
   
   print(args)

   print(opts)
   print(argv)

   for opt, arg in opts:
      if opt == '-h':
         print(usagestring)
         sys.exit()
      elif opt in ("-o", "--outdir"):
         outdir = arg
      elif opt in ("-i", "--indir"):
         indir = arg
      elif opt in ("-l", "--log"):
         if arg == "True":
            logOpt = True
         else:
            logOpt = False
      elif opt in ("-s", "--skip"):
         skipLines = int(arg)
      elif opt in ("-m", "--m"):
         m = int(arg)
      # elif opt in ("-n", "--noise"):
      #    noise = arg

   print('Input dir is ', indir)
   print('Output dir is ', outdir)
   print('Log option is ', logOpt)
   #print('Noise option is ', noise
   print('Number of skip lines is ', skipLines)

   allDatFiles = glob.glob(os.path.join(indir, "*.dat"))

   for datfile in allDatFiles:

      ax.cla()
      spectrogram, header_list = read_sim.get_spectrogram(datfile, skip_lines=skipLines, shape=(int(32*m),int(6144/m)))
      ax.imshow(localLog(spectrogram, logOpt), aspect = 0.5*float(spectrogram.shape[1]) / spectrogram.shape[0], cmap='gray')
      
      pngname,_ = os.path.splitext(os.path.basename(datfile))  
      pngname = pngname + '.png'
      fig.savefig(os.path.join(outdir, pngname))


if __name__ == "__main__":
   main(sys.argv[1:])
