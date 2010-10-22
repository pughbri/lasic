package com.lasic.cloud

import com.lasic.cloud.ImageState._

/**
 *
 * @author Brian Pugh
 */

trait ScalingGroupClient {

  def getImageState(imageId: String): ImageState 

  def createImageForScaleGroup(instanceId: String, name: String, description: String, reboot: Boolean) : String

  def deleteSnapshotAndDeRegisterImage(imageId: String)

  def createScalingLaunchConfiguration(config: LaunchConfiguration)

  def getScalingLaunchConfiguration(configName: String): LaunchConfiguration

  def deleteLaunchConfiguration(name: String)

  def createScalingGroup(autoScalingGroupName: String, launchConfigurationName: String, min: Int, max: Int, lbNames: List[String], availabilityZones: List[String])

  def updateScalingGroup(autoScalingGroupName: String, min: Int, max: Int)

  def describeAutoScalingGroup(autoScalingGroupName: String): ScalingGroupInfo

  def deleteScalingGroup(name: String)

  def createUpdateScalingTrigger(trigger: ScalingTrigger)



}