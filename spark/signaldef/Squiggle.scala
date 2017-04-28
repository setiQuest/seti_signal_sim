package org.seti.simulator.signaldef

import java.util.Random;

private class Squiggle(rand: Random, dataClass: String) extends SignalDef(rand,dataClass) {

  signalClass = "squiggle"

  def next {

    deltaPhiRad = nextDoubleFromRange(-120.0, 120.0) 

    if (dataClass == "basic"){
      SNR = nextDoubleFromRange(0.1, 0.3)
      drift = nextDoubleFromRange(-0.002, 0.002)
      sigmaSquiggle = nextDoubleFromRange(0.005, 0.01)
    }
    else {
      SNR = nextDoubleFromRange(0.0075, 0.5) 
      drift = nextDoubleFromRange(-0.0075, 0.0075) 
      sigmaSquiggle = nextDoubleFromRange(0.0001, 0.01)
    }


    //random chance for pure noise
    //keep this number a secret!!
    if(dataClass == "test") {
      if (rand.nextDouble > 0.11) {
        SNR = 0.0
      }
    }
  }
}