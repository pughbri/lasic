package com.lasic.interpreter

import com.lasic.cloud.{VM, Cloud}
import collection.immutable.List
import com.lasic.model._
import com.lasic.util.Logging
import se.scalablesolutions.akka.actor.Actor._
import com.lasic.cloud._

/**
 *
 * @author Brian Pugh
 */

class RunActionVerb(val actionName: String, val cloud: Cloud, val program: LasicProgram) extends Verb with Logging with ActionRunnerUtil {
  protected val nodes: List[NodeInstance] = program.find("//node[*][*]").map(x => x.asInstanceOf[NodeInstance])
  protected val scaleGroups: List[ScaleGroupInstance] = program.find("//scale-group[*]").map(_.asInstanceOf[ScaleGroupInstance])
  protected val vmState: Map[VMHolder, VMState] = {
    Map.empty ++ (nodes.map {node => (node, new VMState)} ::: scaleGroups.map {scaleGroup => (scaleGroup, new VMState)})
  }

  private var scaleGroupsToDelete = List[String]()
  private var configsToDelete = List[String]()


  private def setVMs() {
    nodes.foreach {
      node =>
        spawn {
          node.vm = setVM(LaunchConfiguration.build(node), node.boundInstanceId)
        }
    }
    scaleGroups foreach {
      scaleGroupInst =>
        spawn {
          val scalingGroup = cloud.getScalingGroup
          val origConfig = scalingGroup.getScalingLaunchConfiguration(scaleGroupInst.configuration.cloudName)
          val newLaunchConfig: LaunchConfiguration = LaunchConfiguration.build(scaleGroupInst.configuration)
          newLaunchConfig.machineImage = origConfig.machineImage
          scaleGroupInst.vm = cloud.createVM(newLaunchConfig, true)
        }
    }
  }

  private def setVM(lc: LaunchConfiguration, instanceId: String): VM = {
    val vm = cloud.findVM(instanceId)
    if (vm.launchConfiguration != null) {
      vm.launchConfiguration.name = lc.name
      vm.launchConfiguration.userName = lc.userName
      vm.launchConfiguration.key = lc.key
    }
    vm
  }

  private def waitForVMState(vmHolders: List[VMHolder], test: VMHolder => Boolean, statusString: String) {
    var waiting = vmHolders.filter(t => test(t))
    while (waiting.size > 0) {
      val descriptions: List[String] = waiting.map(t => t.vmId + ":" + t.vmState)
      logger.info(statusString + descriptions)
      Thread.sleep(10000)
      waiting = vmHolders.filter(t => test(t))
    }
  }

  private def waitForAction {
    waitForVMState(nodes ::: scaleGroups, {vmHolder => !(vmState(vmHolder).ipsComplete)}, "Waiting for action to finish: ")
  }

  private def waitVMsToBeSet {
    waitForVMState(scaleGroups ::: nodes,
      {vmHolder => vmHolder.vm == null || !vmHolder.vm.isInitialized},
      "Waiting to find node VM instances and create prototype VMs for scalegroups: ")
  }

  private def waitForNewScaleGroups {
    waitForVMState(scaleGroups, {vmHolder => vmHolder.vm.getMachineState != MachineState.ShuttingDown && vmHolder.vm.getMachineState != MachineState.Terminated}, "Waiting for new Scale Groups to come up: ")
  }

  private def deleteOldScaleGroups {
    val scalingGroup = cloud.getScalingGroup
    scaleGroupsToDelete foreach {
      scaleGroupName =>
        scalingGroup.deleteScalingGroup(scaleGroupName)
    }

    configsToDelete foreach {
      configName =>
        scalingGroup.deleteLaunchConfiguration(configName)
    }
  }

  private def printBoundLasicProgram {
    if (!scaleGroups.isEmpty) {
      println("/**scale group paths were modified**/")
      println("paths {")
      scaleGroups foreach {
        scaleGroup =>
          println("    " + scaleGroup.path + ": \"" + scaleGroup.cloudName + "\"")
          println("    " + scaleGroup.configuration.path + ": \"" + scaleGroup.configuration.cloudName + "\"")
      }
    }
    println("}")
  }

  private def saveOldScaleGroupAndConfig {
    scaleGroups foreach {
      scaleGroupInst =>
        if (scaleGroupInst.cloudName != null && scaleGroupInst.cloudName != "") {
          scaleGroupsToDelete = scaleGroupInst.cloudName :: scaleGroupsToDelete
        }
        if (scaleGroupInst.configuration.cloudName != null && scaleGroupInst.configuration.cloudName != "") {
          configsToDelete = scaleGroupInst.configuration.cloudName :: configsToDelete
        }
    }
  }

  def doit = {
    setVMs
    waitVMsToBeSet
    startAsyncRunAction(actionName)
    waitForAction
    saveOldScaleGroupAndConfig
    createScaleGroups(cloud.getScalingGroup)
    waitForNewScaleGroups
    deleteOldScaleGroups
    printBoundLasicProgram
  }

}
