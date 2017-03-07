package org.seti.simulator.signaldef

import java.util.Random;

private class TestSignal(rand: Random, dataClass: String) extends SignalDef(rand,dataClass) {
  
  signalClass = "test"

  def next {

    deltaPhiRad = nextDoubleFromRange(-120.0, 120.0) 
    SNR = nextDoubleFromRange(0.003, 0.3) 
    drift = nextDoubleFromRange(-0.001, 0.001) 


  }
}