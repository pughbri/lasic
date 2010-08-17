package com.lasic.cloud

import com.lasic.cloud.ImageState._

/**
 *
 * @author Brian Pugh
 */

trait ScalingGroup {

  def getImageState(imageId: String): ImageState 

  def createImageForScaleGroup(instanceId: String, name: String, description: String, reboot: Boolean) : String

  def deleteSnapshotAndDeRegisterImage(imageId: String)

  def createScalingLaunchConfiguration(config: LaunchConfiguration)

  def deleteLaunchConfiguration(name: String)

  def createScalingGroup(autoScalingGroupName: String, launchConfigurationName: String, min: Int, max: Int, availabilityZones: List[String])

  def deleteScalingGroup(name: String)

  def createUpdateScalingTrigger(trigger: ScalingTrigger)



}