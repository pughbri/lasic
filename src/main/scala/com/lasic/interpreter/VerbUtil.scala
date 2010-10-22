package com.lasic.interpreter

import com.lasic.util.Logging
import com.lasic.LasicProperties
import com.lasic.cloud.{VM, LaunchConfiguration, Cloud}
import com.lasic.model.{NodeInstance, VMHolder}

/**
 *
 * @author Brian Pugh
 */

object VerbUtil extends Logging {
  private val sleepDelay = LasicProperties.getProperty("SLEEP_DELAY", "10000").toInt

  def showValue(x: Any) = x match {
    case Some(s) => s
    case None => "?"
    case y => y
  }


  def waitForScaleGroupsToTerminateInstances(cloud: Cloud, scaleGroups: List[String]) {
    val scalingGroup = cloud.getScalingGroupClient
    var scaleGroupsStillTerminating = scala.collection.immutable.List[String]() ::: scaleGroups
    while (scaleGroupsStillTerminating.size > 0) {
      logger.info("waiting for scale groups to terminate instances: " + scaleGroupsStillTerminating)
      Thread.sleep(sleepDelay)

      scaleGroupsStillTerminating = scaleGroupsStillTerminating.filter(
        name => {
          //TODO: check "describeScalingActivites" instead of instances
          val groupInfo = scalingGroup.describeAutoScalingGroup(name)
          require(groupInfo.maxSize == 0,
            "max size should be 0 for a scale group being deleted.  It is " + groupInfo.maxSize + " for " + name)
          val instances = groupInfo.instances
          instances != null && instances.size > 0
        })

    }
  }


  def waitForVMState(vmHolders: List[VMHolder], test: VMHolder => Boolean, statusString: String) {
    var waiting = vmHolders.filter(t => test(t))
    while (waiting.size > 0) {
      val descriptions: List[String] = waiting.map(t => t.vmId + ":" + t.vmState)
      logger.info(statusString + descriptions)
      Thread.sleep(sleepDelay)
      waiting = vmHolders.filter(t => test(t))
    }
  }


  def setVM(cloud: Cloud, node: NodeInstance): VM = {
    if (node.boundInstanceId == null) {
      logger.warn("Cannot set vm for node [" + node.path + "] because it is not bound to an instance id")
      null
    }
    else {
      val lc = LaunchConfiguration.build(node)
      val vm = cloud.findVM(node.boundInstanceId)
      if (vm.launchConfiguration != null) {
        vm.launchConfiguration.name = lc.name
        vm.launchConfiguration.userName = lc.userName
        vm.launchConfiguration.key = lc.key
      }
      vm
    }
  }

}