package com.lasic.interpreter

import com.lasic.util.Logging
import com.lasic.concurrent.ops._
import com.lasic.cloud.{MachineState, Cloud}
import com.lasic.model.{LoadBalancerInstance, NodeInstance, ScaleGroupInstance, LasicProgram}

/**
 *
 * Shutdown a system.
 * @author Brian Pugh
 */
class ShutdownVerb(val cloud: Cloud, val program: LasicProgram) extends Verb with Logging {
  protected val nodes: List[NodeInstance] = program.find("//node[*][*]").map(_.asInstanceOf[NodeInstance])
  protected val scaleGroups: List[ScaleGroupInstance] = program.find("//scale-group[*]").map(_.asInstanceOf[ScaleGroupInstance])
  protected val loadBalancers: List[LoadBalancerInstance] = program.find("//load-balancer[*]").map(_.asInstanceOf[LoadBalancerInstance])

  def shutdownScaleGroups() {
    val scaleGroupManager = cloud.getScalingGroupClient
    scaleGroups foreach {
      scaleGroup =>
        spawn("shutdown scale groups") {
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
        spawn("shutdown instances") {
          if (node.vm != null) {
            node.vm.shutdown
          }
        }
    }
  }

  def shutdownLoadbalancers() {
    loadBalancers foreach {
      lb =>
        cloud.getLoadBalancerClient.deleteLoadBalancer(lb.cloudName)
    }
  }

  def waitForInstancesToTerminate() {
    VerbUtil.waitForVMState(nodes, {
      vmHolder =>
        vmHolder.vm != null && vmHolder.vm.getMachineState != MachineState.Terminated
    }, "waiting for instances to terminate: ")
  }


  def setVMs() {
    nodes.foreach {
      node => node.vm = VerbUtil.setVM(cloud, node)
    }
  }


  def doit = {
    setVMs
    shutdownInstances
    shutdownScaleGroups
    shutdownLoadbalancers
    waitForInstancesToTerminate
  }
}