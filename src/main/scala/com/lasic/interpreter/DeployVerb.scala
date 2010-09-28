package com.lasic.interpreter

import com.lasic.cloud.{VM, Cloud}
import collection.immutable.List
import com.lasic.cloud.VolumeState._
import com.lasic.cloud.MachineState._
import com.lasic.model._
import com.lasic.interpreter.VerbUtil._
import com.lasic.util.Logging
import concurrent.ops._
import com.lasic.cloud._
import com.lasic.LasicProperties


/**
 * Launches VM, attaches volumes, runs the "install" action for each one, and brings up scale groups.
 */
class DeployVerb(val cloud: Cloud, val program: LasicProgram) extends Verb with Logging with ActionRunnerUtil {
  protected val nodes: List[NodeInstance] = program.find("//node[*][*]").map(_.asInstanceOf[NodeInstance])
  protected val scaleGroups: List[ScaleGroupInstance] = program.find("//scale-group[*]").map(_.asInstanceOf[ScaleGroupInstance])
  protected val vmState: Map[VMHolder, VMState] = {
    Map.empty ++ (nodes.map {node => (node, new VMState)} ::: scaleGroups.map {scaleGroup => (scaleGroup, new VMState)})
  }
  private var volumes: List[VolumeInstance] = nodes.map(_.volumes).flatten

  private val sleepDelay = LasicProperties.getProperty("SLEEP_DELAY", "10000").toInt

  private def validateProgram {}

  private def launchAllAMIs {
    nodes.foreach {
      node =>
        spawn {
          node.vm = cloud.createVM(LaunchConfiguration.build(node), true)
          logger.debug("created instance for node: " + node)
        }
    }
    scaleGroups.foreach {
      scaleGroup =>
        spawn {
          scaleGroup.vm = cloud.createVM(LaunchConfiguration.build(scaleGroup.configuration), true)
        }
    }
  }


  private def createAllVolumes {
    volumes.foreach(volInst =>
      spawn {
        val volume = cloud.createVolume(VolumeConfiguration.build(volInst))
        volInst.volume = volume
      }
      )
  }


  private def waitForAMIsToBoot {
    waitForVMState({vmHolder => vmHolder.vm == null || !vmHolder.vm.isInitialized}, "Waiting for machines to boot: ")
    logger.info("Booted IDs are: " + nodes.map(t => showValue(t.vmId) + ":" + showValue(t.vmState)))
  }

  private def waitForScaleGroupsConfigured() {
    waitForVMState(scaleGroups, {vmHolder => vmHolder.vm.getMachineState != MachineState.ShuttingDown && vmHolder.vm.getMachineState != MachineState.Terminated}, "Waiting for Scale Groups to come up: ")
  }

  private def waitForVMState(state: MachineState, statusString: String) {
    waitForVMState({vmHolder => vmHolder.vm == null || vmHolder.vm.getMachineState != state}, statusString)
  }

  private def waitForActionItems {
    waitForVMState({vmHolder => !(vmState(vmHolder).ipsComplete)}, "Waiting for action items to run: ")
  }

  private def waitForVMState(test: VMHolder => Boolean, statusString: String) {
    waitForVMState(nodes ::: scaleGroups, test, statusString)
  }

  private def waitForVMState(vmHolders: List[VMHolder], test: VMHolder => Boolean, statusString: String) {
    var waiting = vmHolders.filter(t => test(t))
    while (waiting.size > 0) {
      val descriptions: List[String] = waiting.map(t => t.vmId + ":" + t.vmState)
      logger.info(statusString + descriptions)
      Thread.sleep(sleepDelay)
      waiting = vmHolders.filter(t => test(t))
    }
  }


  private def waitForVolumeState(state: VolumeState, statusString: String) {
    var waiting = volumes.filter(v => v.volume.info.state != state)
    while (waiting.size > 0) {
      val descriptions: List[String] = waiting.map(v => v.volume.id + ":" + v.volume.info.state)
      logger.info(statusString + descriptions)
      Thread.sleep(sleepDelay)
      waiting = volumes.filter(v => v.volume.info.state != state)
    }
  }

  private def waitForVolumes {
    waitForVolumeState(VolumeState.Available, "Waiting for volumes to be created: ")
  }

  private def attachAllVolumes {
    volumes.foreach(
      volInst => {
        volInst.volume.attachTo(volInst.parentNodeInstance.vm, volInst.device)
      }
      )
  }

  private def waitForVolumesToAttach {
    waitForVolumeState(VolumeState.InUse, "Waiting for volumes to attach: ")
  }


  private def printBoundLasicProgram {
    println("paths {")
    nodes.foreach({
      node => println("    " + node.path + ": \"" + node.vmId + "\"  // public=" + showValue(node.vmPublicDns) + "\tprivate=" + showValue(node.vmPrivateDns))
    })
    scaleGroups foreach {
      scaleGroup =>
        println("    " + scaleGroup.path + ": \"" + scaleGroup.cloudName + "\"")
        println("    " + scaleGroup.configuration.path + ": \"" + scaleGroup.configuration.cloudName + "\"")
    }
    volumes foreach {
      volumeInst => println("    " + volumeInst.path + ": \"" + volumeInst.volume.id + "\"")
    }
    println("}")
  }

  def doit() {

    // Error checks before doing anything
    validateProgram

    // Startup everything that needs it
    launchAllAMIs
    createAllVolumes
    setScaleGroupNames


    // Wait for all resources to be created before proceeding
    waitForAMIsToBoot

    //assignPrivateDNS2Nodes

    waitForVolumes

    // Attach all volumes in preparation for setup
    attachAllVolumes
    waitForVolumesToAttach

    // Configure all the nodes
    startAsyncRunAction("install")

    // Wait for all nodes to be configured
    waitForActionItems

    createScaleGroups(cloud.getScalingGroup)

    // Wait for scale groups to be configured
    waitForScaleGroupsConfigured

    waitForElasticIpDnsChange("install")    

    // Print out the bound program so the user can see the IDs we are manipulating
    printBoundLasicProgram
  }
}