package com.lasic.interpreter
/*
import actors._
import java.net.URI
import java.io.File
//import actors.VMActor.{MsgVMOperation, ConfigureData}
//import DeployActor._
//import DeployActor.DeployActorState._
import com.lasic.{Cloud}
import com.lasic.cloud.VolumeState._
//import se.scalablesolutions.akka.actor.{ActorRef, Actor}
import com.lasic.model._
import com.lasic.interpreter.VerbUtil._
import com.lasic.util.Logging
import se.scalablesolutions.akka.actor.Actor._
import com.lasic.cloud.{VolumeState, MachineState, Volume, VolumeConfiguration, LaunchConfiguration}
*/
import actors._
import java.net.URI
import java.io.File
//import actors.VMActor.{MsgVMOperation, ConfigureData}
import com.lasic.cloud.VolumeState._
import com.lasic.cloud.MachineState._
//import se.scalablesolutions.akka.actor.{ActorRef, Actor}
import com.lasic.model._
import com.lasic.interpreter.VerbUtil._
import com.lasic.util.Logging
import se.scalablesolutions.akka.actor.Actor._
//import java.io.File
//import java.net.URI
import com.lasic.cloud._
//import com.lasic.{VM, Cloud}
import com.lasic.{Cloud}

private class NodeState() {
  var scpComplete = false
  var scriptsComplete = false
}

class DeployVerb2(val cloud: Cloud, val program: LasicProgram) extends Verb with Logging {
  private val nodes: List[NodeInstance] = program.find("//node[*][*]").map(_.asInstanceOf[NodeInstance])
  private val scaleGroups: List[ScaleGroupInstance] = program.find("//scale-group[*]").map(_.asInstanceOf[ScaleGroupInstance])
  private val nodeState:Map[NodeInstance,NodeState] = Map.empty ++ nodes.map{ node => (node,new NodeState) }
  private var volumes: List[VolumeInstance] = nodes.map(_.volumes).flatten


  private def validateProgram {}

  private def launchAllAMIs {
    nodes.foreach {
      node =>
        spawn {
          node.vm = cloud.createVM(LaunchConfiguration.build(node), true)
        }
    }
  }

//  private def createScaleGroups {
//    scaleGroups.foreach {
//      scaleGroup =>
//        spawn {
//          scaleGroup.= cloud.createVM(LaunchConfiguration.build(node), true)
//        }
//    }
//  }

  private def createAllVolumes {
    volumes.foreach(volInst =>
      spawn {
        val volume = cloud.createVolume(VolumeConfiguration.build(volInst))
        volInst.volume = volume
      }
      )
  }


  private def waitForAMIsToBoot {
    waitForVMState({ni => ni.vm==null || !ni.vm.isInitialized}, "Waiting for machines to boot: ")
    logger.info("Booted IDs are: " + nodes.map(t => showValue(t.vmId) + ":" + showValue(t.vmState)))
  }

  private def waitForVMState( state:MachineState, statusString:String) {
    waitForVMState( {ni=>ni.vm==null || ni.vm.getMachineState!=state}, statusString)
  }

  private def waitForVMScripts {
    waitForVMState( {ni => !(nodeState(ni).scriptsComplete)}, "Waiting for scripts to run: " )
  }
  
  private def waitForVMState(test: NodeInstance => Boolean, statusString: String) {
    var waiting = nodes.filter(t => test(t))
    while (waiting.size > 0) {
      val descriptions: List[String] = waiting.map(t => t.vmId + ":" + t.vmState)
      logger.info(statusString + descriptions)
      Thread.sleep(10000)
      waiting = nodes.filter(t => test(t))
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

  private def startAsyncNodeConfigure {
    nodes.foreach {
      node =>
        val deployActions = node.parent.actions.filter(_.name == "install")
        var allSCPs = Map[String, String]()
        var allScripts = Map[String, Map[String, ScriptArgumentValue]]()

        deployActions.foreach {
          action => {
            allSCPs = allSCPs ++ action.scpMap
            allScripts = allScripts ++ action.scriptMap
          }
        }
        var resolvedScripts = node.resolveScripts(allScripts)

        spawn {
          allSCPs.foreach {
            tuple => node.vm.copyTo(build(new URI(tuple._1)), tuple._2)
          }
          nodeState.synchronized(
            nodeState(node).scpComplete = true
          )
          resolvedScripts.foreach {
            script =>
              val scriptName = script._1
              val argMap = script._2
              //vm.execute(scriptName)
              node.vm.executeScript(scriptName, argMap)
          }
          nodeState.synchronized(
            nodeState(node).scriptsComplete = true          
          )
        }
    }
  }

  //createScaleGroups();
  private def printBoundLasicProgram {
    println("paths {")
    nodes.foreach({
      node => println("    " + node.path + ": \"" + node.vmId + "\"  // public=" + showValue(node.vmPublicDns) + "\tprivate=" + showValue(node.vmPrivateDns))
    })
    println("}")
  }


  def doit() {

    // Error checks before doing anything
    validateProgram

    // Startup everything that needs it
    launchAllAMIs
    createAllVolumes
    //createScaleGroups

    // Wait for all resources to be created before proceeding
    waitForAMIsToBoot

    //assignPrivateDNS2Nodes

    waitForVolumes

    // Attach all volumes in preparation for setup
    attachAllVolumes
    waitForVolumesToAttach

    // Configure all the nodes
    startAsyncNodeConfigure

    // Configure prototypical machines for each scale groups
    // startAsyncNodeGroupNodeConfigure

    // Wait for all nodes to be configured
//    waitForVMState(Configured, "Waiting for machines to be configured: ")
      waitForVMScripts

    // Wait for scale group nodes to be configured
    // waitForScaleGroupNodes

    // create scale groups
    // startAsyncScaleGroupCreation
    // waitForScaleGroupCreation

    // Print out the bound program so the user can see the IDs we are manipulating
    printBoundLasicProgram

    //stopAllActors
  }
}