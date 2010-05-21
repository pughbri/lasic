package com.lasic.interpreter.actors

import com.lasic.{VM, Cloud}
import com.lasic.cloud.{MachineState, LaunchConfiguration}
import scala.actors._


case class CloudNodeLaunch(launchConfig:LaunchConfiguration, actor:Actor, handle:Any)
case class CloudNodeLaunched(vm:VM, handle:Any)
case class TestIsRunning(vm:VM, actor:Actor, handle:Any)

object CloudActor  extends Actor {
  var cloud:Cloud = null

  def start(cloud:Cloud) {
    this.cloud = cloud
    start()
  }

  def launch(lc:LaunchConfiguration, actor:Actor, handle:Any) = {
    val vm = cloud.createVMs(lc,1,true)(0)
    testIsRunning(vm,actor,handle)    
  }

  def queueTest(vm:VM, actor:Actor, handle:Any) = {
    val foo = this
    Actor.actor {
      Thread.sleep(5000)
      foo ! TestIsRunning(vm,actor,handle)
    }
  }

  def testIsRunning(vm:VM, actor:Actor, handle:Any) {
    vm.getState match {
      case MachineState.Running => testIsBooted(vm,actor,handle)
      case _ => queueTest(vm,actor,handle)
    }
  }

  def testIsBooted(vm:VM, actor:Actor, handle:Any) = {
    // how do I test this??
    actor ! CloudNodeLaunched(vm,handle)
  }



  def act() {
    Actor.loop {
      react {
        case CloudNodeLaunch(lc,actor,handle) => launch(lc,actor,handle)
        case TestIsRunning(vm,actor,handle) => testIsRunning(vm,actor,handle)
        case msg => println("Unknown msg sent to cloud actor: "+msg)
      }
    }
  }
}