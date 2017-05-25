package org.seti.simulator.signaldef

import java.util.Random;

import org.seti.simulator.signaldef._

object SignalDefFactory {

  def apply(signalClass: String, rand: Random, dataClass: String) : SignalDef = signalClass.toLowerCase match {
    case "test" => {
      val signalDef = new TestSignal(rand,dataClass)
      return signalDef
    }
    case "narrowband" => {
      val signalDef = new NarrowBand(rand,dataClass)
      return signalDef
    }
    case "squarepulsednarrowband" => {
      val signalDef = new SquarePulsedNarrowBand(rand,dataClass)
      return signalDef
    }
    case "sinepulsednarrowband" => {
      val signalDef = new SinePulsedNarrowBand(rand,dataClass)
      return signalDef
    }
    case "squigglesquarepulsednarrowband" => {
      val signalDef = new SquiggleSquarePulsedNarrowBand(rand,dataClass)
      return signalDef
    }
    case "squigglesinepulsednarrowband" => {
      val signalDef = new SquiggleSinePulsedNarrowBand(rand,dataClass)
      return signalDef
    }
    case "squiggle" => {
      val signalDef = new Squiggle(rand,dataClass)
      return signalDef
    }
    case "narrowbanddrd" => {
      val signalDef = new NarrowBandDRD(rand,dataClass)
      return signalDef
    }
    case "squigglesquarepulsednarrowbanddrd" => {
      val signalDef = new SquiggleSquarePulsedNarrowBandDRD(rand,dataClass)
      return signalDef
    }
    case "squigglesinepulsednarrowbanddrd" => {
      val signalDef = new SquiggleSinePulsedNarrowBandDRD(rand,dataClass)
      return signalDef
    }
    case "brightpixel" => {
      val signalDef = new BrightPixel(rand,dataClass)
      return signalDef
    }
    case "noise" => {
      val signalDef = new PureNoise(rand,dataClass)
      return signalDef
    }
    
  }

}

