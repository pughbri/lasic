package com.lasic.cloud.amazon

import collection.JavaConversions
import collection.JavaConversions.asMap
import collection.JavaConversions.asBuffer
import java.util.{List => JList}
import com.xerox.amazonws.monitoring.{Statistics, StandardUnit}
import com.lasic.cloud.ImageState._
import com.lasic.cloud.{ImageState, ScalingTrigger, LaunchConfiguration, ScalingGroup}
import collection.mutable.Buffer
import com.xerox.amazonws.ec2.{BlockDeviceMapping, ImageDescription, Jec2, AutoScaling, ScalingTrigger => AmazonScalingTrigger}

/**
 *
 * @author Brian Pugh
 */

class AmazonScalingGroup(val ec2: Jec2, val autoscaling: AutoScaling) extends ScalingGroup {

  def createImageForScaleGroup(instanceId: String, name: String, description: String, reboot: Boolean): String = {
    ec2.createImage(instanceId, name, description, !reboot)
  }

  def getImageState(imageId: String): ImageState = {
    val imageIds = new java.util.ArrayList[String]()
    imageIds.add(imageId)
    val imageDescriptions: JList[ImageDescription] = ec2.describeImages(imageIds)
    require(imageDescriptions.size == 1)
    ImageState.withName(imageDescriptions.get(0).getImageState)
  }


  def deleteSnapshotAndDeRegisterImage(imageId: String) = {
    val imageDescriptions: Buffer[ImageDescription] = ec2.describeImages(JavaConversions.asList(List(imageId)))
    require(imageDescriptions.size == 1)
    ec2.deregisterImage(imageId)
    val blockDeviceMappings: Buffer[BlockDeviceMapping] = imageDescriptions(0).getBlockDeviceMapping
    blockDeviceMappings.foreach(bdMapping => {
      ec2.deleteSnapshot(bdMapping.getSnapshotId)
    })
  }


  def createScalingLaunchConfiguration(config: LaunchConfiguration) {
    var launchConfig = MappingUtil.createLaunchConfiguration(config)
    launchConfig.setConfigName(config.name)
    //todo: Typica seems to be sending invalid request for security group: see http://code.google.com/p/typica/issues/detail?id=103
    launchConfig.setSecurityGroup(null)
    autoscaling.createLaunchConfiguration(launchConfig)
  }


  def deleteLaunchConfiguration(name: String) {
   autoscaling.deleteLaunchConfiguration(name)
  }

  def createScalingGroup(autoScalingGroupName: String, launchConfigurationName: String, min: Int, max: Int, availabilityZones: List[String]) {
    autoscaling.createAutoScalingGroup(autoScalingGroupName, launchConfigurationName, min, max, 0, JavaConversions.asList(availabilityZones))
  }

  def deleteScalingGroup(name: String) {
    autoscaling.deleteAutoScalingGroup(name)
  }
  
  def createUpdateScalingTrigger(trigger: ScalingTrigger) {
    val scalingTrigger = new AmazonScalingTrigger(trigger.name,
      trigger.autoScalingGroupName,
      trigger.measureName,
      Statistics.AVERAGE,
      Map("AutoScalingGroupName" -> trigger.autoScalingGroupName), //dimensions
      trigger.period,
      StandardUnit.PERCENT,
      null, //CustomUnit
      trigger.lowerThreshold,
      trigger.lowerBreachScaleIncrement,
      trigger.upperThreshold,
      trigger.upperBreachScaleIncrement,
      trigger.breachDuration,
      null, //status
      null //createdTime
      )
    autoscaling.createOrUpdateScalingTrigger(scalingTrigger)
  }

}