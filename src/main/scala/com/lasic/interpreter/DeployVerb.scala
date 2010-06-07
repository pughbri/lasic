package com.lasic.interpreter

import actors._
import com.lasic.model.{NodeInstance, LasicProgram}
import com.lasic.{Cloud, VM}
import se.scalablesolutions.akka.actor.Actor._
import com.lasic.cloud.LaunchConfiguration
import se.scalablesolutions.akka.actor.{ActorRef, Actor}



private class NodeTracker(val actor:ActorRef, val node:NodeInstance) {
  var _instanceID:String = null

  def instanceID:String = {
    if ( _instanceID==null ) {
      val x:Option[Nothing] = actor !! QueryID
      val y:String = x.get
      if ( y!=null )
        _instanceID = y.toString
    }
    if ( _instanceID!=null ) _instanceID else "(Not Assigned)"
  }

  def isBooted = {
    actor !! QueryNodeState match {
      case Some(VMActorState.Booted) =>  true
      case x => false
    }
  }
}


class DeployVerb(val cloud: Cloud, val program: LasicProgram) extends Verb {
  private val foo:List[NodeInstance] = program.find("//node[*][*]").map( x => x.asInstanceOf[NodeInstance])
  private val nodeTrackers:List[NodeTracker] = foo.map{
          node =>
            val actor = Actor.actorOf(new VMActor(cloud))
            new NodeTracker(actor,node)
  }

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
    nodeTrackers.foreach { tracker => tracker.actor ! StopVMActor }
  }

  private def launchAllAMIs {
    nodeTrackers.foreach { tracker => tracker.actor ! new Launch(new LaunchConfiguration(tracker.node))}
  }

  private def createAllVolumes {}

  private def waitForAMIsToBoot {
    var waiting = nodeTrackers.filter( t => !t.isBooted )
    while( waiting.size>0 ) {
      val descriptions:List[String] = waiting.map( t => t.instanceID )
      println("Waiting for machines to boot: " + descriptions)
      Thread.sleep(5000)
      waiting = nodeTrackers.filter( t=> !t.isBooted )
    }
    val ids = nodeTrackers.map( t => t.instanceID )
    println("Booted IDs are: "+ids)
  }

  private def waitForVolumes {}
  private def attachAllVolumes {}
  private def waitForVolumesToAttach {}

  private def runScpStatements {
    nodeTrackers.foreach {
      tracker =>
        val scp = tracker.node.parent.scpMap
        tracker.actor ! RunSCP( Map.empty ++ scp )
    }
  }
  
  private def runSetupScripts {}
  //createScaleGroups();
  private def printBoundLasicProgram {}

  def doit() {

    validateProgram

    startAllActors
    launchAllAMIs
    createAllVolumes
    waitForAMIsToBoot
    waitForVolumes

    printBoundLasicProgram
    
    attachAllVolumes
    waitForVolumesToAttach
    runScpStatements
    runSetupScripts
    //createScaleGroups();

    stopAllActors
 }
}