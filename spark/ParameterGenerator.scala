package org.seti.simulator.parameters


class ParameterGenerator(signalClass: String) {
  val signalDef = new TestSignal
  signalDef.signalClass = signalClass

  def next : ParameterSet = {
    //gets next set of random values that characterize the signal type to be simulated
    signalDef.next

    //we then load it into a new ParameterSet object to be
    //used by the simulator

    var p: ParameterSet = new ParameterSet
    p.sigmaN = signalDef.sigmaN
    p.deltaPhiRad = signalDef.deltaPhiRad
    p.SNR = signalDef.SNR
    p.drift = signalDef.drift
    p.driftRateDerivate = signalDef.driftRateDerivate
    p.sigmaSquiggle = signalDef.sigmaSquiggle
    p.outputLength = signalDef.outputLength
    p.ampModType = signalDef.ampModType
    p.ampModPeriod = signalDef.ampModPeriod
    p.ampModDuty = signalDef.ampModDuty
    p.signalClass = signalDef.signalClass

    return p
  }
}

class ParameterSet {
  var sigmaN: Double = _
  var deltaPhiRad: Double = _
  var SNR: Double = _
  var drift: Double = _
  var driftRateDerivate: Double = _
  var sigmaSquiggle: Double = _
  var outputLength: Int = _
  var ampModType: String = _
  var ampModPeriod: Double = _
  var ampModDuty: Double = _
  var signalClass: String = _

  override def toString() : String = {
    "args:\n" +
      "sigmaN = " + sigmaN + "\n" +
      "deltaPhiRad = " + deltaPhiRad + "\n" +
      "SNR = " + SNR + "\n" +
      "drift = " + drift + "\n" +
      "driftRateDerivate = " + driftRateDerivate + "\n" +
      "sigmaSquiggle = " + sigmaSquiggle + "\n" +
      "outputLength = " + outputLength + "\n" +
      "ampModType = " + ampModType + "\n" +
      "ampModPeriod (only valid if type != none) = " + ampModPeriod + "\n" +
      "ampModDuty (only if type = 'square' or 'brightpixel') = " + ampModDuty + "\n" +
      "signalClass = " + signalClass + "\n"
  }
}