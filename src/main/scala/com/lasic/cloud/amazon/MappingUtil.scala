package com.lasic.cloud.amazon

import com.xerox.amazonws.ec2.{InstanceType => TypicaInstanceType}
import com.xerox.amazonws.ec2.{LaunchConfiguration => TypicaLaunchConfiguration}
import com.lasic.cloud.LaunchConfiguration
import collection.JavaConversions
import collection.JavaConversions.asBuffer
import com.amazonaws.services.ec2.model.{InstanceType, Placement, RunInstancesRequest}
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest

/**
 *
 * @author Brian Pugh
 */

object MappingUtil {
  def getTypicaInstanceType(instanceTypeStr: String) = {
    val instanceType = TypicaInstanceType.getTypeFromString(instanceTypeStr)
    if (instanceType == null) {
      instanceTypeStr match {
        case "small" => TypicaInstanceType.DEFAULT
        case "medium" => TypicaInstanceType.MEDIUM_HCPU
        case "large" => TypicaInstanceType.LARGE
        case "xlarge" => TypicaInstanceType.XLARGE
        case "xlargehmem" => TypicaInstanceType.XLARGE_HMEM
      }
    }
    else {
      instanceType
    }
  }

  def getAWSInstanceType(instanceTypeStr: String) = {
    instanceTypeStr match {
      case "micro" => InstanceType.T1Micro
      case "small" => InstanceType.M1Small
      case "large" => InstanceType.M1Large
      case "xlarge" => InstanceType.M1Xlarge
      case "xlargehmem" => InstanceType.M2Xlarge
      case "xlargedoublehmem" => InstanceType.M22xlarge
      case "xlargequadhmem" => InstanceType.M24xlarge
      case "highcpumedium" => InstanceType.C1Medium
      case "highcpuextralarge" => InstanceType.C1Xlarge
      case "cluster" => InstanceType.Cc14xlarge
    }
  }


  def createTypicaLaunchConfiguration(lasicLC: LaunchConfiguration): TypicaLaunchConfiguration = {
    val launchConfig = new TypicaLaunchConfiguration(lasicLC.machineImage, 1, 1)
    launchConfig.setKernelId(lasicLC.kernelId)
    launchConfig.setRamdiskId(lasicLC.ramdiskId)
    launchConfig.setAvailabilityZone(lasicLC.availabilityZone)
    launchConfig.setInstanceType(getTypicaInstanceType(lasicLC.instanceType))
    launchConfig.setKeyName(lasicLC.key)
    launchConfig.setSecurityGroup(JavaConversions.asList(lasicLC.groups))
    launchConfig
  }

  def createAWSRunInstancesRequest(lasicLC: LaunchConfiguration): RunInstancesRequest = {
    //    val launchConfig = new RunInstancesRequest(lasicLC.machineImage, 1, 1)
    val launchConfig = new RunInstancesRequest()
    launchConfig.setImageId(lasicLC.machineImage)
    launchConfig.setMinCount(1)
    launchConfig.setMaxCount(1)
    launchConfig.setKernelId(lasicLC.kernelId)
    launchConfig.setRamdiskId(lasicLC.ramdiskId)
    launchConfig.setPlacement(new Placement().withAvailabilityZone(lasicLC.availabilityZone))
    launchConfig.setInstanceType(getAWSInstanceType(lasicLC.instanceType).toString)
    launchConfig.setKeyName(lasicLC.key);
    launchConfig.setSecurityGroups(JavaConversions.asList(lasicLC.groups))
    launchConfig
  }

  def createLaunchConfiguration(awsLC: RunInstancesRequest): LaunchConfiguration = {
    val lasicLC = new LaunchConfiguration()
    lasicLC.machineImage = awsLC.getImageId
    lasicLC.kernelId = awsLC.getKernelId
    lasicLC.ramdiskId = awsLC.getRamdiskId
    lasicLC.availabilityZone = awsLC.getPlacement.getAvailabilityZone
    lasicLC.instanceType = awsLC.getInstanceType.toString
    lasicLC.key = awsLC.getKeyName
    lasicLC.groups = makeList(awsLC.getSecurityGroups)
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