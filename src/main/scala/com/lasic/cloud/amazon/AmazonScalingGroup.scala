package com.lasic.cloud.amazon

import collection.JavaConversions
import collection.JavaConversions.asList
import java.util.{List => JList}
import com.lasic.cloud.ImageState._
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.{DeleteSnapshotRequest, DeregisterImageRequest, DescribeImagesRequest, CreateImageRequest}
import com.amazonaws.services.autoscaling.model._
import com.lasic.cloud.{ScalingTrigger, ScalingGroupInfo, ImageState, ScalingGroupClient, LaunchConfiguration => LasicLaunchConfig}
import com.lasic.util.Logging
import com.amazonaws.AmazonServiceException
import com.lasic.LasicProperties
import org.apache.commons.codec.binary.Base64

/**
 *
 * @author Brian Pugh
 */

class AmazonScalingGroupClient(awsClient: AmazonEC2Client, awsScalingClient: AmazonAutoScalingClient) extends ScalingGroupClient with Logging {
  private val sleepDelay = LasicProperties.getProperty("SLEEP_DELAY", "10000").toInt

  implicit def unboxInt(i: java.lang.Integer) = i.intValue

  implicit def javaListToImmutableScalaList[A](list: JList[A]): List[A] = {
    JavaConversions.asBuffer(list).toList
  }

  def createImageForScaleGroup(instanceId: String, name: String, description: String, reboot: Boolean): String = {
    val imageRequest = new CreateImageRequest().withInstanceId(instanceId).withName(name).withDescription(description).withNoReboot(!reboot)
    awsClient.createImage(imageRequest).getImageId
  }

  def getImageState(imageId: String): ImageState = {
    val descImageResult = awsClient.describeImages(new DescribeImagesRequest().withImageIds(imageId))
    require(descImageResult.getImages.size == 1)
    ImageState.withName(descImageResult.getImages.get(0).getState)
  }


  def deleteSnapshotAndDeRegisterImage(imageId: String) = {
    val descImageResult = awsClient.describeImages(new DescribeImagesRequest().withImageIds(imageId))
    require(descImageResult.getImages.size == 1)

    awsClient.deregisterImage(new DeregisterImageRequest().withImageId(imageId))
    val blockDeviceMappings = descImageResult.getImages.get(0).getBlockDeviceMappings
    blockDeviceMappings foreach (
            bdMapping => awsClient.deleteSnapshot(new DeleteSnapshotRequest().withSnapshotId(bdMapping.getEbs.getSnapshotId))
            )
  }


  def createScalingLaunchConfiguration(config: LasicLaunchConfig) {
    val launchConfig = new CreateLaunchConfigurationRequest
    launchConfig.setImageId(config.machineImage)
    launchConfig.setInstanceType(MappingUtil.getAWSInstanceType(config.instanceType).toString)
    launchConfig.setSecurityGroups(config.groups)
    launchConfig.setLaunchConfigurationName(config.name)
    launchConfig.setKeyName(config.key)
    if (config.userData != null) {
      launchConfig.setUserData(config.userData)
    }
    launchConfig.setKernelId(config.kernelId)
    launchConfig.setRamdiskId(config.ramdiskId)
    awsScalingClient.createLaunchConfiguration(launchConfig)
  }


  def deleteLaunchConfiguration(name: String) {
    val delLaunchConfigReq = new DeleteLaunchConfigurationRequest().withLaunchConfigurationName(name)
    awsScalingClient.deleteLaunchConfiguration(delLaunchConfigReq)
  }

  def createScalingGroup(autoScalingGroupName: String, launchConfigurationName: String, min: Int, max: Int, lbNames: List[String], availabilityZones: List[String]) {
    val scaleGrpReq = new CreateAutoScalingGroupRequest()
    scaleGrpReq.setAutoScalingGroupName(autoScalingGroupName)
    scaleGrpReq.setLaunchConfigurationName(launchConfigurationName)
    scaleGrpReq.setMinSize(min)
    scaleGrpReq.setMaxSize(max)
    scaleGrpReq.setAvailabilityZones(availabilityZones)
    scaleGrpReq.setCooldown(0)
    scaleGrpReq.setLoadBalancerNames(lbNames)
    awsScalingClient.createAutoScalingGroup(scaleGrpReq)
  }

  def updateScalingGroup(autoScalingGroupName: String, min: Int, max: Int) {
    val updateRequest = new UpdateAutoScalingGroupRequest()
    updateRequest.setAutoScalingGroupName(autoScalingGroupName)
    updateRequest.setMinSize(max)
    updateRequest.setMaxSize(max)
    awsScalingClient.updateAutoScalingGroup(updateRequest)
  }

  def deleteScalingGroup(name: String, maxWaitSeconds: Int = 240) {
    val startTime = System.currentTimeMillis
    var deleted = false
    while (!deleted) {
      try {
        awsScalingClient.deleteAutoScalingGroup(new DeleteAutoScalingGroupRequest().withAutoScalingGroupName(name))
        deleted = true
      }
      catch {
        case ex: AmazonServiceException => {
          if (isTimedOut(startTime, maxWaitSeconds)) throw ex
          Thread.sleep(sleepDelay)
          logger.info("Attempting to delete scale group " + name + ".")
        }
      }
    }
  }

