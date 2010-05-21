package com.lasic

import cloud.{AttachmentInfo, VolumeInfo, MachineState, LaunchConfiguration}

/**
 * User: Brian Pugh
 * Date: May 10, 2010
 */
trait Cloud {
  def createVMs(launchConfig: LaunchConfiguration, numVMs: Int, startVM: Boolean): Array[VM]

  def start(vms: Array[VM])

  def reboot(vms: Array[VM])

  def terminate(vms: Array[VM])

  def getState(vm: VM): MachineState.Value

  def getPublicDns(vm: VM): String

  def getPrivateDns(vm: VM): String

  def allocateAddress(): String

  def releaseAddress(ip: String)

  def associateAddress(vm: VM, ip: String)

  def disassociateAddress(ip: String)

  def detach(volumeInfo: VolumeInfo, vm: VM, devicePath: String, force: Boolean) : AttachmentInfo

  /**
   * @param size - size in gigabytes
   */
  def createVolume(size: Int, snapID: String, availabilityZone: String): VolumeInfo

  def deleteVolume(volumeId: String)

  def attach(volumeInfo: VolumeInfo, vm: VM, devicePath: String): AttachmentInfo

  protected def createVMs(numVMs: Int, startVM: Boolean)(createVM: => VM): Array[VM] = {
    var vms = new Array[VM](numVMs)
    for (i <- 0 until numVMs) {
      vms(i) = createVM
    }

    if (startVM) {
      start(vms)
    }

    vms
  }

}