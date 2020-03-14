package org.seti.simulator.objectstorage

import java.net.URI

import org.apache.hadoop.fs.{
  FSDataInputStream,
  FSDataOutputStream,
  FileSystem,
  Path
}
//import java.util.Properties

import org.apache.hadoop.conf.Configuration

class SwiftObjStore(val mConf: Configuration, val configurationName: String) {

  val currentContainer: String = _
  var fs: FileSystem = _
  var objectPath: String = _

  def setup(container: String, objectname: String) {
    if (container != currentContainer) {
      objectPath = s"swift2d://$container.$configurationName/$objectname"
      fs = FileSystem.get(URI.create(objectPath), mConf)
    } else {
      objectPath = s"swift2d://$container.$configurationName/$objectname"
    }
  }

  //lazy val fs:FileSystem = FileSystem.get(URI.create(ptemplate), mConf);

  def put(container: String, objectname: String, data: Array[Byte]): Unit = {
    setup(container, objectname)
    val out: FSDataOutputStream = fs.create(new Path(objectPath))
    out.write(data)
    out.close()
  }

  def get(container: String, objectname: String): Array[Byte] = {
    setup(container, objectname)
    val fstatus = fs.getFileStatus(new Path(objectPath))
    val vals: Array[Byte] = new Array[Byte](fstatus.getLen.toInt)
    val in: FSDataInputStream = fs.open(new Path(objectPath))
    in.readFully(vals)
    vals
  }

  def delete(container: String, objectname: String) {
    setup(container, objectname)
    fs.delete(new Path(objectPath), false)
  }
}
