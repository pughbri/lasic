package com.lasic.interpreter

import actors._
import VMActor._
import VMActor.VMActorState._
import com.lasic.model.{NodeInstance, LasicProgram}
import com.lasic.{Cloud, VM}
import se.scalablesolutions.akka.actor.Actor._
import com.lasic.cloud.LaunchConfiguration
import se.scalablesolutions.akka.actor.{ActorRef, Actor}


private class NodeTracker(val actor:ActorRef, val node:NodeInstance) {
  var _instanceID:String = null

  def instanceID:String = {
    if ( _instanceID==null ) {
      val x:Option[Nothing] = actor !! MsgQueryID
      val y:String = x.get
      if ( y!=null )
        _instanceID = y.toString
    }
    if ( _instanceID!=null ) _instanceID else "(Not Assigned)"
  }

  def nodeState:Any = {
    actor !! MsgQueryState
//    match {
//      case Some(x:VMActorState) => x
//      case _ => null
//    }
  }
  
  def isInState(x:VMActorState) = {
    val y = actor !! MsgQueryState
    val result:Boolean =
    y match {
      case Some(something) => something==x
      case x => false
    }

    result

  }
}


class DeployVerb(val cloud: Cloud, val program: LasicProgram) extends Verb {
  private val foo:List[NodeInstance] = program.find("//node[*][*]").map( x => x.asInstanceOf[NodeInstance])
  private val nodeTrackers:List[NodeTracker] = foo.map{
          node =>
            val actor = Actor.actorOf(new VMActor(cloud))
            new NodeTracker(actor,node)
  }

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
    nodeTrackers.foreach {
      tracker =>
        tracker.actor.start
    }
  }

  private def stopAllActors {
    nodeTrackers.foreach { tracker => tracker.actor ! MsgStop }
  }

  private def launchAllAMIs {
    nodeTrackers.foreach { tracker => tracker.actor ! new MsgLaunch(new LaunchConfiguration(tracker.node))}
  }

  private def createAllVolumes {}

  private def waitForAMIsToBoot {
    waitForVMActorState(Booted, "Waiting for machines to boot: ")
    println("Booted IDs are: "+nodeTrackers.map( t => t.instanceID +":"+t.nodeState ))
  }

  private def waitForVMActorState(state:VMActorState, statusString:String) {
    var waiting = nodeTrackers.filter( t => !t.isInState(state))
    while( waiting.size>0 ) {
      val descriptions:List[String] = waiting.map( t => t.instanceID+":"+t.nodeState )
      println(statusString + descriptions)
      Thread.sleep(5000)
      waiting = nodeTrackers.filter( t=> !t.isInState(state) )
    }
  }

  private def waitForVolumes {}
  private def attachAllVolumes {}
  private def waitForVolumesToAttach {}

  private def startAsyncNodeConfigure {
    nodeTrackers.foreach {
      tracker =>
//        val scp = Map.empty ++ tracker.node.parent.scpMap
//        val x = tracker.node.parent.scriptMap
//        val theList = x.map {
//          tuple =>
//            val key = tuple._1
//            val value = Map.empty ++ tuple._2
//            (key,value)
//        }
//        val scripts = Map.empty ++ theList
//        //val scripts = Map.empty ++ tracker.node.parent.scriptMap
//        val configData = new ConfigureData(scp,scripts)
        val configData = new ConfigureData(tracker.node.parent.scpMap, tracker.node.parent.scriptMap)
        tracker.actor ! MsgConfigure(configData)
    }
  }
  
  //createScaleGroups();
  private def printBoundLasicProgram {}

  def doit() {

    // Error checks before doing anything
    validateProgram

    // Startup everything that needs it
    startAllActors
    launchAllAMIs
    createAllVolumes

    // Wait for all resources to be created before proceeding
    waitForVMActorState(Booted, "Waiting for machines to boot: ")
    waitForVolumes

    // Attach all volumes in preparation for setup
    attachAllVolumes
    waitForVolumesToAttach

    // Configure all the nodes
    startAsyncNodeConfigure

    // Configure prototypical machines for each scale groups
    // startAsyncNodeGroupNodeConfigure

    // Wait for all nodes to be configured
    waitForVMActorState(Configured,"Waiting for machines to be configured: ")

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