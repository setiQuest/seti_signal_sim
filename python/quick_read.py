#!/usr/bin/python

import sys, getopt
import read_sim

def main(argv):
   inputfile = ''
   logOpt = False
   skipLines = 2
   noise = None

   try:
      opts, args = getopt.getopt(argv,"hi:l:s:n:",["ifile=","log=","skip=","noise="])
   except getopt.GetoptError:
      print 'quick_read.py -i <inputfile> -l <logOpt> -s <skip> -n <noise>'
      sys.exit(2)
   
   print args

   print opts
   print argv

   for opt, arg in opts:
      if opt == '-h':
         print 'quick_read.py -i <inputfile> -l <logOpt> -s <skip> -n <noise>'
         sys.exit()
      elif opt in ("-i", "--ifile"):
         inputfile = arg
      elif opt in ("-l", "--log"):
         if arg == "True":
            logOpt = True
         else:
            logOpt = False
      elif opt in ("-s", "--skip"):
         skipLines = int(arg)
      elif opt in ("-n", "--noise"):
         noise = arg

   print 'Input file is ', inputfile
   print 'Log option is ', logOpt
   print 'Noise option is ', noise
   print 'Number of skip lines is ', skipLines


   read_sim.read_and_show(inputfile, logOpt, aspect=True, skip_lines = skipLines, noise=noise)

   raw_input("type anything to quit")

if __name__ == "__main__":
   main(sys.argv[1:])
