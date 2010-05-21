package com.lasic.interpreter.actors

import scala.actors.Actor
import com.lasic.model.NodeInstance
import com.lasic.cloud.LaunchConfiguration
import com.lasic.VM


case class NodeLaunch(node:NodeInstance, actor:Actor)

object NodeActor  extends Actor {
  def asLaunchConfiguration(node:NodeInstance) = {
    val lc = new LaunchConfiguration
      lc.name = node.parent.name
      lc.machineImage = node.parent.machineimage
      lc.ramdiskId = node.parent.ramdiskid
      lc.kernelId = node.parent.kernelid
      lc.key = node.parent.key
      //lc.groups = node.parent.groups
      //lc.instanceType = node.parent.instancetype
      lc.userName = node.parent.user
      //lc.availabilityZone = node.parent.
      //lc.s3Download = ??
    lc
  }

  def launch(node:NodeInstance, actor:Actor) = {

      CloudActor ! CloudNodeLaunch(asLaunchConfiguration(node), actor, node)
    
  }

  def launched(vm:VM) = {
    println("was launched!!")
  }

  def act() {
    Actor.loop {
      react {
        case NodeLaunch(node, actor) => launch(node, actor)
        case msg => println("Unknown msg sent to cloud actor: "+msg)
      }
    }
  }
}