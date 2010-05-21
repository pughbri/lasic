package com.lasic.interpreter

import actors._
import scala.actors.Actor
import com.lasic.model.{NodeInstance, LasicProgram}
import com.lasic.VM

case class Deploy(program: LasicProgram)

object DeployActor extends Actor {
  

  def deploy(prog: LasicProgram) = {
    prog.find("//node[*][*]").foreach {
      node =>
              val a = node.asInstanceOf[NodeInstance]
        CloudActor ! CloudNodeLaunch(NodeActor.asLaunchConfiguration(a), this, a)

    }
  }

  def deployed(vm: VM, handle:Any) = {
    val node = handle.asInstanceOf[NodeInstance]
    println("Deployed: " + vm.instanceId)
  }

  def act() {
    Actor.loop {
      react {
        case Deploy(prog) => deploy(prog)
        case CloudNodeLaunched(vm, handle) => deployed(vm,handle)
        case msg => println("Unknown msg sent to deploy actor: " + msg)
      }
    }
  }
}