package com.lasic.cloud

import amazon.AmazonVM
import com.lasic.{LasicProperties, VM, Cloud}
import java.lang.String
import java.util.Iterator
import java.util.{List => JList}
import com.xerox.amazonws.ec2.{AutoScaling, Jec2, ReservationDescription, AttachmentInfo => XAttachmentInfo}
import scala.collection.JavaConversions.asBuffer
import collection.JavaConversions
import com.lasic.cloud.MachineState._
import com.lasic.util.Logging

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
      throw new Exception("must provide both ACCESS_KEY and SECRET_KEY in properties file")
    (key, secret)
  }

  override def createVMs(launchConfig: LaunchConfiguration, numVMs: Int, startVM: Boolean): List[VM] = {
    createVMs(numVMs, startVM) {new AmazonVM(this, launchConfig)}
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

  private def createLaunchConfiguration(lasicLC: LaunchConfiguration): com.xerox.amazonws.ec2.LaunchConfiguration = {
    val launchConfig = new com.xerox.amazonws.ec2.LaunchConfiguration(lasicLC.machineImage, 1, 1)
    launchConfig.setKernelId(lasicLC.kernelId)
    launchConfig.setRamdiskId(lasicLC.ramdiskId)
    launchConfig.setAvailabilityZone(lasicLC.availabilityZone)
    launchConfig.setInstanceType(lasicLC.instanceType)
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
      logger.info("terminating " + vm.instanceId)
      var instances = new java.util.ArrayList[String]
      instances.add(vm.instanceId)
      ec2.terminateInstances(instances)
    }
      )
  }


  private def getInstance(vm: VM): ReservationDescription#Instance = {
    val list: JList[ReservationDescription] = ec2.describeInstances(Array(vm.instanceId))
    if (list.size != 1) {
      throw new IllegalStateException("expected a single reservation description for instance id " + vm.instanceId + " but got " + list.size)
    }

    val instances: JList[ReservationDescription#Instance] = list.get(0).getInstances
    if (list.size != 1) {
      throw new IllegalStateException("expected a single instance for instance id " + vm.instanceId + " but got " + instances.size)
    }

    instances.get(0)
  }

  def getState(vm: VM): MachineState = {
    MachineState.withName(getInstance(vm).getState)
  }

  def getPublicDns(vm: VM): String = {
    getInstance(vm).getDnsName()
  }

  def getPrivateDns(vm: VM): String = {
    getInstance(vm).getPrivateDnsName()
  }


  def createVolume(size: Int, snapID: String, availabilityZone: String): VolumeInfo = {
    val vi: com.xerox.amazonws.ec2.VolumeInfo = ec2.createVolume(size.toString, snapID, availabilityZone)

    val typicaAttachmentInfoList = vi.getAttachmentInfo
    var attachmentInfoList = List[AttachmentInfo]()
    val iterator: Iterator[com.xerox.amazonws.ec2.AttachmentInfo] = typicaAttachmentInfoList.iterator()
    while (iterator.hasNext()) {
      val attachmentInfo: XAttachmentInfo = iterator.next
      val info: AttachmentInfo = new AttachmentInfo(attachmentInfo.getVolumeId,
        attachmentInfo.getInstanceId,
        attachmentInfo.getDevice,
        attachmentInfo.getStatus,
        attachmentInfo.getAttachTime)

      attachmentInfoList = info :: attachmentInfoList
    }


    new VolumeInfo(vi.getVolumeId, vi.getSize, vi.getSnapshotId, vi.getZone, vi.getStatus, vi.getCreateTime, attachmentInfoList)

  }


  def deleteVolume(volumeId: String) = {
    ec2.deleteVolume(volumeId)
  }

  def attach(volumeInfo: VolumeInfo, vm: VM, devicePath: String): AttachmentInfo = {
    var info: XAttachmentInfo = ec2.attachVolume(volumeInfo.volumeId, vm.instanceId, devicePath)
    new AttachmentInfo(info.getVolumeId, info.getInstanceId, info.getDevice, info.getStatus, info.getAttachTime)
  }

  def detach(volumeInfo: VolumeInfo, vm: VM, devicePath: String, force: Boolean): AttachmentInfo = {
    var info: XAttachmentInfo = ec2.detachVolume(volumeInfo.volumeId, vm.instanceId, devicePath, force)
    new AttachmentInfo(info.getVolumeId, info.getInstanceId, info.getDevice, info.getStatus, info.getAttachTime)
  }


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