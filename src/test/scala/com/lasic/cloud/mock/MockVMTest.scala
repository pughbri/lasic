package com.lasic.cloud.mock

import junit.framework.TestCase
import com.lasic.cloud.{LaunchConfiguration, MachineState}
import com.lasic.cloud.VM

/**
 *
 * User: Brian Pugh
 * Date: May 25, 2010
 */

class MockVMTest extends TestCase("MockVMTest") {

  /**
   *  Test that VM goes through the expected states
   */
  def testVMStates() = {
    val vm = new MockVM(new MockCloud(1))
    vm.startup()
    waitForState(vm, MachineState.Pending, 5)
    waitForState(vm, MachineState.Running, 2)

    vm.reboot()
    waitForState(vm, MachineState.Rebooting, 5)
    waitForState(vm, MachineState.Running, 2)

    vm.shutdown()
    waitForState(vm, MachineState.Terminated, 3)

  }

  /**
   *  Test that a VM is created and gets started
   */
  def testStartVMFromCloud() = {
    val cloud = new MockCloud(1)
    val vms: List[VM] = cloud.createVMs(new LaunchConfiguration, 1, true)
    waitForState(vms(0), MachineState.Running, 5)
  }

  def waitForState(vm: VM, expectedState: MachineState.MachineState, timeout: Int) {
    val startTime = System.currentTimeMillis
    var currState = vm.getMachineState
    while (currState != expectedState) {
      if (System.currentTimeMillis - startTime > (timeout * 1000)) {
        throw new Exception("timed out waiting for state [" + expectedState + "]. Current state [" + currState + "]")
      }
      Thread.sleep(200)
      currState = vm.getMachineState
    }
  }

}