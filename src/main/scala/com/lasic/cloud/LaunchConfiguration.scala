package com.lasic.cloud

import com.lasic.model.NodeInstance
import com.lasic.LasicProperties

/**
 * User: Brian Pugh
 * Date: May 10, 2010
 */
class LaunchConfiguration {
  var name: String = null
  var machineImage: String = null
  var ramdiskId: String = null
  var kernelId: String = null
  var key: String = null
  var groups: List[String] = List()
  var instanceType  = "small"
  var userName: String = null
  var s3Download: String = null
  var availabilityZone: String = LasicProperties.getProperty("availability_zone", "us-east-1d")


  //val scpDeclarations: List[ScpDeclaration] = new ArrayList[ScpDeclaration]
  //private ScriptDeclaration script = new ScriptDeclaration();
  //private final val startupScripts: List[ScriptDeclaration] = new ArrayList[ScriptDeclaration]

  override def toString = "name [" + name + "] " +
   "machineImage ["  + machineImage + "]" +
   "ramdiskId ["  + ramdiskId + "]" +
   "kernelId ["  + kernelId + "]" +
   "key ["  + key + "]" +
   "groups ["  + groups.mkString(", ") + "]" +
   "instanceType ["  + instanceType + "]" +
   "userName ["  + userName + "]" +
   "s3Download ["  + s3Download + "]" +
   "availabilityZone ["  + availabilityZone + "]"
}

object LaunchConfiguration {
  def build(node:NodeInstance) = {
    val lc = new LaunchConfiguration
    if (node != null) {
      lc.name = node.parent.name
      lc.machineImage = node.parent.machineimage
      lc.ramdiskId = node.parent.ramdiskid
      lc.kernelId = node.parent.kernelid
      lc.key = node.parent.key
      lc.groups = node.parent.groups
      //lc.instanceType = node.parent.instancetype
      lc.userName = node.parent.user
      lc.instanceType = node.parent.instancetype
      //lc.availabilityZone = node.parent.
      //lc.s3Download = ??
    }
    lc
  }
}