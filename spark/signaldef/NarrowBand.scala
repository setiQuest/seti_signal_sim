package org.seti.simulator.signaldef

private class NarrowBand(seed: Long) extends SignalDef(seed) {

  signalClass = "narrowband"

  def next {

    deltaPhiRad = nextDoubleFromRange(-120.0, 120.0) 
    SNR = nextDoubleFromRange(0.003, 0.3) 
    drift = nextDoubleFromRange(0.000, 0.001) 

  }
}