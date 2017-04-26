package org.seti.simulator.signaldef

import java.util.Random;

import org.seti.simulator.signaldef._

object SignalDefFactory {

  def apply(signalClass: String, rand: Random, dataClass: String) : SignalDef = signalClass.toLowerCase match {
    case "test" => {
      val signalDef = new TestSignal(rand,dataClass)
      signalDef.next
      return signalDef
    }
    case "narrowband" => {
      val signalDef = new NarrowBand(rand,dataClass)
      signalDef.next
      return signalDef
    }
    case "squarepulsednarrowband" => {
      val signalDef = new SquarePulsedNarrowBand(rand,dataClass)
      signalDef.next
      return signalDef
    }
    case "squigglesquarepulsednarrowband" => {
      val signalDef = new SquiggleSquarePulsedNarrowBand(rand,dataClass)
      signalDef.next
      return signalDef
    }
    case "squiggle" => {
      val signalDef = new Squiggle(rand,dataClass)
      signalDef.next
      return signalDef
    }
    case "narrowbanddrd" => {
      val signalDef = new NarrowBandDRD(rand,dataClass)
      signalDef.next
      return signalDef
    }
    case "squigglesquarepulsednarrowbanddrd" => {
      val signalDef = new NarrowBandDRD(rand,dataClass)
      signalDef.next
      return signalDef
    }
    case "brightpixel" => {
      val signalDef = new BrightPixel(rand,dataClass)
      signalDef.next
      return signalDef
    }
    case "noise" => {
      val signalDef = new PureNoise(rand,dataClass)
      signalDef.next
      return signalDef
    }
    
  }

}

