package org.seti.simulator.objectstorage

// import org.apache.hadoop.fs.FSDataInputStream;
// import org.apache.hadoop.fs.FSDataOutputStream;
// import org.apache.hadoop.fs.FileStatus;
// import org.apache.hadoop.fs.FileSystem;
// import org.apache.hadoop.fs.Path;

// import java.io.EOFException;
// import java.io.IOException;
// import java.net.URI;

// import org.apache.hadoop.conf.Configuration

import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.common.Payloads;

import java.io._;

class ObjectStore (val userId: String, val password: String, val auth_url: String, val domain: String, val project: String) {
  val container : String = "simsignals"  

  //val configurationName = "something"
//   var config: AccountConfig = new AccountConfig()
//   config.setUsername(userId)
//   config.setPassword(password)
//   config.setAuthUrl(auth_url)
//   config.setTenantName(project)
// //            .setTenantId(tenantId)

//   var account:Account = new AccountFactory(config).createAccount();

  // var mConf: Configuration = new Configuration(true)
  // mConf.set("fs.swift2d.impl","com.ibm.stocator.fs.ObjectStoreFileSystem");
  // mConf.set(s"fs.swift2d.service.$configurationName.auth.url", auth_url + "auth/tokens");
  // mConf.set(s"fs.swift2d.service.$configurationName.public", "true");
  // mConf.set(s"fs.swift2d.service.$configurationName.tenant", project);
  // mConf.set(s"fs.swift2d.service.$configurationName.password", password);
  // mConf.set(s"fs.swift2d.service.$configurationName.username", userId);
  // mConf.set(s"fs.swift2d.service.$configurationName.region", "dallas");


  val domainIdent: Identifier = Identifier.byName(domain);
  val projectIdent: Identifier  = Identifier.byName(project);
  val osc = OSFactory.builderV3()
  osc.endpoint(auth_url)
  osc.credentials(userId, password)
  osc.scopeToProject(projectIdent, domainIdent)
  val os : OSClientV3 = osc.authenticate()


  def put(objectname: String, data: InputStream) : String = {
    os.objectStorage().objects().put(container, objectname, Payloads.create(data));
  }

  def delete(objectname: String ) {
    os.objectStorage().objects().delete(container, objectname);
  } 
} 

    
    