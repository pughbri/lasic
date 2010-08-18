package com.lasic.cloud.amazon

import com.xerox.amazonws.ec2.InstanceType
import com.xerox.amazonws.ec2.{LaunchConfiguration => AmazonLaunchConfiguration}
import com.lasic.cloud.LaunchConfiguration
import collection.JavaConversions

/**
 * 
 * @author Brian Pugh
 */

object MappingUtil {
  def getInstanceType(instanceTypeStr: String) = {
      val instanceType = InstanceType.getTypeFromString(instanceTypeStr)
      if (instanceType == null) {
        instanceTypeStr match {
          case "small" => InstanceType.DEFAULT
          case "medium" => InstanceType.MEDIUM_HCPU
          case "large" => InstanceType.LARGE
          case "xlarge" => InstanceType.XLARGE
        }
      }
      else {
        instanceType
      }
    }

  def createLaunchConfiguration(lasicLC: LaunchConfiguration): AmazonLaunchConfiguration = {
    val launchConfig = new AmazonLaunchConfiguration(lasicLC.machineImage, 1, 1)
    launchConfig.setKernelId(lasicLC.kernelId)
    launchConfig.setRamdiskId(lasicLC.ramdiskId)
    launchConfig.setAvailabilityZone(lasicLC.availabilityZone)
    launchConfig.setInstanceType(getInstanceType(lasicLC.instanceType))
    launchConfig.setKeyName(lasicLC.key);
    launchConfig.setSecurityGroup(JavaConversions.asList(lasicLC.groups))
    launchConfig
  }

}