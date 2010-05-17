package com.lasic.cloud.amazon

import junit.framework.TestCase
import java.lang.String
import java.io.File
import com.lasic.cloud.ssh.SshSession
import com.lasic.cloud.{MachineState, MachineDescription, LaunchConfiguration}

/**
 *
 * User: Brian Pugh
 * Date: May 17, 2010
 */

class AmazonVMTest extends TestCase("AmazonCloudTest") {
  def testCopyTo() = {

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
    lc.machineImage = "ami-714ba518" //base ubuntu image
    lc.key = "some"
    val vm: AmazonVM = new AmazonVM(null, lc) {
      override protected def createSshSession = {
        new MockSshSession
      }
    }

    vm.baseLasicDir = sourceFile.getParent

    try {
      vm.copyTo(sourceFile, remoteFile)
      assert(false, "should have got IllegalStateException")
    }
    catch {
      case t: IllegalStateException => { //expected
      }
      case t: Throwable => {
        assert(false, "unexpected exception " + t)
      }
    }

    vm.machineDescription = new MachineDescription("id", MachineState.Running, "my-machine", "my-private-machine")
    vm.copyTo(sourceFile, remoteFile)


  }


}