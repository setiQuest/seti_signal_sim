package org.seti.simulator.errors

case class MisMatchDigest(message: String) extends Exception(message)
