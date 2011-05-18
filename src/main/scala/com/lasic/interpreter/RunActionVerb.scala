package com.lasic.interpreter

import collection.immutable.List
import com.lasic.model._
import com.lasic.util.Logging
import com.lasic.concurrent.ops._
import com.lasic.cloud._
import com.lasic.LasicProperties
import com.lasic.util._

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

  private[interpreter] var scaleGroupsToDelete = List[String]()
  private[interpreter] var configsToDelete = List[String]()

  private val sleepDelay = LasicProperties.getProperty("SLEEP_DELAY", "10000").toInt


  private def setVMs() {
    nodes.foreach {
      node => node.vm = VerbUtil.setVM(cloud, node)
    }
    scaleGroups foreach {
      scaleGroupInst =>
        spawn ("find bound scalegroups and launch new VM") {
          val scalingGroup = cloud.getScalingGroupClient
          val origConfig = scalingGroup.getScalingLaunchConfiguration(scaleGroupInst.configuration.cloudName)
          val newLaunchConfig: LaunchConfiguration = LaunchConfiguration.build(scaleGroupInst.configuration)
          newLaunchConfig.machineImage = origConfig.machineImage
          scaleGroupInst.vm = cloud.createVM(newLaunchConfig, true)
        }
    }
  }

  private def waitForAction {
    VerbUtil.waitForVMState(nodes ::: scaleGroups, {vmHolder => !(vmState(vmHolder).ipsComplete)}, "Waiting for action to finish: ")
  }

  private def waitToCreatePrototypeVMsForScaleGroup {
    VerbUtil.waitForVMState(scaleGroups,
      {vmHolder => vmHolder.vm == null || !vmHolder.vm.isInitialized},
      "Waiting to create prototype VMs for scalegroups: ")
  }

  private def waitForNewScaleGroups {
    VerbUtil.waitForVMState(scaleGroups, {vmHolder => vmHolder.vm.getMachineState != MachineState.ShuttingDown && vmHolder.vm.getMachineState != MachineState.Terminated}, "Waiting for new Scale Groups to come up: ")
  }

  private[interpreter] def deleteOldScaleGroups {
    val scalingGroup = cloud.getScalingGroupClient

    //set the scale group size to 0 so all instances are down so that it can be deleted.
    scaleGroupsToDelete foreach {
      scaleGroupName =>
        scalingGroup.updateScalingGroup(scaleGroupName, 0, 0)
    }

    VerbUtil.waitForScaleGroupsToTerminateInstances(cloud, scaleGroupsToDelete)

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
      PrintLine("/**scale group paths were modified**/")
      PrintLine("paths {")
      scaleGroups foreach {
        scaleGroup =>
          PrintLine("    " + scaleGroup.path + ": \"" + scaleGroup.cloudName + "\"")
          PrintLine("    " + scaleGroup.configuration.path + ": \"" + scaleGroup.configuration.cloudName + "\"")
      }
    }
    PrintLine("}")
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
    waitToCreatePrototypeVMsForScaleGroup
    saveOldScaleGroupAndConfig
    setScaleGroupNames
    startAsyncRunAction(actionName)
    waitForAction
    createScaleGroups(cloud.getScalingGroupClient)
    waitForNewScaleGroups
    deleteOldScaleGroups
    waitForElasticIpDnsChange(actionName)
    printBoundLasicProgram
  }

}
