package org.seti.simulator.signaldef

import java.util.Random

import org.seti.simulator.signaldef._

object SignalDefFactory {

  def apply(signalClass: String, rand: Random, dataClass: String): SignalDef =
    signalClass.toLowerCase match {
      case "test" =>
        val signalDef = new TestSignal(rand, dataClass)
        signalDef
      case "narrowband" =>
        val signalDef = new NarrowBand(rand, dataClass)
        signalDef
      case "squarepulsednarrowband" =>
        val signalDef = new SquarePulsedNarrowBand(rand, dataClass)
        signalDef
      case "sinepulsednarrowband" =>
        val signalDef = new SinePulsedNarrowBand(rand, dataClass)
        signalDef
      case "squigglesquarepulsednarrowband" =>
        val signalDef = new SquiggleSquarePulsedNarrowBand(rand, dataClass)
        signalDef
      case "squigglesinepulsednarrowband" =>
        val signalDef = new SquiggleSinePulsedNarrowBand(rand, dataClass)
        signalDef
      case "squiggle" =>
        val signalDef = new Squiggle(rand, dataClass)
        signalDef
      case "narrowbanddrd" =>
        val signalDef = new NarrowBandDRD(rand, dataClass)
        signalDef
      case "squigglesquarepulsednarrowbanddrd" =>
        val signalDef = new SquiggleSquarePulsedNarrowBandDRD(rand, dataClass)
        signalDef
      case "squigglesinepulsednarrowbanddrd" =>
        val signalDef = new SquiggleSinePulsedNarrowBandDRD(rand, dataClass)
        signalDef
      case "brightpixel" =>
        val signalDef = new BrightPixel(rand, dataClass)
        signalDef
      case "noise" =>
        val signalDef = new PureNoise(rand, dataClass)
        signalDef

    }

}
