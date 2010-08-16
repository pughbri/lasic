package com.lasic.cloud.amazon

import java.lang.String
import java.util.{List => JList}
import scala.collection.JavaConversions.asBuffer
import scala.collection.JavaConversions.asMap
import collection.JavaConversions
import com.lasic.cloud.MachineState._
import com.lasic.cloud.ImageState._
import com.lasic.util.Logging
import com.lasic.{LasicProperties, VM, Cloud}
import com.lasic.cloud._
import com.xerox.amazonws.monitoring.{StandardUnit, Statistics}
import com.xerox.amazonws.ec2.{ImageDescription, Jec2, AutoScaling, InstanceType, ReservationDescription, LaunchConfiguration => AmazonLaunchConfiguration, ScalingTrigger => AmazonScalingTrigger}

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
    new AutoScaling(key, secret);
  }


  def ec2Keys = {
    val key: String = LasicProperties.getProperty("AWS_ACCESS_KEY")
    val secret: String = LasicProperties.getProperty("AWS_SECRET_KEY")
    if (key == null || secret == null)
      throw new Exception("must provide both AWS_ACCESS_KEY and AWS_SECRET_KEY in properties file")
    (key, secret)
  }

  override def createVMs(launchConfig: LaunchConfiguration, numVMs: Int, startVM: Boolean): List[VM] = {
    createVMs(numVMs, startVM) {new AmazonVM(this, launchConfig)}
  }


  def findVM(instanceId: String) = {

    val descriptions = ec2.describeInstances(JavaConversions.asList(List(instanceId)))
    if (descriptions.size < 1) {
      null
    }
    else {
      //descriptions(0).getInstances().foreach(instance => {
      val instance = descriptions(0).getInstances()(0)
      val vm = new AmazonVM(this, convertToLC(instance))
      vm.instanceId = instance.getInstanceId
      vm
      //})
    }
  }

  def createImage(instanceId: String, name: String, description: String, reboot: Boolean): String = {
    ec2.createImage(instanceId, name, description, !reboot)
  }


  def convertToLC(instance: ReservationDescription#Instance): LaunchConfiguration = {
    val lc = new LaunchConfiguration
    lc.machineImage = instance.getImageId
    lc.ramdiskId = instance.getRamdiskId
    lc.kernelId = instance.getKernelId
    lc.instanceType = instance.getInstanceType.toString
    lc.availabilityZone = instance.getAvailabilityZone
    lc
  }

  def start(vms: List[VM]) {
    //todo: don't just iterate.  Batching things together and making a single call with params is MUCH more efficient
    vms.foreach(vm => {startVM(vm)})
  }

  private def startVM(vm: VM) {
    val amazonLC = createLaunchConfiguration(vm.launchConfiguration)
    val rd: ReservationDescription = ec2.runInstances(amazonLC)
    rd.getInstances().foreach(instance => vm.instanceId = instance.getInstanceId)
  }

  protected def getInstanceType(instanceTypeStr: String) = {
    val instanceType = InstanceType.getTypeFromString(instanceTypeStr)
    if (instanceType == null) {
      instanceTypeStr match {
        case "small" => InstanceType.DEFAULT
        case "medium" => InstanceType.MEDIUM_HCPU
        case "large" => InstanceType.LARGE
        case "xlarge" => InstanceType.XLARGE
      }
    }
    else {
      instanceType
    }
  }

  private def createLaunchConfiguration(lasicLC: LaunchConfiguration): AmazonLaunchConfiguration = {
    val launchConfig = new AmazonLaunchConfiguration(lasicLC.machineImage, 1, 1)
    launchConfig.setKernelId(lasicLC.kernelId)
    launchConfig.setRamdiskId(lasicLC.ramdiskId)
    launchConfig.setAvailabilityZone(lasicLC.availabilityZone)
    launchConfig.setInstanceType(getInstanceType(lasicLC.instanceType))
    launchConfig.setKeyName(lasicLC.key);
    launchConfig.setSecurityGroup(JavaConversions.asList(lasicLC.groups))
    launchConfig
  }

  def reboot(vms: List[VM]) {
    //    val vm: AmazonVM = new AmazonVM(this, new LaunchConfiguration(null))
    //   logger.info(vm.launchConfiguration)
  }

  def terminate(vms: List[VM]) {
    vms.foreach(vm => {
      logger.debug("terminating " + vm.instanceId)
      var instances = new java.util.ArrayList[String]
      instances.add(vm.instanceId)
      ec2.terminateInstances(instances)
    }
      )
  }

  def createAutoScalingLaunchConfiguration(config: LaunchConfiguration) = {
    var launchConfig = createLaunchConfiguration(config)
    //TODO: add a timestamp to the name (probably handle this in the verb)
    launchConfig.setConfigName(config.name)
    //todo: Typica seems to be sending invalid request for security group: see http://code.google.com/p/typica/issues/detail?id=103
    launchConfig.setSecurityGroup(null)
    autoscaling.createLaunchConfiguration(launchConfig)
  }

  def createAutoScalingGroup(autoScalingGroupName: String, launchConfigurationName: String, min: Int, max: Int, availabilityZones: List[String]) = {
    //TODO: add a timestamp to the name (probably handle this in the verb)
    autoscaling.createAutoScalingGroup(autoScalingGroupName, launchConfigurationName, min, max, 0, JavaConversions.asList(availabilityZones))
  }

  def createUpdateScalingTrigger(trigger: ScalingTrigger) = {
    var scalingTrigger = new AmazonScalingTrigger(trigger.name,
      trigger.autoScalingGroupName,
      trigger.measureName,
      Statistics.AVERAGE,
      Map("AutoScalingGroupName" -> trigger.autoScalingGroupName), //dimensions
      trigger.period,
      StandardUnit.PERCENT,
      null, //CustomUnit
      trigger.lowerThreshold,
      trigger.lowerBreachScaleIncrement,
      trigger.upperThreshold,
      trigger.upperBreachScaleIncrement,
      trigger.breachDuration,
      null, //status
      null //createdTime
      )
    autoscaling.createOrUpdateScalingTrigger(scalingTrigger)
  }

  private def getInstance(vm: VM): ReservationDescription#Instance = {
    val list: JList[ReservationDescription] = ec2.describeInstances(Array(vm.instanceId))
    if (list.size != 1) {
      throw new IllegalStateException("expected a single reservation description for instance vmId " + vm.instanceId + " but got " + list.size)
    }

    val instances: JList[ReservationDescription#Instance] = list.get(0).getInstances
    if (list.size != 1) {
      throw new IllegalStateException("expected a single instance for instance vmId " + vm.instanceId + " but got " + instances.size)
    }

    instances.get(0)
  }

  def getState(vm: VM): MachineState = {
    MachineState.withName(getInstance(vm).getState)
  }

  def getState(imageId: String): ImageState = {
    val imageIds = new java.util.ArrayList[String]()
    imageIds.add(imageId)
    val imageDescriptions: JList[ImageDescription] = ec2.describeImages(imageIds)
    require(imageDescriptions.length == 1)
    ImageState.withName(imageDescriptions.get(0).getImageState)
  }

  def getPublicDns(vm: VM): String = {
    getInstance(vm).getDnsName()
  }

  def getPrivateDns(vm: VM): String = {
    getInstance(vm).getPrivateDnsName()
  }


  def createVolume(config: VolumeConfiguration): Volume = {
    val vi: com.xerox.amazonws.ec2.VolumeInfo = ec2.createVolume(config.size.toString, config.snapID, config.availabilityZone)
    new AmazonVolume(ec2, vi.getVolumeId)
  }




  //  def deleteVolume(volumeId: String) = {
  //    ec2.deleteVolume(volumeId)
  //  }
  //
  //  def attach(volumeInfo: VolumeInfo, vm: VM, devicePath: String): AttachmentInfo = {
  //    var info: XAttachmentInfo = ec2.attachVolume(volumeInfo.volumeId, vm.instanceId, devicePath)
  //    new AttachmentInfo(info.getVolumeId, info.getInstanceId, info.getDevice, info.getStatus, info.getAttachTime)
  //  }
  //
  //  def detach(volumeInfo: VolumeInfo, vm: VM, devicePath: String, force: Boolean): AttachmentInfo = {
  //    var info: XAttachmentInfo = ec2.detachVolume(volumeInfo.volumeId, vm.instanceId, devicePath, force)
  //    new AttachmentInfo(info.getVolumeId, info.getInstanceId, info.getDevice, info.getStatus, info.getAttachTime)
  //  }


  def associateAddress(vm: VM, ip: String) = {
    ec2.associateAddress(vm.instanceId, ip)
  }


  def disassociateAddress(ip: String) = {
    ec2.disassociateAddress(ip)
  }

  def allocateAddress() = {
    ec2.allocateAddress()
  }


  def releaseAddress(ip: String) = {
    ec2.releaseAddress(ip)
  }
}