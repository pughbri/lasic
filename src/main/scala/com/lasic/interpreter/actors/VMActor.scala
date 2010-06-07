package com.lasic.interpreter.actors

import com.lasic.model.{NodeInstance, LasicProgram}
import com.lasic.{Cloud, VM}
import se.scalablesolutions.akka.actor.Actor._
import com.lasic.cloud.LaunchConfiguration
import se.scalablesolutions.akka.actor.Actor
import com.lasic.Cloud


//object VMActor {
  /**
   * These are the FSM states held by the NodeActor
   */
  object VMActorState extends Enumeration {
    type NodeStates = Value
    val Blank, WaitingForVM, WaitingForBoot, Booted, Configured = Value
  }

  /**
   * These are commands, which cause state transitions, that can be sent to the NodeActor
   */
  case class NodeCommand()
  case class Launch(lc:LaunchConfiguration) extends NodeCommand
  case class SetVM(vm:VM) extends NodeCommand
  case class QueryNodeState() extends NodeCommand
  case class QueryID() extends NodeCommand
  case class SetBootState(isInitialized:Boolean) extends NodeCommand
  case class StopVMActor extends NodeCommand
//}

import VMActorState._
/**
 * A private actor which performs all the blocking operations on a node.   A NodeActor delegates all blocking
 * operations to an instance of this actor -- enabling the NodeActor to 1) appear to complete all operations
 * asynchronously and 2) allow the NodeActor to be queried for status while long running (and blocking)
 * operations are occurring.
 */
private class Sleeper extends Actor {

  def receive = {

    case ("checkBootState", vm: VM) => {
      Thread.sleep(500);
      self.reply(SetBootState(vm.isInitialized))
    }

    case ("createVM", lc: LaunchConfiguration, cloud: Cloud) => {
      val vm = cloud.createVM(lc, true)
      self.reply(SetVM(vm))
    }

  }
}

/**
 * An Actor which is also a finite state machine for nodes in the cloud.   An instance of NodeActor represents
 * a specific NodeInstance and will perform asynchronous operations on that NodeInstance based on messages
 * sent to the Actor.   For this to operate correctly, it is important to clearly document and maintain the FSM.
 * The FSM is included in this source code distribution as XXX
 */
class VMActor(cloud: Cloud) extends Actor {
//  object NodeCommand extends Enumeration {
//    type WeekDay = Value
//    val QueryNodeState, Launch, SetVM, SetBootState, RunScripts = Value
//  }

  var nodeState = VMActorState.Blank
  var vm: VM = null
  var sleeper = actorOf[Sleeper].start


  def handleQueryID: Unit = {
    if (vm != null) {
      val id = vm.instanceId
      if (id != null)
        self.reply(id)
      else
        self.reply(null)
    } else self.reply(null)
  }

  def handleStopVMActor: Unit = {
    sleeper.stop;
    self.stop
  }

  def receive = {
//    case (Launch,x:LaunchConfiguration) => handleLaunch(x)
    case Launch(lc)           => handleLaunch(lc)
    case SetVM(vm)            => handleVM(vm)
    case QueryNodeState       => self.reply(nodeState)
    case SetBootState(booted) => handleWake(booted)
    case QueryID              => handleQueryID
    case StopVMActor          => handleStopVMActor
    case _                    => println("something else")
  }

  def handleVM(x: VM) {
    nodeState match {
      case WaitingForVM => {vm = x; nodeState = WaitingForBoot; asyncCheckIfVMIsBooted}
    }
  }

  def asyncCheckIfVMIsBooted {
    sleeper ! ("checkBootState", vm)
  }

  def handleWake(bootState: Boolean) = {
    nodeState match {
      case WaitingForBoot => {
        if (bootState) nodeState = Booted
        else asyncCheckIfVMIsBooted
      }
    }
  }

  def handleLaunch(lc:LaunchConfiguration) = {
    nodeState match {
      case Blank => {
        nodeState = WaitingForVM
        sleeper ! ("createVM", lc, cloud)
      }
      case _ => // no state change
    }
  }
}
