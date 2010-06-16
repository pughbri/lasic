package com.lasic.interpreter

import actors._
import VMActor._
import VMActor.VMActorState._
import com.lasic.{Cloud, VM}
import se.scalablesolutions.akka.actor.Actor._
import com.lasic.cloud.LaunchConfiguration
import se.scalablesolutions.akka.actor.{ActorRef, Actor}
import com.lasic.model._
import com.lasic.util.Logging
import se.scalablesolutions.akka.dispatch.{DefaultCompletableFuture, CompletableFuture}



class DeployVerb(val cloud: Cloud, val program: LasicProgram) extends Verb with Logging {
  private val nodes: List[NodeInstance] = program.find("//node[*][*]").map(x => x.asInstanceOf[NodeInstance])

  private def validateProgram {}

  private def startAllActors {
    nodes.foreach { _.actor = Actor.actorOf(new VMActor(cloud)).start  }
  }

  private def stopAllActors {
    nodes.foreach { _.actor ! MsgStop}
  }

  private def launchAllAMIs {
    nodes.foreach {node => node.actor ! new MsgLaunch(new LaunchConfiguration(node))}
  }

  private def createAllVolumes {}

  private def waitForAMIsToBoot {
    waitForVMActorState(Booted, "Waiting for machines to boot: ")
    logger.info("Booted IDs are: " + nodes.map(t => showValue(t.instanceID) + ":" + showValue(t.nodeState)))
  }

  private def waitForVMActorState(state: State, statusString: String) {
    var waiting = nodes.filter(t => !t.isInState(state))
    while (waiting.size > 0) {
      val descriptions: List[String] = waiting.map(t => showValue(t.instanceID) + ":" + showValue(t.nodeState))
      logger.info(statusString + descriptions)
      Thread.sleep(10000)
      waiting = nodes.filter(t => !t.isInState(state))
    }
  }

  private def showValue(x: Any) = x match {
    case Some(s) => s
    case None => "?"
    case y => y
  }

  private def waitForVolumes {}

  private def attachAllVolumes {}

  private def waitForVolumesToAttach {}

  private def startAsyncNodeConfigure {
    nodes.foreach {
      node =>
        val configData = new ConfigureData(node.parent.scpMap, node.resolveScripts)
        node.actor ! MsgConfigure(configData)
    }
  }

  //createScaleGroups();
  private def printBoundLasicProgram {
    println("paths {")
    nodes.foreach( {
      node => println("    " + node.path + ": " + node.instanceID + "  // public=" + showValue(node.publicDNS) + "\tprivate=" + showValue(node.privateDNS))
    })
    println("}")
  }


  def doit() {

    // Error checks before doing anything
    validateProgram

    // Startup everything that needs it
    startAllActors
    launchAllAMIs
    createAllVolumes

    // Wait for all resources to be created before proceeding
    waitForVMActorState(Booted, "Waiting for machines to boot: ")
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
    waitForVMActorState(Configured, "Waiting for machines to be configured: ")

    // Wait for scale group nodes to be configured
    // waitForScaleGroupNodes

    // create scale groups
    // startAsyncScaleGroupCreation
    // waitForScaleGroupCreation

    // Print out the bound program so the user can see the IDs we are manipulating
    printBoundLasicProgram

    stopAllActors
  }
}