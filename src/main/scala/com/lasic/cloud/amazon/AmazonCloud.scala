package com.lasic.cloud.amazon

import java.lang.String
import java.util.{List => JList}
import scala.collection.JavaConversions.asBuffer
import scala.collection.JavaConversions.asMap
import collection.JavaConversions
import com.lasic.cloud.MachineState._
import com.lasic.cloud.ImageState._
import com.lasic.util.Logging
import com.lasic.{LasicProperties}
import com.xerox.amazonws.monitoring.{StandardUnit, Statistics}
import com.xerox.amazonws.ec2.{ImageDescription, Jec2, AutoScaling, InstanceType, ReservationDescription, LaunchConfiguration => AmazonLaunchConfiguration, ScalingTrigger => AmazonScalingTrigger}
import com.lasic.cloud._
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.{Instance, Reservation, DescribeInstancesRequest}
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient

/**
 * @author Brian Pugh
 * Date: May 10, 2010
 */

class AmazonCloud extends Cloud with Logging {
  lazy val ec2: Jec2 = {
    val (key, secret) = ec2Keys
    var cloudApiHost = LasicProperties.getProperty("CLOUD_API_HOST")
    if (cloudApiHost == null) {
      new Jec2(key, secret)
    }
    else {
      var cloudApiPath = LasicProperties.getProperty("CLOUD_API_PATH", "/services/Eucalyptus")
      var ec2tmp = new Jec2(key, secret, false, cloudApiHost, 8773)
      ec2tmp.setResourcePrefix(cloudApiPath)
      ec2tmp.setSignatureVersion(1)
      ec2tmp
    }
  }

  lazy val autoscaling: AutoScaling = {
    val (key, secret) = ec2Keys
    new AutoScaling(key, secret)
  }

  lazy val awsAutoScaling = {
    val (key, secret) = ec2Keys
    new AmazonAutoScalingClient(new BasicAWSCredentials(key, secret))
  }

  lazy val awsClient = {
    val (key, secret) = ec2Keys
    new AmazonEC2Client(new BasicAWSCredentials(key, secret))

  }

  lazy val awsLoadBalancer = {
    val (key, secret) = ec2Keys
    new AmazonElasticLoadBalancingClient(new BasicAWSCredentials(key, secret))

  }


  def ec2Keys = {
    val key: String = LasicProperties.getProperty("AWS_ACCESS_KEY")
    val secret: String = LasicProperties.getProperty("AWS_SECRET_KEY")
    if (key == null || secret == null)
      throw new Exception("must provide both AWS_ACCESS_KEY and AWS_SECRET_KEY in properties file")
    (key, secret)
  }


  def getLoadBalancerClient(): LoadBalancerClient = {
    new AmazonLoadBalancerClient(awsLoadBalancer)
  }
  def getScalingGroupClient(): ScalingGroupClient = {
    new AmazonScalingGroupClient(awsClient, awsAutoScaling)
  }

  override def createVMs(launchConfig: LaunchConfiguration, numVMs: Int, startVM: Boolean): List[VM] = {
    createVMs(numVMs, startVM) {new AmazonVM(awsClient, launchConfig)}
  }


  def findVM(instanceId: String) = {
    require(instanceId != null, "must provide an instance id to find a vm")
    var dir = new DescribeInstancesRequest()
    dir.setInstanceIds(JavaConversions.asList(List(instanceId)))
    val descriptions = awsClient.describeInstances(dir).getReservations()
    if (descriptions.size != 1) {
      null
    }
    else {
      val instance = descriptions.get(0).getInstances.get(0)
      val vm = new AmazonVM(awsClient, convertToLC(instance))
      vm.instanceId = instance.getInstanceId
      vm
    }
  }

  def convertToLC(instance:Instance): LaunchConfiguration = {
    val lc = new LaunchConfiguration
    lc.machineImage = instance.getImageId
    lc.ramdiskId = instance.getRamdiskId
    lc.kernelId = instance.getKernelId
    lc.instanceType = instance.getInstanceType.toString
    lc
  }

  def createVolume(config: VolumeConfiguration): Volume = {
    val vi: com.xerox.amazonws.ec2.VolumeInfo = ec2.createVolume(config.size.toString, config.snapID, config.availabilityZone)
    new AmazonVolume(ec2, vi.getVolumeId)
  }

  def allocateAddress() = {
    ec2.allocateAddress()
  }


  def releaseAddress(ip: String) = {
    ec2.releaseAddress(ip)
  }
}