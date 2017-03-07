package org.seti.simulator.signaldef

import java.util.Random;

private class PureNoise(rand: Random, dataClass: String) extends SignalDef(rand) {

  signalClass = "noise"

  def next {
    return
  }
}