package com.lasic.cloud.mock


//import mock.MockVM
import com.lasic.{VM, Cloud}
import java.lang.String
import java.util.{Random}
import com.lasic.cloud.{MachineState, LaunchConfiguration}
import com.lasic.util.Logging

/**
 * User: Brian Pugh
 * Date: May 10, 2010
 */

class MockCloud(startupDelay: Int) extends Cloud with Logging {
  def this() = this (2);

  override def createVMs(launchConfig: LaunchConfiguration, numVMs: Int, startVM: Boolean): List[VM] = {
    createVMs(numVMs, startVM) {new MockVM(startupDelay, this)}
  }


  def findVM(instanceId: String) = {
    val vm = new MockVM(startupDelay, null, this)
    vm.instanceId = instanceId
    vm ! vm.StateChange(MachineState.Running, 0)
    vm
  }

  def start(vms: List[VM]) {
    vms.foreach(vm => {
      vm match {
        case mvm: MockVM => {
          mvm ! mvm.StateChange(MachineState.Pending, 0)
          mvm ! mvm.StateChange(MachineState.Running, startupDelay)
          mvm ! ("init", true, startupDelay)
        }
        case _ => logger.info("starting vm " + vm)
      }
    })
  }

  def getStartupDelay(): Int = {
    startupDelay
  }

  def reboot(vms: List[VM]) {

    vms.foreach(vm => {
      vm match {
        case mvm: MockVM => {
          mvm ! mvm.StateChange(MachineState.Rebooting, 0)
          mvm ! mvm.StateChange(MachineState.Pending, 0)
          mvm ! mvm.StateChange(MachineState.Running, startupDelay)
        }
        case _ => logger.info("rebooting vm " + vm)
      }
    })
  }

  def terminate(vms: List[VM]) {
    vms.foreach(vm => {
      vm match {
        case mvm: MockVM => {
          mvm ! mvm.StateChange(MachineState.ShuttingDown, 0)
          mvm ! mvm.StateChange(MachineState.Terminated, 0)
        }
        case _ => logger.info("shutting down vm " + vm)
      }
    })
  }

  def getState(vm: VM) = {
    vm match {
      case mvm: MockVM => mvm.machineState
      case _ => MachineState.Unknown
    }
  }

  def getPublicDns(vm: VM): String = {
    if (vm.getMachineState == MachineState.Running) "mock-public-dns" else ""
  }

  def getPrivateDns(vm: VM): String = {
    "mock-private-dns"
  }


  def createVolume(size: Int, snapID: String, availabilityZone: String) = {
    new MockVDisk(this, size, snapID, availabilityZone)
  }


  //  def deleteVolume(volumeId: String) = {
  //    logger.info("deleted volume " + volumeId)
  //  }
  //
  //  def attach(volumeInfo: VolumeInfo, vm: VM, devicePath: String): AttachmentInfo = {
  //    new AttachmentInfo("volumeid", "instanceid", "/some/device", "good", Calendar.getInstance)
  //  }
  //
  //  def detach(volumeInfo: VolumeInfo, vm: VM, devicePath: String, force: Boolean) = {
  //    new AttachmentInfo(volumeInfo.volumeId, vm.instanceId, devicePath, "detached", Calendar.getInstance)
  //
  //  }

  def associateAddress(vm: VM, ip: String) {
    logger.info("associate ip [" + ip + "] with instance [" + vm.instanceId + "]")
  }


  def disassociateAddress(ip: String) = {
    logger.info("disassociate ip [" + ip + "]")

  }

  def allocateAddress() = {
    val random: Random = new Random()
    "10.255." + +random.nextInt(200) + "." + random.nextInt(200);
  }


  def releaseAddress(ip: String) = {
    logger.info("release ip [" + ip + "]")
  }
}