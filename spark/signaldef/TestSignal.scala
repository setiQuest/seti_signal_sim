package org.seti.simulator.signaldef

private class TestSignal(seed: Long) extends SignalDef(seed) {
  
  signalClass = "test"

  def next {

    deltaPhiRad = nextDoubleFromRange(-120.0, 120.0) 
    SNR = nextDoubleFromRange(0.003, 0.3) 
    drift = nextDoubleFromRange(0.000, 0.001) 

  }
}