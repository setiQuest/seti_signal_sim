package org.seti.simulator.signaldef

import java.util.Random;

private class TestSignal(rand: Random) extends SignalDef(rand) {
  
  signalClass = "test"

  def next {

    deltaPhiRad = nextDoubleFromRange(-120.0, 120.0) 
    SNR = nextDoubleFromRange(0.003, 0.3) 
    drift = nextDoubleFromRange(0.000, 0.001) 

  }
}