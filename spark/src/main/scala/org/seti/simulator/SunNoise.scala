package org.seti.simulator

import org.seti.simulate.NoiseGenerator
import org.seti.simulator.objectstorage.SwiftObjStore

class SunNoise(container: String, objectname: String, objstore: SwiftObjStore)
    extends NoiseGenerator {
  var index: Int = 0
  val vals: Array[Byte] = objstore.get(container, objectname)

  setName("sunnoise")

  override def next(): Double = {
    index += 1
    vals(index - 1) * getAmp
  }

}
