package org.seti.simulator.signaldef

import java.util.Random;

private class NarrowBand(rand: Random, dataClass: String) extends SignalDef(rand,dataClass) {

  signalClass = "narrowband"

  def next {

    deltaPhiRad = nextDoubleFromRange(-120.0, 120.0) 
    SNR = nextDoubleFromRange(0.003, 0.3) 
    drift = nextDoubleFromRange(-0.0075, 0.0075) 

    //random chance for pure noise
    //keep this number a secret!!
    if(dataClass == "test") {
      if (rand.nextDouble > 0.08) {
        SNR = 0.0
      }
    }
  }
}