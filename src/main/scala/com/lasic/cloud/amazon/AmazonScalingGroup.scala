package com.lasic.cloud.amazon

import collection.JavaConversions
import collection.JavaConversions.asBuffer
import collection.JavaConversions.asList
import java.util.{List => JList}
import com.lasic.cloud.ImageState._
import collection.mutable.Buffer
import com.xerox.amazonws.ec2.{BlockDeviceMapping, ImageDescription, Jec2, AutoScaling, LaunchConfiguration => AmazonLaunchConfig, ScalingTrigger => AmazonScalingTrigger}
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.auth.BasicAWSCredentials
import com.lasic.cloud._
import com.amazonaws.services.autoscaling.model.{CreateOrUpdateScalingTriggerRequest, DescribeAutoScalingGroupsRequest}
import com.amazonaws.services.autoscaling.model.{Dimension, UpdateAutoScalingGroupRequest}
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.CreateImageRequest

/**
 *
 * @author Brian Pugh
 */

class AmazonScalingGroup(ec2: Jec2, autoscaling: AutoScaling) extends ScalingGroup {
  private val awsScalingClient = new AmazonAutoScalingClient(new BasicAWSCredentials(ec2.getAwsAccessKeyId, ec2.getSecretAccessKey))
  private val awsClient = new AmazonEC2Client(new BasicAWSCredentials(ec2.getAwsAccessKeyId, ec2.getSecretAccessKey))
  implicit def unboxInt(i: java.lang.Integer) = i.intValue
  implicit def javaListToImmutableScalaList[A](list: JList[A]) : List[A] = {
     JavaConversions.asBuffer(list).toList 
  }

  def createImageForScaleGroup(instanceId: String, name: String, description: String, reboot: Boolean): String = {
//    ec2.createImage(instanceId, name, description, !reboot)
    val imageRequest = new CreateImageRequest().withInstanceId(instanceId).withName(name).withDescription(description).withNoReboot(!reboot)
    awsClient.createImage(imageRequest).getImageId
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
    var launchConfig = MappingUtil.createTypicaLaunchConfiguration(config)
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

    //Typica is broken.  Use amazon api. http://code.google.com/p/typica/issues/detail?id=98
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
    val imageDescriptions: Buffer[AmazonLaunchConfig] = autoscaling.describeLaunchConfigurations(JavaConversions.asList(List(configName)))
    require(imageDescriptions.size == 1)
    MappingUtil.createLaunchConfiguration(imageDescriptions(0))
  }
}