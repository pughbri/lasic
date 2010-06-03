package com.lasic.interpreter.actors

import scala.actors.Actor
import com.lasic.model.NodeInstance
import com.lasic.cloud.LaunchConfiguration
//import ParallelSimulation._
import com.lasic.{Cloud, VM}

case class NodeLaunch

class NodeActor {
  //val lc = new LaunchConfiguration(node)

  var vm:VM = null

//  def handleSimMessage(msg: Any) {
//    msg match {
//      case x:NodeLaunch  => {
//
//        vm = cloud.createVMs(lc, 1, true)(0)
//
//      }
//    }
//
//  }

//
  def asLaunchConfiguration(node:NodeInstance) = {
  }
//
//  def launch(node:NodeInstance, actor:Actor) = {
//
//      CloudActor ! CloudNodeLaunch(asLaunchConfiguration(node), actor, node)
//
//  }
//
//  def launched(vm:VM) = {
//    println("was launched!!")
//  }
//
//  def act() {
//    Actor.loop {
//      react {
//        case NodeLaunch(node, actor) => launch(node, actor)
//        case msg => println("Unknown msg sent to cloud actor: "+msg)
//      }
//    }
//  }
}