package com.lasic.cloud

import junit.framework.TestCase
import mock.{MockCloud, MockVM}
import ssh.{ConnectException, SshSession}
import java.io.File
import com.lasic.{Cloud, VM}
import collection.immutable.{List, Map}
import java.lang.String

/**
 *
 * User: Brian Pugh
 * Date: May 25, 2010
 */

class VMTest extends TestCase("VMTest") {
  //setup

  class MockSshSession(numTimesToFailOnConnect: Int) extends SshSession("dns", "uname", new File("")) {
    def this() = this (0)

    var currentNumFailures = 0

    override def connect() = {
      if (numTimesToFailOnConnect > currentNumFailures) {
        currentNumFailures += 1
        throw new ConnectException("test failure:  " + currentNumFailures + " out of " + numTimesToFailOnConnect + " have occurred", new RuntimeException())
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
      override def getMachineState() = {
        state
      }
    }


    //test1: fail on non-initalized vm
    try {
      vm.connect(mockSshSession, 2)
      assert(false, "should have got IllegalStateException: VM hasn't been initialized")
    }
    catch {
      case t: IllegalArgumentException => { //expected
      }
      case t: Throwable => {
        assert(false, "unexpected exception " + t)
      }
    }

    //test2: set machine to valid state of running and pass
    state = MachineState.Running
    vm.connect(mockSshSession, 2)


    //test3: have mock just keep failing so it take more time than the timeout
    mockSshSession = new MockSshSession(1000)
    try {
      vm.connect(mockSshSession, 1)
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
    mockSshSession = new MockSshSession(0)
    vm.connect(mockSshSession, 2)


  }


  class InitializationMockVM(delay: Int, val launchConfiguration: LaunchConfiguration,val cloud: Cloud) extends VM {
    def executeScript(scriptAbsPath: String, variables: Map[String, List[String]]) = null
    def execute(executableAbsPath: String) = null
    def copyTo(sourceFile: File, destinationAbsPath: String) = null
  }

  def testIsInitialized() = {

    val lc: LaunchConfiguration = new LaunchConfiguration(null)
    lc.key = "some"
    var mockSshSession = new MockSshSession(3)

    val vm = new InitializationMockVM(2, lc, new MockCloud(0)) {
      override def getMachineState() = {
        MachineState.Running
      }

      override protected def createSshSession = {
        mockSshSession
      }
    }

    assert(vm.isInitialized() == false, "expect to be not initialized when couldn't connect because mock delay is longer than timeout")

    mockSshSession = new MockSshSession(0)
    assert(vm.isInitialized() == true, "expect to be initialized after connect")

  }


}