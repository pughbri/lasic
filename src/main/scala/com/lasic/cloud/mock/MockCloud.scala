package com.lasic.cloud


import mock.MockVM
import com.lasic.{VM, Cloud}
import java.lang.String
import java.util.Calendar

/**
 * User: Brian Pugh
 * Date: May 10, 2010
 */

class MockCloud(startupDelay: Int) extends Cloud {
  def this() = this (2);

  override def createVMs(launchConfig: LaunchConfiguration, numVMs: Int, startVM: Boolean): Array[VM] = {
    createVMs(numVMs, startVM) {new MockVM(startupDelay, this)}
  }

  def start(vms: Array[VM]) {
    vms.foreach(vm => System.out.println("starting vm [" + vm + "]...."))
  }

  def getStartupDelay(): Int = {
    startupDelay
  }

  def reboot(vms: Array[VM]) {

    vms.foreach(vm => System.out.println("rebooting vm [" + vm + "]...."))
  }

  def terminate(vms: Array[VM]) {
    vms.foreach(vm => System.out.println("shutting down vm [" + vm + "]...."))
  }

  def getState(vm: VM) = {
    MachineState.Unknown
  }

  def getPublicDns(vm: VM): String = {
    "mock-public-dns"
  }

  def getPrivateDns(vm: VM): String = {
    "mock-private-dns"
  }


  def createVolume(size: Int, snapID: String, availabilityZone: String) = {
    new VolumeInfo("id", "10g", "snapid", "east", "up", Calendar.getInstance,
      List[AttachmentInfo](new AttachmentInfo("vol-id", "inst-id", "device", "up", Calendar.getInstance)))
  }


  def attach(volumeInfo: VolumeInfo, vm: VM, devicePath: String): AttachmentInfo =  {
     new AttachmentInfo("volumeid", "instanceid","/some/device","good",Calendar.getInstance)
  }
}