package com.lasic.cloud.amazon

import com.lasic.VM
import junit.framework.TestCase
import com.lasic.cloud.{MachineState, LaunchConfiguration, AmazonCloud}
import java.io.File

/**
 * User: pughbc
 * Date: May 12, 2010
 * Time: 1:13:40 PM
 */

class AmazonCloudTest extends TestCase("AmazonCloudTest") {
  def testCloud(): Unit = { //disable test as it requires real keys and creates real instances
    if (false) {
      val cloud = new AmazonCloud()
      val lc: LaunchConfiguration = new LaunchConfiguration()
      lc.machineImage = "ami-714ba518" //base ubuntu image
      lc.key = "default"
      lc.userName = "ubuntu"
      val vm: VM = new AmazonVM(cloud, lc)
      vm.baseLasicDir = System.getProperty("user.home") + "/ec2-keys"
      val vms = Array(vm)
      cloud.start(vms)
      waitForVMToStart(vm)
      testCopyTo(vm)

      cloud.terminate(vms)
    }

    def testCopyTo(vm: VM) = {
      val sourceFileURL = classOf[Application].getResource("/lasic.properties")
      val sourceFile: File = new File(sourceFileURL.toURI)
      vm.copyTo(sourceFile, "/tmp/text.txt")
    }



    def waitForVMToStart(vm: VM) {
      val startTime = System.currentTimeMillis
      var timedOut = false
      while (!(vm.getState == MachineState.Running) && !timedOut) {
        Thread.sleep(1000)
        if ((System.currentTimeMillis - startTime) > 120000) {
          timedOut = true
        }
      }
      Thread.sleep(10000); //give it 10 more seconds for the ssh daemon to come up
    }
    null
  }
}