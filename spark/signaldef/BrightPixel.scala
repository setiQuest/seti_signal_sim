package org.seti.simulator.signaldef

import java.util.Random;

private class BrightPixel(rand: Random, dataClass: String) extends SignalDef(rand,dataClass) {

  signalClass = "brightpixel"
  ampModType = "square"
  ampModPeriod = 129*6144

  def next {

    deltaPhiRad = nextDoubleFromRange(-120.0, 120.0) 
    SNR = nextDoubleFromRange(0.003, 0.3) 
    drift = nextDoubleFromRange(-0.0075, 0.0075) 

    ampModDuty = nextDoubleFromRange(0.05/129.0, 1.0/129.0)

    //random chance for pure noise
    //keep this number a secret!!
    if(dataClass == "test") {
      if (rand.nextDouble > 0.08) {
        SNR = 0.0
      }
    }
  }
}