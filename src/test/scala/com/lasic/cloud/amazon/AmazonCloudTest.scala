package com.lasic.cloud.amazon

import com.lasic.VM
import junit.framework.TestCase
import com.lasic.cloud.{MachineState, LaunchConfiguration, AmazonCloud}

/**
 * User: pughbc
 * Date: May 12, 2010
 * Time: 1:13:40 PM
 */

class AmazonCloudTest extends TestCase("AmazonCloudTest") {
  def testCloud() : Unit = { //disable test as it requires real keys and creates real instances
    if (false) {
      val cloud = new AmazonCloud()
      val lc: LaunchConfiguration = new LaunchConfiguration()
      lc.machineImage = "ami-714ba518" //base ubuntu image
      val vm: VM = new AmazonVM(cloud, lc)
      val vms = Array(vm)
      cloud.start(vms)
      //waitForVMToStart(vm)
      //testCopyTo(vm)
      Thread.sleep(20000); //give it a minute to come up
      cloud.terminate(vms)
    }

    def testCopyTo(vm: VM) = {
     //todo: test that an scp actually works and sends a file to the remote machine
    }

    

    def waitForVMToStart(vm: VM) {
      val startTime = System.currentTimeMillis
      var timedOut = false
      while( !(vm.getState == MachineState.Running) && !timedOut)  {
        Thread.sleep(1000)
        if ((System.currentTimeMillis - startTime) > 120000){
          timedOut = true
        }
      }
    }
    null
  }
}