package org.seti.simulator.objectstorage

import java.io.ByteArrayInputStream
import java.util.Properties

import org.openstack4j.api.OSClient.OSClientV3
import org.openstack4j.api.client.IOSClientBuilder
import org.openstack4j.model.common.{Identifier, Payloads}
import org.openstack4j.openstack.OSFactory

class OpenStack4jObjectStore(val creds: Properties,
                             val configurationName: String) {

  val domainIdent: Identifier = Identifier.byName(creds.getProperty("domain"))
  val projectIdent: Identifier = Identifier.byName(creds.getProperty("project"))
  val osc: IOSClientBuilder.V3 = OSFactory.builderV3()
  osc.endpoint(creds.getProperty("auth_url"))
  osc.credentials(creds.getProperty("user_id"), creds.getProperty("password"))
  osc.scopeToProject(projectIdent, domainIdent)
  val os: OSClientV3 = osc.authenticate()

  def put(container: String, objectname: String, data: Array[Byte]): String = {
    os.objectStorage()
      .objects()
      .put(
        container,
        objectname,
        Payloads.create(new ByteArrayInputStream(data))
      )
  }

  def delete(container: String, objectname: String) {
    os.objectStorage().objects().delete(container, objectname)
  }
}
