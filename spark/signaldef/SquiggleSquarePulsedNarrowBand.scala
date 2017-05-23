package org.seti.simulator.signaldef

import java.util.Random;

private class SquiggleSquarePulsedNarrowBand(rand: Random, dataClass: String) extends SignalDef(rand,dataClass) {

  signalClass = "squigglesquarepulsednarrowband"
  ampModType = "square"

  def next {

    deltaPhiRad = nextDoubleFromRange(-120.0, 120.0) 
    SNR = nextDoubleFromRange(0.05, 0.5) 
    drift = nextDoubleFromRange(-0.0075, 0.0075) 
    sigmaSquiggle = nextDoubleFromRange(0.0001, 0.01)

    ampModPeriod = nextDoubleFromRange(5*6144, 50*6144)
    ampModDuty = nextDoubleFromRange(0.05, 0.9)

    // //random chance for pure noise
    // //keep this number a secret!!
    // if(dataClass == "test") {
    //   if (rand.nextDouble > 0.13) {
    //     SNR = 0.0
    //   }
    // }
  }
}