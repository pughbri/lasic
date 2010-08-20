package com.lasic.interpreter

import java.net.URI
import java.io.File
import com.lasic.{VM, Cloud}
import com.lasic.values.BaseAction
import collection.immutable.List
import java.util.Date
import com.lasic.cloud.VolumeState._
import com.lasic.cloud.MachineState._
import com.lasic.model._
import com.lasic.interpreter.VerbUtil._
import com.lasic.util.Logging
import se.scalablesolutions.akka.actor.Actor._
import com.lasic.cloud._

private class VMState() {
  var scpComplete = false
  var scriptsComplete = false
}

/**
 * Launches VM, attaches volumes, runs the "install" action for each one, and brings up scale groups.
 */
class DeployVerb2(val cloud: Cloud, val program: LasicProgram) extends Verb with Logging {
  private val nodes: List[NodeInstance] = program.find("//node[*][*]").map(_.asInstanceOf[NodeInstance])
  private val scaleGroups: List[ScaleGroupInstance] = program.find("//scale-group[*]").map(_.asInstanceOf[ScaleGroupInstance])
  private val vmState: Map[VMHolder, VMState] = {
    Map.empty ++ (nodes.map {node => (node, new VMState)} ::: scaleGroups.map {scaleGroup => (scaleGroup, new VMState)})
  }
  private var volumes: List[VolumeInstance] = nodes.map(_.volumes).flatten


  private def validateProgram {}

  private def launchAllAMIs {
    nodes.foreach {
      node =>
        spawn {
          node.vm = cloud.createVM(LaunchConfiguration.build(node), true)
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

  private def waitForVMScripts {
    waitForVMState({vmHolder => !(vmState(vmHolder).scriptsComplete)}, "Waiting for scripts to run: ")
  }

  private def waitForVMState(test: VMHolder => Boolean, statusString: String) {
    waitForVMState(nodes ::: scaleGroups, test, statusString)
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


  private def waitForVolumeState(state: VolumeState, statusString: String) {
    var waiting = volumes.filter(v => v.volume.info.state != state)
    while (waiting.size > 0) {
      val descriptions: List[String] = waiting.map(v => v.volume.id + ":" + v.volume.info.state)
      logger.info(statusString + descriptions)
      Thread.sleep(10000)
      waiting = volumes.filter(v => v.volume.info.state != state)
    }
  }

  private def waitForVolumes {
    waitForVolumeState(VolumeState.Available, "Waiting for volumes to be created: ")
  }

  def build(uri: URI): File = {
    if (uri.isOpaque) new File(uri.toString.split(":")(1)) else new File(uri)
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

  def copyAndRunScripts(vmHolder: VMHolder, allSCPs: Map[String, String], resolvedScripts: Map[String, Map[String, scala.List[String]]]): Unit = {
    spawn {
      allSCPs.foreach {
        tuple => vmHolder.vm.copyTo(build(new URI(tuple._1)), tuple._2)
      }
      vmState.synchronized(
        vmState(vmHolder).scpComplete = true
        )
      resolvedScripts.foreach {
        script =>
          val scriptName = script._1
          val argMap = script._2
          //vm.execute(scriptName)
          vmHolder.vm.executeScript(scriptName, argMap)
      }
      vmState.synchronized(
        vmState(vmHolder).scriptsComplete = true
        )
    }
  }

  def getSCPAndScriptMaps(allActions: List[BaseAction]): (Map[String, Map[String, ScriptArgumentValue]], Map[String, String]) = {
    val deployActions = allActions.filter(_.name == "install")
    var allSCPs = Map[String, String]()
    var allScripts = Map[String, Map[String, ScriptArgumentValue]]()

    deployActions.foreach {
      action => {
        allSCPs = allSCPs ++ action.scpMap
        allScripts = allScripts ++ action.scriptMap
      }
    }
    (allScripts, allSCPs)
  }

  private def startAsyncNodeConfigure {
    nodes.foreach {
      node =>
        val allActions = node.parent.actions
        val (allScripts, allSCPs) = getSCPAndScriptMaps(allActions)
        var resolvedScripts = node.resolveScripts(allScripts)
        copyAndRunScripts(node, allSCPs, resolvedScripts)
    }

    scaleGroups.foreach {
      scaleGroup =>
        val allActions = scaleGroup.actions
        val (allScripts, allSCPs) = getSCPAndScriptMaps(allActions)
        var resolvedScripts = scaleGroup.resolveScripts(allScripts)
        copyAndRunScripts(scaleGroup, allSCPs, resolvedScripts)
    }
  }

  private def createScaleGroups() {
    scaleGroups.foreach {
      scaleGroupInstance =>
        spawn {
          val scaleGroup = cloud.getScalingGroup()

          //create unique names
          val dateString = new java.text.SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date())
          scaleGroupInstance.cloudName = scaleGroupInstance.localName + "-" + dateString
          val scaleGroupConfig = scaleGroupInstance.configuration
          scaleGroupConfig.cloudName = scaleGroupConfig.name + "-" + dateString

          //create the image
          val imageID = scaleGroup.createImageForScaleGroup(scaleGroupInstance.vm.instanceId, scaleGroupInstance.cloudName, "Created by LASIC on " + dateString, false)

          //create the config
          val launchConfiguration = LaunchConfiguration.build(scaleGroupInstance.configuration)
          launchConfiguration.machineImage = imageID
          launchConfiguration.name = scaleGroupConfig.cloudName
          scaleGroup.createScalingLaunchConfiguration(launchConfiguration)

          //create the group
          scaleGroup.createScalingGroup(scaleGroupInstance.cloudName, scaleGroupConfig.cloudName, scaleGroupConfig.minSize, scaleGroupConfig.maxSize, List(launchConfiguration.availabilityZone))

          //create the triggers
          scaleGroupInstance.triggers.foreach {
            trigger =>
              val scalingTrigger = new ScalingTrigger(scaleGroupInstance.cloudName,
                trigger.breachDuration,
                trigger.lowerBreachIncrement.toString,
                trigger.lowerThreshold,
                trigger.measure,
                trigger.name,
                trigger.period,
                trigger.upperBreachIncrement.toString,
                trigger.upperThreshold)
              scaleGroup.createUpdateScalingTrigger(scalingTrigger)
          }

          //terminate the original vm
          scaleGroupInstance.vm.shutdown
        }
    }

  }

  private def printBoundLasicProgram {
    println("paths {")
    nodes.foreach({
      node => println("    " + node.path + ": \"" + node.vmId + "\"  // public=" + showValue(node.vmPublicDns) + "\tprivate=" + showValue(node.vmPrivateDns))
    })
    scaleGroups foreach {
      scaleGroup =>
        println("    " + scaleGroup.path + ": \"" + scaleGroup.cloudName) + "\""
        println("    " + scaleGroup.configuration.path + ": \"" + scaleGroup.configuration.cloudName) + "\""
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


    // Wait for all resources to be created before proceeding
    waitForAMIsToBoot

    //assignPrivateDNS2Nodes

    waitForVolumes

    // Attach all volumes in preparation for setup
    attachAllVolumes
    waitForVolumesToAttach

    // Configure all the nodes
    startAsyncNodeConfigure

    // Wait for all nodes to be configured
    waitForVMScripts

    createScaleGroups

    // Wait for scale groups to be configured
    waitForScaleGroupsConfigured

    // Print out the bound program so the user can see the IDs we are manipulating
    printBoundLasicProgram
  }
}