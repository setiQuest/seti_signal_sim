package org.seti.simulator.signaldef

import java.util.Random;

import org.seti.simulator.signaldef._

object SignalDefFactory {

  def apply(signalClass: String, rand: Random) : SignalDef = signalClass.toLowerCase match {
    case "test" => {
      val signalDef = new TestSignal(rand)
      signalDef.next
      return signalDef
    }
    case "narrowband" => {
      val signalDef = new NarrowBand(rand)
      signalDef.next
      return signalDef
    }
    case "noise" => {
      val signalDef = new PureNoise(rand)
      signalDef.next
      return signalDef
    }
    
  }

}

