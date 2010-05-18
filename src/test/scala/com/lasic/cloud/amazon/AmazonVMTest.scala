package com.lasic.cloud.amazon

import junit.framework.TestCase
import java.lang.String
import java.io.File
import com.lasic.cloud.ssh.SshSession
import com.lasic.cloud.{MockCloud, MachineState, LaunchConfiguration}

/**
 *
 * User: Brian Pugh
 * Date: May 17, 2010
 */

class AmazonVMTest extends TestCase("AmazonCloudTest") {
  def testCopyTo() = {

    //setup
    val sourceFileURL = classOf[Application].getResource("/lasic.properties")
    val sourceFile: File = new File(sourceFileURL.toURI)
    val remoteFile: String = "/some/path/and/file.txt"

    class MockSshSession extends SshSession {
      override def sendFile(f: File, remoteFileName: String) = {
        assert(sourceFile == f)
        assert(remoteFileName.eq(remoteFile))
        1
      }

      override def connect(dnsName: String, userName: String, pemFile: File) = {true}

      override def disconnect = {}
    }


    val lc: LaunchConfiguration = new LaunchConfiguration()
    lc.key = "some"
    var state = MachineState.Unknown
    val vm: AmazonVM = new AmazonVM(new MockCloud(0), lc) {
      override protected def createSshSession = {
        new MockSshSession
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
  }


}