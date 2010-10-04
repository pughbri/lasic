package com.lasic.interpreter

import com.lasic.util.Logging
import com.lasic.model.{NodeInstance, ScaleGroupInstance, LasicProgram}
import com.lasic.concurrent.ops._
import com.lasic.cloud.{LaunchConfiguration, MachineState, Cloud}

/**
 *
 * Shutdown a system.
 * @author Brian Pugh
 */
class ShutdownVerb(val cloud: Cloud, val program: LasicProgram) extends Verb with Logging {
  protected val nodes: List[NodeInstance] = program.find("//node[*][*]").map(_.asInstanceOf[NodeInstance])
  protected val scaleGroups: List[ScaleGroupInstance] = program.find("//scale-group[*]").map(_.asInstanceOf[ScaleGroupInstance])

  def shutdownScaleGroups() {
    val scaleGroupManager = cloud.getScalingGroup
    scaleGroups.foreach {
      scaleGroup =>
        spawn ("shutdown scale groups") {
          scaleGroupManager.updateScalingGroup(scaleGroup.cloudName, 0, 0)
        }
    }

    VerbUtil.waitForScaleGroupsToTerminateInstances(cloud, scaleGroups.map(_.cloudName))

    scaleGroups foreach {
      scaleGroup =>
        scaleGroupManager.deleteScalingGroup(scaleGroup.cloudName)
        scaleGroupManager.deleteLaunchConfiguration(scaleGroup.configuration.cloudName)
    }
  }

  def shutdownInstances() {
    nodes foreach {
      node =>
        spawn ("shutdown instances") {
          node.vm.shutdown
        }
    }
  }

  def waitForInstancesToTerminate() {
    VerbUtil.waitForVMState(nodes, {
      vmHolder =>
        vmHolder.vm != null && vmHolder.vm.getMachineState != MachineState.Terminated
    }, "waiting for instances to terminate")
  }


  def setVMs() {
    nodes.foreach {
      node =>
        spawn ("find instances for nodes") {
          node.vm = VerbUtil.setVM(cloud, LaunchConfiguration.build(node), node.boundInstanceId)
        }
    }
  }

  def waitForVMsToBeSet() {
    VerbUtil.waitForVMState(nodes,
      {vmHolder => vmHolder.vm == null},
      "Waiting to find node VM instances: ")
  }

  def doit = {
    setVMs
    waitForVMsToBeSet
    shutdownInstances
    shutdownScaleGroups
    waitForInstancesToTerminate
  }
}