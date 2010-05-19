package com.lasic.cloud.amazon

import junit.framework.TestCase
import java.lang.String
import java.io.File
import com.lasic.cloud.{MockCloud, MachineState, LaunchConfiguration}
import com.lasic.cloud.ssh.{ConnectException, SshSession}

/**
 *
 * User: Brian Pugh
 * Date: May 17, 2010
 */

class AmazonVMTest extends TestCase("AmazonCloudTest") {
  //setup
  val sourceFileURL = classOf[Application].getResource("/lasic.properties")
  val sourceFile: File = new File(sourceFileURL.toURI)
  val remoteFile: String = "/some/path/and/file.txt"

  class MockSshSession(numTimesToFailOnConnect: Int) extends SshSession {
    def this() = this (0)
    
    var currentNumFailures = 0

    override def sendFile(f: File, remoteFileName: String) = {
      assert(sourceFile == f)
      assert(remoteFileName.eq(remoteFile))
      1
    }

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

  def testCopyTo() = {


    val lc: LaunchConfiguration = new LaunchConfiguration()
    lc.key = "some"
    var state = MachineState.Unknown
    var mockSshSession = new MockSshSession
    val vm: AmazonVM = new AmazonVM(new MockCloud(0), lc, 2) {
      override protected def createSshSession = {
        mockSshSession
      }


      override def getState() = {
        state
      }
    }

    vm.baseLasicDir = sourceFile.getParent


    //test1: fail on non-initalized vm
    try {
      vm.copyTo(sourceFile, remoteFile)
      assert(false, "should have got IllegalStateException: VM hasn't been initialized")
    }
    catch {
      case t: IllegalStateException => { //expected
      }
      case t: Throwable => {
        assert(false, "unexpected exception " + t)
      }
    }

    //test2: set machine to valid state of running
    state = MachineState.Running
    vm.copyTo(sourceFile, remoteFile)


    //test3: fail too many times
    mockSshSession = new MockSshSession(3)
    try {
      vm.copyTo(sourceFile, remoteFile)
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


    //test4: fail only twice, then succeed
    mockSshSession = new MockSshSession(1)
    vm.copyTo(sourceFile, remoteFile)


  }


}