package com.lasic.cloud

import junit.framework.TestCase
import mock.{MockCloud, MockVM}
import ssh.{ConnectException, SshSession}
import java.io.File

/**
 *
 * User: Brian Pugh
 * Date: May 25, 2010
 */

class VMTest extends TestCase("VMTest") {
  //setup

  class MockSshSession(numTimesToFailOnConnect: Int) extends SshSession {
    def this() = this (0)

    var currentNumFailures = 0

    override def connect(dnsName: String, userName: String, pemFile: File) = {
      if (numTimesToFailOnConnect > currentNumFailures) {
        currentNumFailures += 1
        throw new ConnectException("test failure:  " + currentThread + " out of " + numTimesToFailOnConnect + " have occurred", new RuntimeException())
      }
      currentNumFailures = 0
      true
    }

    override def disconnect = {}
  }

  def testConnect() = {

    val lc: LaunchConfiguration = new LaunchConfiguration(null)
    lc.key = "some"
    var state = MachineState.Unknown
    var mockSshSession = new MockSshSession
    val vm = new MockVM(2, lc, new MockCloud(0)) {
      override def getState() = {
        state
      }
    }


    //test1: fail on non-initalized vm
    try {
      vm.connect(mockSshSession, 2)
      assert(false, "should have got IllegalStateException: VM hasn't been initialized")
    }
    catch {
      case t: IllegalStateException => { //expected
      }
      case t: Throwable => {
        assert(false, "unexpected exception " + t)
      }
    }

    //test2: set machine to valid state of running and pass
    state = MachineState.Running
    vm.connect(mockSshSession, 2)


    //test3: have mock pause longer than timeout so fails
    mockSshSession = new MockSshSession(3)
    try {
      vm.connect(mockSshSession, 2)
      assert(false, "should have got ConnectException")
    }
    catch {
      case t: ConnectException => {
        assert(t.getCause != null, "expected")
      }
      case t: Throwable => {
        assert(false, "unexpected exception " + t)
      }
    }


    //test4: make mock pause less that timeout so pass
    mockSshSession = new MockSshSession(1)
    vm.connect(mockSshSession, 2)


  }

  def testIsInitialized() = {

    val lc: LaunchConfiguration = new LaunchConfiguration(null)
    lc.key = "some"
    var mockSshSession = new MockSshSession(3)

    val vm = new MockVM(2, lc, new MockCloud(0)) {
      override def getState() = {
        MachineState.Running
      }

      override protected def createSshSession = {
        mockSshSession
      }
    }

    assert(vm.isInitialized() == false, "expect to be not initialized when couldn't connect because mock delay is longer than timeout")

    mockSshSession = new MockSshSession(1)
    assert(vm.isInitialized() == true, "expect to be initialized after connect")

  }


}