package com.lasic

import cloud.{AttachmentInfo, VolumeInfo, MachineState, LaunchConfiguration}

/**
 * User: Brian Pugh
 * Date: May 10, 2010
 */
trait Cloud {
  def createVMs(launchConfig: LaunchConfiguration, numVMs: Int, startVM: Boolean): List[VM]

  def start(vms: List[VM])

  def reboot(vms: List[VM])

  def terminate(vms: List[VM])

  def getState(vm: VM): MachineState.Value

  def getPublicDns(vm: VM): String

  def getPrivateDns(vm: VM): String

  def allocateAddress(): String

  def releaseAddress(ip: String)

  def associateAddress(vm: VM, ip: String)

  def disassociateAddress(ip: String)

  def detach(volumeInfo: VolumeInfo, vm: VM, devicePath: String, force: Boolean) : AttachmentInfo

//  def createLaunchConfiguration(config: LaunchConfiguration)

  /**
   * @param size - size in gigabytes
   */
  def createVolume(size: Int, snapID: String, availabilityZone: String): VolumeInfo

  def deleteVolume(volumeId: String)

  def attach(volumeInfo: VolumeInfo, vm: VM, devicePath: String): AttachmentInfo

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