  private def isTimedOut(startTime: Long, maxWaitSeconds: Int): Boolean = {
    (((System.currentTimeMillis - startTime) / 1000) > maxWaitSeconds)
  }

  def describeAutoScalingGroup(autoScalingGroupName: String): ScalingGroupInfo = {
    val request = new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(List(autoScalingGroupName))
    val groupsResult = awsScalingClient.describeAutoScalingGroups(request)
    val scaleGroups = groupsResult.getAutoScalingGroups
    var scaleGroupInfo: ScalingGroupInfo = null
    require(scaleGroups.size <= 1, "should be at most one scale group with name [" + autoScalingGroupName + "]")
    if (scaleGroups.size == 1) {
      val scaleGroup = scaleGroups(0)
      val instances = javaListToImmutableScalaList(scaleGroup.getInstances).map(_.getInstanceId)
      scaleGroupInfo = new ScalingGroupInfo(scaleGroup.getAutoScalingGroupName,
        scaleGroup.getLaunchConfigurationName,
        scaleGroup.getMinSize,
        scaleGroup.getMaxSize,
        scaleGroup.getDesiredCapacity,
        scaleGroup.getCooldown,
        scaleGroup.getAvailabilityZones,
        instances)
    }
    scaleGroupInfo
  }

  def canScaleGroupBeShutdown(autoScalingGroupName: String): Boolean = {
    var canShutdown = false

    //check for scaling activities
    val request = new DescribeScalingActivitiesRequest().withAutoScalingGroupName(autoScalingGroupName)
    val scalingActivitiesResult = awsScalingClient.describeScalingActivities(request)
    val activities = scalingActivitiesResult.getActivities()
    val activeScaleActivities = activities exists (_.getStatusCode == "InProgress")

    //make sure there are no instances
    if (!activeScaleActivities) {
      val group = describeAutoScalingGroup(name)
      canShutdown = (group == null || (group.maxSize == 0 && group.instances.size == 0))
    }

    if (logger.isDebugEnabled) {
      logScaleGroupInfo(canShutdown, autoScalingGroupName, activeScaleActivities, activities)
    }

    canShutdown
  }

  private def logScaleGroupInfo(canShutdown: Boolean, autoScalingGroupName: String, activeScaleActivities: Boolean, activities: List[Activity]): Unit = {
    val group = describeAutoScalingGroup(name)
    var size = 0
    if (group != null) {
      size = group.instances.size
    }
    logger.debug("can shutdown: " + canShutdown
            + ". Instance count: "
            + size
            + ".  Active scale activities for "
            + autoScalingGroupName + ": "
            + activeScaleActivities + ".  Details activities InProgress: "
            + activities.filter(_.getStatusCode == "InProgress").mkString("\n "))
  }

  def createUpdateScalingTrigger(trigger: ScalingTrigger) {

    val triggerRequest = new CreateOrUpdateScalingTriggerRequest()
    triggerRequest.setTriggerName(trigger.name)
    triggerRequest.setAutoScalingGroupName(trigger.autoScalingGroupName)
    triggerRequest.setMeasureName(trigger.measureName)
    triggerRequest.setStatistic("Average")
    val dimension = new Dimension().withName("AutoScalingGroupName").withValue(trigger.autoScalingGroupName)
    triggerRequest.setDimensions(List(dimension))
    triggerRequest.setPeriod(trigger.period)
    triggerRequest.setUnit("Percent")
    triggerRequest.setLowerThreshold(trigger.lowerThreshold)
    triggerRequest.setLowerBreachScaleIncrement(trigger.lowerBreachScaleIncrement)
    triggerRequest.setUpperThreshold(trigger.upperThreshold)
    triggerRequest.setUpperBreachScaleIncrement(trigger.upperBreachScaleIncrement)
    triggerRequest.setBreachDuration(trigger.breachDuration)
    triggerRequest.setNamespace(trigger.namespace)
    awsScalingClient.createOrUpdateScalingTrigger(triggerRequest)
  }


  def getScalingLaunchConfiguration(configName: String) = {
    val dlcr = new DescribeLaunchConfigurationsRequest
    dlcr.setLaunchConfigurationNames(JavaConversions.asList(List(configName)))
    val imageDescriptions: DescribeLaunchConfigurationsResult = awsScalingClient.describeLaunchConfigurations(dlcr)
    require(imageDescriptions.getLaunchConfigurations.size == 1)
    val imd = imageDescriptions.getLaunchConfigurations.get(0)
    val lasicLC = new LasicLaunchConfig
    lasicLC.machineImage = imd.getImageId
    lasicLC.kernelId = imd.getKernelId
    lasicLC.ramdiskId = imd.getRamdiskId
    //    lasicLC.availabilityZone = imd.
    lasicLC.instanceType = imd.getInstanceType.toString
    lasicLC.key = imd.getKeyName
    lasicLC.groups = imd.getSecurityGroups
    lasicLC.name = imd.getLaunchConfigurationName
    lasicLC
  }
}