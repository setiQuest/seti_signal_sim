package org.seti.simulator

import apps.simulate.NoiseGenerator
import org.seti.simulator.objectstorage.SwiftObjStore

class SunNoise(container: String, objectname: String, objstore: SwiftObjStore) extends NoiseGenerator {
  var index: Int = 0
  var vals = objstore.get(container, objectname)
  
  setName("sunnoise")

  override def next() : Double = {
    index += 1
    return vals(index-1) * getAmp()
  }

}
