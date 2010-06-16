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

//private class NodeTracker(val node: NodeInstance) {
//  var _instanceID: String = null
//
//  def instanceID: String = {
//    if (_instanceID == null) {
//      val x: Option[Nothing] = node.actor !! MsgQueryID
//      val y: String = x.get
//      if (y != null)
//        _instanceID = y.toString
//    }
//    if (_instanceID != null) _instanceID else "(Not Assigned)"
//  }
//
//  def nodeState: Any = {
//    node.actor !! MsgQueryState
//    //    match {
//    //      case Some(x:VMActorState) => x
//    //      case _ => null
//    //    }
//  }
//
//  def isInState(x: VMActorState) = {
//    val y = node.actor !! MsgQueryState
//    val result: Boolean =
//    y match {
//      case Some(something) => something == x
//      case x => false
//    }
//
//    result
//
//  }
//
//  def resolveScriptArguments(args: Map[String, ScriptArgumentValue]): Map[String, List[String]] = {
//    Map.empty ++ args.map {
//      argTuple: Tuple2[String, ScriptArgumentValue] =>
//        val values: List[String] = argTuple._2 match {
//          case x: LiteralScriptArgumentValue => List(x.literal)
//          case x: PathScriptArgumentValue => {
//            val a = x.literal
//            val b = node.findNodes(a)
//            val c = b.map { _.privateDNS }
//            c
//          }
//        }
//        (argTuple._1, values)
//    }
//  }
//
//  def resolveScripts: Map[String, Map[String, List[String]]] = {
//    Map.empty ++ node.parent.scriptMap.map {
//      scriptTuple => (scriptTuple._1, resolveScriptArguments(scriptTuple._2))
//    }
//  }
//}


class DeployVerb(val cloud: Cloud, val program: LasicProgram) extends Verb with Logging {
  private val nodes: List[NodeInstance] = program.find("//node[*][*]").map(x => x.asInstanceOf[NodeInstance])

  //  private def waitForAllNodesToReachState(state:VMActorState) {
  //
  //  }
  //  private def notBootedList(nodes:List[NodeTracker])= {
  //    nodes.filter { t => t.isBooted }
  //  }
  //
  //  private def vmIDs(nodes:List[NodeTracker])= {
  //    nodes.map { t => t.instanceID }
  //  }

  private def validateProgram {}

  private def startAllActors {
    nodes.foreach { node =>
      node.actor = Actor.actorOf(new VMActor(cloud))
      node.actor.start
    }
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
    println("Booted IDs are: " + nodes.map(t => t.instanceID + ":" + t.nodeState))
  }

  private def waitForVMActorState(state: VMActorState, statusString: String) {
    var waiting = nodes.filter(t => !t.isInState(state))
    while (waiting.size > 0) {
      val descriptions: List[String] = waiting.map(t => t.instanceID + ":" + t.nodeState)
      println(statusString + descriptions)
      Thread.sleep(5000)
      waiting = nodes.filter(t => !t.isInState(state))
    }
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
      node => println("    " + node.path + ": " + node.instanceID + "  // public=" + node.publicDNS + "\tprivate=" +node.privateDNS)
    })
    println("}")
  }

//  private def assignPrivateDNS2Nodes {
//    node.foreach {
//      tracker =>
//        var func = { vm:VM => (vm.instanceId, vm.getPublicDns, vm.getPrivateDns) }
//        var result = (tracker.actor !! MsgVMOperation(func)).get
//        tracker.node.instanceID =
//        tracker.node.privateDNS =
//
//        func = { vm:VM => vm.getPublicDns }
//        result = tracker.actor !! MsgVMOperation(func)
//        tracker.node.publicDNS = result.get
//
//        func = { vm:VM => vm.instanceId }
//        result = tracker.actor !! MsgVMOperation(func)
//        tracker.node.instanceID = result.get
//
//        tracker.node.privateDNS = (tracker.actor !! MsgQueryPrivateDNS).get.toString
//        logger.debug("assigning private dns [" + tracker.node.privateDNS + "] to " + tracker.node.path)
//    }
//  }

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