package com.lasic.cloud.amazon

import com.xerox.amazonws.ec2.InstanceType
import com.xerox.amazonws.ec2.{LaunchConfiguration => AmazonLaunchConfiguration}
import com.lasic.cloud.LaunchConfiguration
import collection.JavaConversions
import collection.JavaConversions.asBuffer

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
        case "xlargehmem" => InstanceType.XLARGE_HMEM
      }
    }
    else {
      instanceType
    }
  }

  def createAmazonLaunchConfiguration(lasicLC: LaunchConfiguration): AmazonLaunchConfiguration = {
    val launchConfig = new AmazonLaunchConfiguration(lasicLC.machineImage, 1, 1)
    launchConfig.setKernelId(lasicLC.kernelId)
    launchConfig.setRamdiskId(lasicLC.ramdiskId)
    launchConfig.setAvailabilityZone(lasicLC.availabilityZone)
    launchConfig.setInstanceType(getInstanceType(lasicLC.instanceType))
    launchConfig.setKeyName(lasicLC.key);
    launchConfig.setSecurityGroup(JavaConversions.asList(lasicLC.groups))
    launchConfig
  }

  def createLaunchConfiguration(amazonLC: AmazonLaunchConfiguration): LaunchConfiguration = {
    val lasicLC = new LaunchConfiguration()
    lasicLC.machineImage = amazonLC.getImageId
    lasicLC.kernelId = amazonLC.getKernelId
    lasicLC.ramdiskId = amazonLC.getRamdiskId
    lasicLC.availabilityZone = amazonLC.getAvailabilityZone
    lasicLC.instanceType = amazonLC.getInstanceType.toString
    lasicLC.key = amazonLC.getKeyName
    lasicLC.groups = makeList(amazonLC.getSecurityGroup)
    lasicLC
  }

  //TODO: there has got to be a better way to convert from a java list to a
  //TODO: immutable list (or even from mutable.buffer to immutable list)
  private def makeList(buffer: scala.collection.mutable.Buffer[String]) = {
    val list = List()
    buffer.foreach(s => s :: list)
    list
  }

}