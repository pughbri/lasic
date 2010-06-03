package com.lasic.cloud

import com.xerox.amazonws.ec2.InstanceType
import com.lasic.model.NodeInstance
import com.lasic.LasicProperties

/**
 * User: Brian Pugh
 * Date: May 10, 2010
 */
class LaunchConfiguration(node: NodeInstance) {
  var name: String = null
  var machineImage: String = null
  var ramdiskId: String = null
  var kernelId: String = null
  var key: String = null
  var groups: String = null
  val instanceType: InstanceType = InstanceType.DEFAULT
  var userName: String = null
  var s3Download: String = null
  var availabilityZone: String = LasicProperties.getProperty("availability_zone", "us-east-1d")

  if (node != null) {
    name = node.parent.name
    machineImage = node.parent.machineimage
    ramdiskId = node.parent.ramdiskid
    kernelId = node.parent.kernelid
    key = node.parent.key
    //lc.groups = node.parent.groups
    //lc.instanceType = node.parent.instancetype
    userName = node.parent.user
    //lc.availabilityZone = node.parent.
    //lc.s3Download = ??
  }
  //val scpDeclarations: List[ScpDeclaration] = new ArrayList[ScpDeclaration]
  //private ScriptDeclaration script = new ScriptDeclaration();
  //private final val startupScripts: List[ScriptDeclaration] = new ArrayList[ScriptDeclaration]
}