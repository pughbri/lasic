package com.lasic.interpreter.actors

import com.lasic.VM
import se.scalablesolutions.akka.actor.Actor._
import com.lasic.cloud.LaunchConfiguration
import com.lasic.Cloud
import DeployActor._
import VMActor._
import com.lasic.util.Logging
import se.scalablesolutions.akka.actor.{ActorRef, Actor}

/**
 * An Actor which is also a finite state machine for nodes in the cloud.   An instance of this class represents
 * a specific VM (and corresponding machine in the cloud) and will perform asynchronous operations on that
 * VM based on messages sent to the Actor.   For this to operate correctly, it is important to clearly document and
 * maintain the FSM.  The FSM is included in this source code distribution as XXX
 */
class DeployActor(protected val cloud: Cloud) extends VMActor with Logging {

  /**Current state of the FSM */
  var nodeState:Any = DeployActorState.Blank

  /**The VM we are manipulating **/
  protected var vm: VM = null

  def startAsyncLaunch(lc: LaunchConfiguration) {
    val me = self
    spawn {
        val avm = cloud.createVM(lc, true)
        me ! MsgSetVM(avm)
    }
  }

  def startAsyncBootWait {
    val vmActor = self
    spawn {
      Thread.sleep(2000);
      val initialized: Boolean = vm.isInitialized
      if (initialized) {
        vmActor ! MsgSetBootState(true)
      }
      else {
        vmActor ! MsgSetBootState(false)
      }
    }
  }

  /**
   * The message receiver / dispatcher for this actor
   */
  def receive = {case x => respondToMessage(x)}

  import DeployActorState._

  /**
   * This is the heart of the state machine -- the transitions from one state to another is accomplished here
   * (and only here!).
   */
  private def respondToMessage(msg: Any) {
    nodeState =
            (nodeState, msg) match {
              //    currentState    MessageRecieved               Operations to do            Next State
              case (_,              MsgVMOperation(op))       => {vmOperation(op);            nodeState}
              case (_,              MsgQueryState)            => {self.reply(nodeState);      nodeState}
              case (_,              MsgStop)                  => {stopEverything;             Froggy}
              case (Blank,          MsgLaunch(lc))            => {startAsyncLaunch(lc);       WaitingForVM}
              case (Booted,         MsgConfigure(config))     => {startAsyncSCP(config);      RunningSCP}
              case (RunningSCP,     MsgSCPCompleted(config))  => {startAsyncScripts(config);  RunningScripts}
              case (RunningScripts, MsgScriptsCompleted(x))   => {                            Configured}
              case (WaitingForBoot, MsgSetBootState(false))   => {startAsyncBootWait;         WaitingForBoot}
              case (WaitingForBoot, MsgSetBootState(true))    => {                            Booted}
              case (WaitingForVM,   MsgSetVM(avm))            => {vm=avm; startAsyncBootWait; WaitingForBoot}
              case _                                          => {                            nodeState}
            }
  }
}

/**
 * Companion object that contains messages to send, etc.
 */
object DeployActor {

  /** The states of the VMActor state machine */
  object DeployActorState extends Enumeration {
    type State = Value
    val Blank, WaitingForVM, WaitingForBoot, Booted, RunningSCP, RunningScripts, Configured, Froggy = Value
  }


  /**
   * These are public messages, which cause state transitions, that can be sent to the VMACtor as part
   * of its public API
   */
  case class MsgConfigure(configData: ConfigureData)
  case class MsgLaunch(lc: LaunchConfiguration)
  
  case class MsgStop()


  // Private messages sent to ourselves
  private case class MsgSetBootState(isInitialized: Boolean)
  private case class MsgSetVM(avm:VM)

}


