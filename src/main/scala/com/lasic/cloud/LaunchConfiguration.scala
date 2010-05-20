package com.lasic.cloud

import com.xerox.amazonws.ec2.InstanceType

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
  var groups: String = null
  val instanceType: InstanceType = InstanceType.DEFAULT
  var userName: String = null
  var s3Download: String = null
  var availabilityZone: String = "us-east-1d"
  //val scpDeclarations: List[ScpDeclaration] = new ArrayList[ScpDeclaration]
  //private ScriptDeclaration script = new ScriptDeclaration();
  //private final val startupScripts: List[ScriptDeclaration] = new ArrayList[ScriptDeclaration]
  }