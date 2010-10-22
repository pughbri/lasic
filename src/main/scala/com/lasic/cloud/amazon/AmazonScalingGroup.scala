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

/**
 *
 * @author Brian Pugh
 */

class AmazonScalingGroupClient(awsClient: AmazonEC2Client, awsScalingClient: AmazonAutoScalingClient) extends ScalingGroupClient {

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

  def deleteScalingGroup(name: String) {
    awsScalingClient.deleteAutoScalingGroup(new DeleteAutoScalingGroupRequest().withAutoScalingGroupName(name))
  }

  def describeAutoScalingGroup(autoScalingGroupName: String): ScalingGroupInfo = {
    val request = new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(List(autoScalingGroupName))
    val groupsResult = awsScalingClient.describeAutoScalingGroups(request)
    val scaleGroups = JavaConversions.asBuffer(groupsResult.getAutoScalingGroups)
    require(scaleGroups.size == 1)
    val scaleGroup = scaleGroups(0)
    val instances = javaListToImmutableScalaList(scaleGroup.getInstances).map(inst => inst.getInstanceId)
    new ScalingGroupInfo(scaleGroup.getAutoScalingGroupName,
      scaleGroup.getLaunchConfigurationName,
      scaleGroup.getMinSize,
      scaleGroup.getMaxSize,
      scaleGroup.getDesiredCapacity,
      scaleGroup.getCooldown,
      scaleGroup.getAvailabilityZones,
      instances)
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