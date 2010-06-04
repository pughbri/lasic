package com.lasic.cloud.mock

import junit.framework.TestCase
import com.lasic.cloud.{LaunchConfiguration, MachineState}
import com.lasic.VM

/**
 *
 * User: Brian Pugh
 * Date: May 25, 2010
 */

class MockVMTest extends TestCase("MockVMTest") {
  def testVMStates() = {
    val vm = new MockVM(new MockCloud(2))
    vm.startup()
    Thread.sleep(200)
    assert(vm.getMachineState() == MachineState.Pending, "expected pending, got " + vm.getMachineState())
    Thread.sleep(1000)
    assert(vm.getMachineState() == MachineState.Pending, "expected pending, got " + vm.getMachineState())
    Thread.sleep(2000)
    assert(vm.getMachineState() == MachineState.Running, "expected Running, got " + vm.getMachineState())

    vm.reboot()
    Thread.sleep(200)
    assert(vm.getMachineState() == MachineState.Pending || vm.getMachineState() == MachineState.Rebooting, "expected pending or rebooting, got " + vm.getMachineState())
    Thread.sleep(1000)
    assert(vm.getMachineState() == MachineState.Pending, "expected pending, got " + vm.getMachineState())
    Thread.sleep(2000)
    assert(vm.getMachineState() == MachineState.Running, "expected Running, got " + vm.getMachineState())


    vm.shutdown()
    Thread.sleep(200)
    assert(vm.getMachineState() == MachineState.ShuttingDown|| vm.getMachineState() == MachineState.Terminated, "expected shuttingdown or terminated, got " + vm.getMachineState())

  }

  def testStartVMFromCloud() = {
    val cloud = new MockCloud(2)
    val vms: List[VM] = cloud.createVMs(new LaunchConfiguration(null), 1, true)
    val vm = vms(0)
    Thread.sleep(200)
    assert(vm.getMachineState() == MachineState.Pending, "expected pending, got " + vm.getMachineState())
    Thread.sleep(1000)
    assert(vm.getMachineState() == MachineState.Pending, "expected pending, got " + vm.getMachineState())
    Thread.sleep(2000)
    assert(vm.getMachineState() == MachineState.Running, "expected Running, got " + vm.getMachineState())

    cloud.reboot(vms)
    Thread.sleep(200)
    assert(vm.getMachineState() == MachineState.Pending || vm.getMachineState() == MachineState.Rebooting, "expected pending or rebooting, got " + vm.getMachineState())
    Thread.sleep(1000)
    assert(vm.getMachineState() == MachineState.Pending, "expected pending, got " + vm.getMachineState())
    Thread.sleep(2000)
    assert(vm.getMachineState() == MachineState.Running, "expected Running, got " + vm.getMachineState())


    cloud.terminate(vms)
    Thread.sleep(200)
    assert(vm.getMachineState() == MachineState.ShuttingDown|| vm.getMachineState() == MachineState.Terminated, "expected shuttingdown or terminated, got " + vm.getMachineState())


  }
  
}