package com.lasic.cloud.mock

import com.lasic.util.Logging
import com.lasic.cloud._

/**
 *
 * @author Brian Pugh
 */

object MockScalingGroupClient extends ScalingGroupClient with Logging {
  class InternalScaleGroup {
    var name = ""
    var triggers = List[ScalingTrigger]()
    var min = 0
    var max = 0
    var lbNames = List[String]()
  }
  private var scaleGroups = List[InternalScaleGroup]()
  private var launchConfigs = List[String]()
  private var imageState = ImageState.Unknown
  private val lock = "lock"

  def createUpdateScalingTrigger(trigger: ScalingTrigger) = {
    logger.info("creating scaling trigger: " + trigger)
    lock.synchronized {
      val scaleGroup = scaleGroups.find(t => t.name == trigger.autoScalingGroupName)
      scaleGroup match {
        case Some(s) => s.triggers ::= trigger
        case None => throw new Exception("unknown scale group: " + trigger.autoScalingGroupName)
      }
    }
  }

  def deleteScalingGroup(name: String) = {
    logger.info("deleting scaling group: " + name)
    lock.synchronized {
      scaleGroups = scaleGroups.filter(group => group.name != name)
    }

  }

  def createScalingGroup(autoScalingGroupName: String, launchConfigurationName: String, min: Int, max: Int, lbNames: List[String], availabilityZones: scala.List[String]) = {
    logger.info("creating scaling group: " + autoScalingGroupName)
    val group = new InternalScaleGroup
    group.name = autoScalingGroupName
    group.min = min
    group.max = max
    group.lbNames = lbNames
    lock.synchronized {
      scaleGroups ::= group
    }

  }

  def deleteLaunchConfiguration(name: String) {
    logger.info("deleting launch config : " + name)
    lock.synchronized {
      launchConfigs = launchConfigs.filter(configName => configName != name)
    }
  }

  def createScalingLaunchConfiguration(config: LaunchConfiguration) {
    logger.info("creating launch config : " + config)
    lock.synchronized {
      launchConfigs ::= config.name
    }
  }

  def deleteSnapshotAndDeRegisterImage(imageId: String) {
    logger.info("deleting and deregistering imageid : " + imageId)
  }

  def createImageForScaleGroup(instanceId: String, name: String, description: String, reboot: Boolean) = {
    logger.info("creating image for scale group: " + name)
    lock.synchronized {
      imageState = ImageState.Available
    }
    "mock-image-id"
  }

  def getImageState(imageId: String) = {
    lock.synchronized {
      imageState
    }
  }

  def getScalingLaunchConfiguration(configName: String) = {
    val lc = new LaunchConfiguration
    lc.name = "mock-launch-config"
    lc
  }

  def getScaleGroups = {
    lock.synchronized {
      scaleGroups
    }
  }

  def getlaunchConfigs = {
    lock.synchronized {
      launchConfigs
    }
  }

  def reset() {
    lock.synchronized {
      scaleGroups = List[InternalScaleGroup]()
      launchConfigs = List[String]()
      imageState = ImageState.Unknown
    }
  }

  def updateScalingGroup(autoScalingGroupName: String, min: Int, max: Int) = {
    lock.synchronized {
      val scaleGroup = scaleGroups.find(t => t.name == autoScalingGroupName)
      scaleGroup match {
        case Some(s) => {
          s.min = min
          s.max = max
        }
        case None => throw new Exception("unknown scale group: " + autoScalingGroupName)
      }
    }
  }

  def describeAutoScalingGroup(autoScalingGroupName: String): ScalingGroupInfo = {
    lock.synchronized {
      val scaleGroup = scaleGroups.find(t => t.name == autoScalingGroupName)
      scaleGroup match {
        case Some(s) => {
          new ScalingGroupInfo(autoScalingGroupName,"",s.min, s.max, 0,0, null, null)
        }
        case None => throw new Exception("unknown scale group: " + autoScalingGroupName)
      }
    }
  }

}