package org.seti.simulator.signaldef

import java.util.Random;

private class BrightPixel(rand: Random, dataClass: String) extends SignalDef(rand,dataClass) {

  signalClass = "brightpixel"
  ampModType = "brightpixel"
  ampModPeriod = 32*6144

  def next {

    deltaPhiRad = nextDoubleFromRange(-120.0, 120.0) 
    SNR = nextDoubleFromRange(0.05, 0.5) 
    drift = nextDoubleFromRange(-0.0075, 0.0075) 

    ampModDuty = nextDoubleFromRange(0.25/32.0, 1.0/32.0)

    // //random chance for pure noise
    // //keep this number a secret!!
    // if(dataClass == "test") {
    //   if (rand.nextDouble > 0.083) {
    //     SNR = 0.0
    //   }
    // }
  }
}