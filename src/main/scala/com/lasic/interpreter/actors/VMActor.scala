package com.lasic.interpreter.actors

import com.lasic.{Cloud, VM}
import se.scalablesolutions.akka.actor.Actor._
import com.lasic.cloud.LaunchConfiguration
import com.lasic.Cloud
import VMActor._
import java.io.File
import com.lasic.util.Logging
import com.lasic.model.{ScriptArgumentValue, NodeInstance, LasicProgram}
import java.net.URI
import se.scalablesolutions.akka.actor.{ActorRef, Actor}

/**
 * An Actor which is also a finite state machine for nodes in the cloud.   An instance of this class represents
 * a specific VM (and corresponding machine in the cloud) and will perform asynchronous operations on that
 * VM based on messages sent to the Actor.   For this to operate correctly, it is important to clearly document and
 * maintain the FSM.  The FSM is included in this source code distribution as XXX
 */
class VMActor(cloud: Cloud) extends Actor with Logging {

  /**Current state of the FSM */
  var nodeState = VMActorState.Blank

  /**The VM we are manipulating **/
  var vm: VM = null

  /**
   * Send back a reply of the VM id, if there is one, otherwise null
   */
  def replyWithVMId {
    var result: String = "?"
    if (vm != null && vm.instanceId != null)
      result = vm.instanceId
    self.senderFuture.foreach(_.completeWithResult(result))
  }


  def vmOperation(op: VM => Any) {
    if (vm == null) {
      self.senderFuture.foreach(_.completeWithResult(None))
    } else {
      val senderFuture = self.senderFuture
      spawn {
        val result = op(vm)
        senderFuture.foreach(_.completeWithResult(result))
        //      replyTo ! result
      }
    }
  }

  def build(uri: URI): File = {
    if (uri.isOpaque) new File(uri.toString.split(":")(1)) else new File(uri)
  }

  /**
   * Stop this actor and the sleeper actor from running.  This will unconditionally stop
   * both actors regardless of their current state
   */
  def stopEverything {
    self.stop
  }

  def startAsyncLaunch(lc: LaunchConfiguration) {
    val me = self
    spawn {
      vm = cloud.createVM(lc, true)
      me ! MsgSetVM
    }
  }

  def startAsyncSCP(configData: ConfigureData) {
    val me = self
    spawn {
      configData.scp.foreach {
        foo =>
          vm.copyTo(build(new URI(foo._1)), foo._2)
      }
      me ! MsgSCPCompleted(configData)
    }

  }

  def startAsyncScripts(configData: ConfigureData) {
    val me = self
    spawn {
      configData.scripts.foreach {
        script =>
          val scriptName = script._1
          val argMap = script._2
          //vm.execute(scriptName)
          vm.executeScript(scriptName, argMap)
      }
      me ! MsgScriptsCompleted(configData)
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

  import VMActorState._

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
              case (WaitingForVM,   MsgSetVM)                 => {startAsyncBootWait;         WaitingForBoot}
              case _                                          => {                            nodeState}
            }
  }
}

/**
 * Companion object that contains messages to send, etc.
 */
object VMActor {

  /** The states of the VMActor state machine */
  object VMActorState extends Enumeration {
    type State = Value
    val Blank, WaitingForVM, WaitingForBoot, Booted, RunningSCP, RunningScripts, Configured, Froggy = Value
  }

  /** Configuation information sent to setup a VM */
  class ConfigureData(val scp: Map[String, String], val scripts: Map[String, Map[String, List[String]]])

  /**
   * These are public messages, which cause state transitions, that can be sent to the VMACtor as part
   * of its public API
   */
  case class MsgConfigure(configData: ConfigureData)
  case class MsgLaunch(lc: LaunchConfiguration)
  case class MsgQueryState()
  case class MsgStop()
  case class MsgVMOperation(operation: VM => Any)

  // Private messages sent to ourselves
  private case class MsgSetBootState(isInitialized: Boolean)
  private case class MsgSetVM()
  private case class MsgSCPCompleted(val cd: ConfigureData)
  private case class MsgScriptsCompleted(val cd: ConfigureData)
}


/**
 * A utility mixin to to make interacting with a VMActor a bit easier.  It presents blocking methods that
 * hide the fact that they send messages underneath.
 */
trait VMActorUtil {
  var actor: ActorRef = null

  private def asString(x: Any):String = x match {
    case Some(null) => "?"
    case Some(s) => s.toString
    case None => "?"
  }

  def instanceID: String = {
    val a = actor !! MsgVMOperation({vm: VM => vm.instanceId})
    asString(a)
  }

  def publicDNS: String = {
    val x = actor !! MsgVMOperation({vm: VM => vm.getPublicDns})
    asString(x)
  }

  def privateDNS: String = {
    val x = actor !! MsgVMOperation({vm: VM => vm.getPrivateDns})
    asString(x)
  }

  def nodeState: Any = {
    val x = (actor !! MsgQueryState)
    println(x)
    x match {
      case Some(y) => y.asInstanceOf[VMActorState.State]
      case _ => VMActorState.Blank
    }
  }

  def isInState(x: VMActorState.State) = {
    val y = actor !! MsgQueryState
    val result: Boolean =
    y match {
      case Some(something) => something == x
      case x => false
    }

    result

  }

}
