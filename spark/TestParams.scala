package org.seti.simulator.parameters


class TestSignal {
  var sigmaN = 13
  var deltaPhiRad = 10.0
  var SNR = 0.1
  var drift = 0.0001
  var driftRateDerivate = 0.0
  var sigmaSquiggle = 0.0
  var outputLength = 792576
  var ampModType = "none"
  var ampModPeriod = 0
  var ampModDuty = 0.0
  var signalClass = "narrow-test"

  def next {
    //this will generate a new set of parameters based on the ranges
    //allowd by this signal class
  }
}