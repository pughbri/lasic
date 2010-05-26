package com.lasic.cloud.mock

import junit.framework.TestCase
import com.lasic.cloud.{MachineState, MockCloud}

/**
 *
 * User: Brian Pugh
 * Date: May 25, 2010
 */

class MockVMTest extends TestCase("MockVMTest") {
  def testVMStates() = {
    val vm = new MockVM(2, null, new MockCloud)
    vm.startup()
    assert(vm.getState() == MachineState.Pending, "expected pending, got " + vm.getState())
    Thread.sleep(1000)
    assert(vm.getState() == MachineState.Pending, "expected pending, got " + vm.getState())
    Thread.sleep(2000)
    assert(vm.getState() == MachineState.Running, "expected Running, got " + vm.getState())

    vm.reboot()
    assert(vm.getState() == MachineState.Pending, "expected pending, got " + vm.getState())
    Thread.sleep(1000)
    assert(vm.getState() == MachineState.Pending, "expected pending, got " + vm.getState())
    Thread.sleep(2000)
    assert(vm.getState() == MachineState.Running, "expected Running, got " + vm.getState())

  }
}