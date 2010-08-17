package com.lasic.cloud.mock

import com.lasic.util.Logging
import com.lasic.cloud.{ImageState, ScalingTrigger, LaunchConfiguration, ScalingGroup}

/**
 *
 * @author Brian Pugh
 */

class MockScalingGroup extends ScalingGroup with Logging {
  private var imageState = ImageState.Unknown

  def createUpdateScalingTrigger(trigger: ScalingTrigger) = {
    logger.info("creating scaling trigger: " + trigger)
  }

  def deleteScalingGroup(name: String) = {
    logger.info("deleteing scaling trigger: " + name)
  }

  def createScalingGroup(autoScalingGroupName: String, launchConfigurationName: String, min: Int, max: Int, availabilityZones: scala.List[String]) = {
    logger.info("creating scaling group: " + autoScalingGroupName)
  }

  def deleteLaunchConfiguration(name: String) {
    logger.info("deleting launch config : " + name)
  }

  def createScalingLaunchConfiguration(config: LaunchConfiguration) {
    logger.info("creating launch config : " + config)
  }

  def deleteSnapshotAndDeRegisterImage(imageId: String) {
    logger.info("deleting and deregistering imageid : " + imageId)
  }

  def createImageForScaleGroup(instanceId: String, name: String, description: String, reboot: Boolean) = {
    logger.info("creating image for scale group: " + name)
    imageState = ImageState.Available
    "mock-image-id"
  }

  def getImageState(imageId: String) = {
    imageState
  }
}