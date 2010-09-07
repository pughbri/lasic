package com.lasic.cloud.amazon

import com.lasic.cloud.VM
import com.lasic.cloud._

/**
 *
 * @author Brian Pugh
 */

class AmazonScalingGroupTest extends AmazonBaseTest {
  def testCreateScaleGroup() {
    if (doActualCloudOperations) { //disable test as it requires real keys and creates real instances
      val cloud = new AmazonCloud()
      val lc: LaunchConfiguration = new LaunchConfiguration
      lc.machineImage = "ami-714ba518" //base ubuntu image
      lc.key = "default"
      lc.userName = "ubuntu"
      lc.groups = List("default")
      lc.name = "lctestname" + System.currentTimeMillis
      val vm: VM = cloud.createVM(lc, true );
      vm.baseLasicDir = System.getProperty("user.home") + "/ec2-keys"
//      val vms = List(vm)
//      cloud.start(vms)
      val scaleGroup = cloud.getScalingGroup
      val scaleGroupName = lc.name + "scalegroup"
      var imageId: String = null
      try {
        waitForVMToStart(vm)
        imageId = scaleGroup.createImageForScaleGroup(vm.instanceId, "imagetest" + System.currentTimeMillis, "test image creation", false)
        waitForImageToBeAvailable(scaleGroup, imageId)
        println(imageId)
        lc.machineImage = imageId
        scaleGroup.createScalingLaunchConfiguration(lc)
        Thread.sleep(2000)
        scaleGroup.createScalingGroup(scaleGroupName, lc.name, 1, 2, List(lc.availabilityZone))
        val trigger = new ScalingTrigger(scaleGroupName, 300, "1", 10, "CPUUtilization", "trigger" + System.currentTimeMillis, "AWS/EC2", 60, "1", 60)
        scaleGroup.createUpdateScalingTrigger(trigger)
      }
      finally {
        vm.shutdown
        //cloud.terminate(vms)
        scaleGroup.deleteScalingGroup(scaleGroupName)
        scaleGroup.deleteLaunchConfiguration(lc.name)
        if (imageId != null) {
          scaleGroup.deleteSnapshotAndDeRegisterImage(imageId)
        }
      }
    }
  }


  def waitForVMToStart(vm: VM) {
    val startTime = System.currentTimeMillis
    var timedOut = false
    while (!(vm.getMachineState == MachineState.Running) && !timedOut) {
      Thread.sleep(3000)
      if ((System.currentTimeMillis - startTime) > 120000) {
        timedOut = true
      }
    }
    Thread.sleep(30000); //give it 3
    // 0 more seconds for the ssh daemon to come up
  }

  def waitForImageToBeAvailable(scaleGroup: ScalingGroup, imageId: String) {
    val startTime = System.currentTimeMillis
    var timedOut = false
    while (!(scaleGroup.getImageState(imageId) == ImageState.Available) && !timedOut) {
      Thread.sleep(3000)
      if ((System.currentTimeMillis - startTime) > 120000) {
        timedOut = true
      }
    }
  }


}