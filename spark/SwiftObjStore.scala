package org.seti.simulator.objectstorage

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
//import java.util.Properties

import org.apache.hadoop.conf.Configuration

import scala.collection.Map

class SwiftObjStore (val mConf: Configuration, val configurationName: String) {
  

  var currentContainer:String = _
  var fs:FileSystem = _
  var objectPath:String = _

  def setup(container: String, objectname: String) {
    if (container != currentContainer) {
      objectPath  = s"swift2d://$container.$configurationName/$objectname";
      fs = FileSystem.get(URI.create(objectPath), mConf)
    } else {
      objectPath  = s"swift2d://$container.$configurationName/$objectname";
    }
  }
  
  //lazy val fs:FileSystem = FileSystem.get(URI.create(ptemplate), mConf);


  def put(container: String, objectname: String, data: Array[Byte]) = {
    setup(container,objectname)
    val out: FSDataOutputStream =  fs.create(new Path(objectPath));
    out.write(data);
    out.close();
  }

  def get(container: String, objectname: String) : Array[Byte] = {
    setup(container,objectname)
    var fstatus = fs.getFileStatus(new Path(objectPath))
    var vals:Array[Byte] = new Array[Byte](fstatus.getLen.toInt)
    val in: FSDataInputStream =  fs.open(new Path(objectPath))
    in.readFully(vals);
    return vals;
  }

  def delete(container: String, objectname: String ) {
    setup(container,objectname)
    fs.delete(new Path(objectPath), false);
  } 
} 

    
    