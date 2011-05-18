package com.lasic.cloud

import com.lasic.model.NodeInstance
import com.lasic.LasicProperties
import com.lasic.model.ScaleGroupConfiguration
import com.lasic.values.NodeProperties

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
  var instanceType = "small"
  var userName: String = null
  var s3Download: String = null
  var userData: String = null
  var availabilityZone: String = LasicProperties.getProperty("availability_zone", "us-east-1d")


  //val scpDeclarations: List[ScpDeclaration] = new ArrayList[ScpDeclaration]
  //private ScriptDeclaration script = new ScriptDeclaration();
  //private final val startupScripts: List[ScriptDeclaration] = new ArrayList[ScriptDeclaration]

  override def toString = "name [" + name + "] " +
          "machineImage [" + machineImage + "]" +
          "ramdiskId [" + ramdiskId + "]" +
          "kernelId [" + kernelId + "]" +
          "key [" + key + "]" +
          "groups [" + groups.mkString(", ") + "]" +
          "instanceType [" + instanceType + "]" +
          "userName [" + userName + "]" +
          "s3Download [" + s3Download + "]" +
          "userData [" + userData + "]" +
          "availabilityZone [" + availabilityZone + "]"
}

object LaunchConfiguration {
  
  def build(node: NodeInstance): LaunchConfiguration = {
    val lc = build(node.parent)
    lc
  }

  def build(nodeProps: NodeProperties) = {
    val lc = new LaunchConfiguration
    if (nodeProps != null) {
      lc.name = nodeProps.name
      lc.machineImage = nodeProps.machineimage
      lc.ramdiskId = nodeProps.ramdiskid
      lc.kernelId = nodeProps.kernelid
      lc.key = nodeProps.key
      lc.groups = nodeProps.groups
      lc.userData = nodeProps.data
      //lc.instanceType = node.parent.instancetype
      lc.userName = nodeProps.user
      lc.instanceType = nodeProps.instancetype
      //lc.availabilityZone = node.parent.
      //lc.s3Download = ??
    }
    lc
  }

  def buildMaster(nodeProps: NodeProperties) = {
    val lc = build(nodeProps)
    lc.userData = null
    lc
  }

}