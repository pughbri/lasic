package com.lasic.cloud.mock

import com.lasic.util.Logging
import com.lasic.cloud.{ImageState, ScalingTrigger, LaunchConfiguration, ScalingGroup}

/**
 *
 * @author Brian Pugh
 */

object MockScalingGroup extends ScalingGroup with Logging {
  class InternalScaleGroup {
    var name = ""
    var triggers = List[ScalingTrigger]()
  }
  private var scaleGroups = List[InternalScaleGroup]()
  private var launchConfigs = List[String]()
  private var imageState = ImageState.Unknown

  def createUpdateScalingTrigger(trigger: ScalingTrigger) = {
    logger.info("creating scaling trigger: " + trigger)
    val scaleGroup = scaleGroups.find(t => t.name == trigger.autoScalingGroupName)
    scaleGroup.get.triggers ::= trigger
  }

  def deleteScalingGroup(name: String) = {
    logger.info("deleting scaling group: " + name)
    scaleGroups = scaleGroups.filter(group => group.name != name)

  }

  def createScalingGroup(autoScalingGroupName: String, launchConfigurationName: String, min: Int, max: Int, availabilityZones: scala.List[String]) = {
    logger.info("creating scaling group: " + autoScalingGroupName)
    val group = new InternalScaleGroup
    group.name = autoScalingGroupName
    scaleGroups ::= group
  }

  def deleteLaunchConfiguration(name: String) {
    logger.info("deleting launch config : " + name)
    launchConfigs = launchConfigs.filter(configName => configName != name)
  }

  def createScalingLaunchConfiguration(config: LaunchConfiguration) {
    logger.info("creating launch config : " + config)
    launchConfigs ::= config.name
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

  def getScalingLaunchConfiguration(configName: String) = {
    val lc = new LaunchConfiguration
    lc.name = "mock-launch-config"
    lc
  }

  def getScaleGroups = {
    scaleGroups
  }

  def getlaunchConfigs = {
    launchConfigs
  }

  def reset() {
    scaleGroups = List[InternalScaleGroup]()
    launchConfigs = List[String]()
    imageState = ImageState.Unknown
  }

}