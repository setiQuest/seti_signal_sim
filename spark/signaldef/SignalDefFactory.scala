package org.seti.simulator.signaldef

import org.seti.simulator.signaldef._

object SignalDefFactory {

  def apply(signalClass: String, initSeed: Long) : SignalDef = signalClass.toLowerCase match {
    case "test" => {
      val signalDef = new TestSignal(initSeed)
      //signalDef.signalClass = signalClass
      //signalDef.next
      return signalDef
    }
    case "narrowband" => {
      val signalDef = new NarrowBand(initSeed)
      //signalDef.signalClass = signalClass
      //signalDef.next
      return signalDef
    }
    
  }

}

