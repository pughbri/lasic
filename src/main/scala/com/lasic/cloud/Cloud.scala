package com.lasic

import cloud._
import com.lasic.cloud.MachineState._
import java.util.{List => JList}



/**
 * User: Brian Pugh
 * Date: May 10, 2010
 */
trait Cloud {

  def createVM(launchConfig:LaunchConfiguration, startVM:Boolean) = {
    createVMs(launchConfig,1,startVM)(0)
  }
  def createVMs(launchConfig: LaunchConfiguration, numVMs: Int, startVM: Boolean): List[VM]

  def findVM(instanceId: String): VM

  def createImage(instanceId: String, name: String, description: String, reboot: Boolean): String

  def start(vms: List[VM])

  def reboot(vms: List[VM])

  def terminate(vms: List[VM])

  def getState(vm: VM): MachineState

  def getPublicDns(vm: VM): String

  def getPrivateDns(vm: VM): String

  def allocateAddress(): String

  def releaseAddress(ip: String)

  def associateAddress(vm: VM, ip: String)

  def disassociateAddress(ip: String)

//  def detach(volumeInfo: VolumeInfo, vm: VM, devicePath: String, force: Boolean) : AttachmentInfo

//  def createLaunchConfiguration(config: LaunchConfiguration)

  def createAutoScalingLaunchConfiguration(config: LaunchConfiguration)

  def createAutoScalingGroup(launchConfigurationName: String, autoScalingGroupName: String, min: Int, max: Int, availabilityZone: JList[String])

  def createUpdateScalingTrigger(trigger: ScalingTrigger)

  /**
   * @param size - size in gigabytes
   */
  def createVolume(config:VolumeConfiguration): Volume

//  def deleteVolume(volumeId: String)
//
//  def attach(volumeInfo: VolumeInfo, vm: VM, devicePath: String): AttachmentInfo
//
//  def volumeState(volumeId:String):VolumeState

  protected def createVMs(numVMs: Int, startVM: Boolean)(createVM: => VM): List[VM] = {
    var vms = List[VM]()
    for (i <- 0 until numVMs) {
      vms = createVM :: vms
    }

    if (startVM) {
      start(vms)
    }

    vms
  }

}