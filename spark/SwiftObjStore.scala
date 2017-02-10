package org.seti.simulator.objectstorage

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.util.Properties

import org.apache.hadoop.conf.Configuration

import scala.collection.Map

class SwiftObjStore (val creds: Properties, val configurationName: String) {
  
  var mConf: Configuration = new Configuration(true)
  mConf.set("fs.swift2d.impl","com.ibm.stocator.fs.ObjectStoreFileSystem");
  mConf.set(s"fs.swift2d.service.$configurationName.auth.url", creds.getProperty("auth_url") + "/auth/tokens");
  mConf.set(s"fs.swift2d.service.$configurationName.public", "true");
  mConf.set(s"fs.swift2d.service.$configurationName.tenant", creds.getProperty("project_id"));
  mConf.set(s"fs.swift2d.service.$configurationName.password", creds.getProperty("password"));
  mConf.set(s"fs.swift2d.service.$configurationName.username", creds.getProperty("user_id"));
  mConf.set(s"fs.swift2d.service.$configurationName.region", creds.getProperty("region"));

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

  def delete(container: String, objectname: String ) {
    setup(container,objectname)
    fs.delete(new Path(objectname), false);
  } 
} 

    
    