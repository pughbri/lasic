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
    assert(vm.getState() == MachineState.Pending, "expected pending, got " + vm.getState())
    Thread.sleep(1000)
    assert(vm.getState() == MachineState.Pending, "expected pending, got " + vm.getState())
    Thread.sleep(2000)
    assert(vm.getState() == MachineState.Running, "expected Running, got " + vm.getState())

    vm.reboot()
    Thread.sleep(200)
    assert(vm.getState() == MachineState.Pending || vm.getState() == MachineState.Rebooting, "expected pending or rebooting, got " + vm.getState())
    Thread.sleep(1000)
    assert(vm.getState() == MachineState.Pending, "expected pending, got " + vm.getState())
    Thread.sleep(2000)
    assert(vm.getState() == MachineState.Running, "expected Running, got " + vm.getState())


    vm.shutdown()
    Thread.sleep(200)
    assert(vm.getState() == MachineState.ShuttingDown|| vm.getState() == MachineState.Terminated, "expected shuttingdown or terminated, got " + vm.getState())

  }

  def testStartVMFromCloud() = {
    val cloud = new MockCloud(2)
    val vms: List[VM] = cloud.createVMs(new LaunchConfiguration(null), 1, true)
    val vm = vms(0)
    Thread.sleep(200)
    assert(vm.getState() == MachineState.Pending, "expected pending, got " + vm.getState())
    Thread.sleep(1000)
    assert(vm.getState() == MachineState.Pending, "expected pending, got " + vm.getState())
    Thread.sleep(2000)
    assert(vm.getState() == MachineState.Running, "expected Running, got " + vm.getState())

    cloud.reboot(vms)
    Thread.sleep(200)
    assert(vm.getState() == MachineState.Pending || vm.getState() == MachineState.Rebooting, "expected pending or rebooting, got " + vm.getState())
    Thread.sleep(1000)
    assert(vm.getState() == MachineState.Pending, "expected pending, got " + vm.getState())
    Thread.sleep(2000)
    assert(vm.getState() == MachineState.Running, "expected Running, got " + vm.getState())


    cloud.terminate(vms)
    Thread.sleep(200)
    assert(vm.getState() == MachineState.ShuttingDown|| vm.getState() == MachineState.Terminated, "expected shuttingdown or terminated, got " + vm.getState())


  }
  
}