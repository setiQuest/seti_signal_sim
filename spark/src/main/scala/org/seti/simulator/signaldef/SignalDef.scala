package org.seti.simulator.signaldef

import java.util.Random

abstract class SignalDef(val rand: Random, val dataClass: String) {

  //default values
  val sigmaN: Double = 13.0
  var deltaPhiRad: Double = 0.0
  var SNR: Double = 0.0
  var drift: Double = 0.0
  var driftRateDerivate: Double = 0.0
  var sigmaSquiggle: Double = 0.0
  //var outputLength: Int = 786432 128 * 6144
  val outputLength: Int = 196608 //32 * 6144
  var ampModType: String = "none"
  var ampModPeriod: Double = 0.0
  var ampModDuty: Double = 0.0
  var signalClass: String = _

  next //call next in constructor!

  //each subclass needs to define this
  def next()

  def nextDoubleFromRange(min: Double, max: Double): Double = {
    min + (max - min) * rand.nextDouble
  }

  override def toString: String = {
    "dataClass = " + dataClass + "\n"
  }
}
