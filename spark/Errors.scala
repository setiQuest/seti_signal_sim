package org.seti.simulator.errors

case class MisMatchDigest(message: String) extends Exception(message)

case class MissingSunNoise(message: String) extends Exception(message)
