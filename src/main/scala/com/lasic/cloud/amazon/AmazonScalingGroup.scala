package com.lasic.cloud.amazon

import collection.JavaConversions
import collection.JavaConversions.asList
import java.util.{List => JList}
import com.lasic.cloud.ImageState._
import com.xerox.amazonws.ec2.{Jec2, AutoScaling, LaunchConfiguration => TypicaLaunchConfig, ScalingTrigger => AmazonScalingTrigger}
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.auth.BasicAWSCredentials
import com.lasic.cloud._
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.autoscaling.model.{CreateOrUpdateScalingTriggerRequest, DescribeAutoScalingGroupsRequest, DescribeLaunchConfigurationsRequest}
import com.amazonaws.services.autoscaling.model.{UpdateAutoScalingGroupRequest, CreateLaunchConfigurationRequest, Dimension, DescribeLaunchConfigurationsResult}
import com.amazonaws.services.ec2.model.{DeleteSnapshotRequest, DeregisterImageRequest, DescribeImagesRequest, CreateImageRequest}

/**
 *
 * @author Brian Pugh
 */

class AmazonScalingGroup(awsClient: AmazonEC2Client, autoscaling: AutoScaling) extends ScalingGroup {
  private val awsScalingClient = new AmazonAutoScalingClient(new BasicAWSCredentials(autoscaling.getAwsAccessKeyId, autoscaling.getSecretAccessKey))

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


  def createScalingLaunchConfiguration(config: LaunchConfiguration) {
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
    autoscaling.deleteLaunchConfiguration(name)
  }

  def createScalingGroup(autoScalingGroupName: String, launchConfigurationName: String, min: Int, max: Int, availabilityZones: List[String]) {
    autoscaling.createAutoScalingGroup(autoScalingGroupName, launchConfigurationName, min, max, 0, JavaConversions.asList(availabilityZones))
  }

  def updateScalingGroup(autoScalingGroupName: String, min: Int, max: Int) {
    val updateRequest = new UpdateAutoScalingGroupRequest()
    updateRequest.setAutoScalingGroupName(autoScalingGroupName)
    updateRequest.setMinSize(max)
    updateRequest.setMaxSize(max)
    awsScalingClient.updateAutoScalingGroup(updateRequest)
  }

  def deleteScalingGroup(name: String) {
    autoscaling.deleteAutoScalingGroup(name)
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
    val lasicLC = new LaunchConfiguration
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