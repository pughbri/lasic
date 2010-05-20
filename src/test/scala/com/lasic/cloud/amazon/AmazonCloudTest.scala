package com.lasic.cloud.amazon

import junit.framework.TestCase
import java.io.File
import com.lasic.{Cloud, VM}
import com.lasic.cloud.{AttachmentInfo, MachineState, LaunchConfiguration, AmazonCloud}
import java.util.Calendar

/**
 * User: pughbc
 * Date: May 12, 2010
 * Time: 1:13:40 PM
 */

class AmazonCloudTest extends TestCase("AmazonCloudTest") {

//  def testCreateVolume(): Unit = {
//    val cloud = new AmazonCloud()
//    cloud.createVolume(1, "snap-lasictest", "us-east-1d")
//  }



  def testCloud(): Unit = {
    if (false) { //disable test as it requires real keys and creates real instances
      val cloud = new AmazonCloud()
      val lc: LaunchConfiguration = new LaunchConfiguration()
      lc.machineImage = "ami-714ba518" //base ubuntu image
      lc.key = "default"
      lc.userName = "ubuntu"
      val vm: VM = new AmazonVM(cloud, lc)
      vm.baseLasicDir = System.getProperty("user.home") + "/ec2-keys"
      val vms = Array(vm)
      cloud.start(vms)
      try {
        waitForVMToStart(vm)
        testCopyTo(vm)
        testExecute(vm)
        testCreateAndMountVolume(cloud, vm)
      }
      finally {
        cloud.terminate(vms)
      }
    }

    def testCopyTo(vm: VM) = {
      val sourceFileURL = classOf[Application].getResource("/lasic2.properties")
      val sourceFile: File = new File(sourceFileURL.toURI)
      vm.copyTo(sourceFile, "/tmp/test.txt")
    }

    def testExecute(vm: VM) = {
      vm.execute("cp /tmp/test.txt /tmp/test2.txt")
    }


    def testCreateAndMountVolume(cloud: Cloud, vm: VM) = {
      val volumeInfo = cloud.createVolume(1, "", "us-east-1d")
      val attachmentInfo = vm.attach(volumeInfo, "/dev/sdh")
      println(attachmentInfo)
    }

    def waitForVMToStart(vm: VM) {
      val startTime = System.currentTimeMillis
      var timedOut = false
      while (!(vm.getState == MachineState.Running) && !timedOut) {
        Thread.sleep(3000)
        if ((System.currentTimeMillis - startTime) > 120000) {
          timedOut = true
        }
      }
      Thread.sleep(20000); //give it 20 more seconds for the ssh daemon to come up
    }
    null
  }
  
}