package com.lasic.interpreter.actors

import com.lasic.{VM, Cloud}
import VMActor._
import com.lasic.interpreter.actors.RunActionActor._
import se.scalablesolutions.akka.actor.Actor._

/**
 *
 * @author Brian Pugh
 */

class RunActionActor(protected val cloud: Cloud) extends VMActor {
  import RunActionActor.RunActionActorState._

  /**Current state of the FSM */
  var nodeState:Any = Blank

  /**The VM we are manipulating **/
  protected var vm: VM = null


  def startAsyncSetVM(actionData: ActionData) {
    val me = self
    spawn {
      val avm = cloud.findVM(actionData.instanceId)
      me ! MsgSetVM(avm, actionData)
    }
  }

  def startAsyncSCP(actionData: ActionData) {
    startAsyncSCP(new ConfigureData(actionData.scp, actionData.scripts))
  }




  /**
   *  The message receiver / dispatcher for this actor
   */
  def receive = {case x => respondToMessage(x)}

  /**
   * This is the heart of the state machine -- the transitions from one state to another is accomplished here
   * (and only here!).
   */
  private def respondToMessage(msg: Any) {
    nodeState =
            (nodeState, msg) match {
             //    currentState    MessageRecieved               Operations to do                        Next State
              case (_,              MsgVMOperation(op))        => {vmOperation(op);                      nodeState}
              case (_,              MsgQueryState)             => {self.reply(nodeState);                nodeState}
              case (_,              MsgStop)                   => {stopEverything;                       Blank}
              case (Blank, MsgRunAction(actionData))           => {startAsyncSetVM(actionData);          WaitingForVM}
              case (Blank, MsgRunAction(actionData))           => {startAsyncSCP(actionData);            RunningSCP}
              case (RunningSCP, MsgSCPCompleted(config))       => {startAsyncScripts(config);            RunningScripts}
              case (RunningScripts, MsgScriptsCompleted(x))    => {                                      Configured}
              case (WaitingForVM,   MsgSetVM(avm, actionData)) => {vm=avm; startAsyncSCP(actionData);    RunningSCP}
              case _ => {nodeState}
            }
  }

}

object RunActionActor {

  /**Configuation information sent to setup a VM */
  class ActionData(val instanceId: String, val scp: Map[String, String], val scripts: Map[String, Map[String, List[String]]])

/**
   * These are public messages, which cause state transitions, that can be sent to the VMACtor as part
   * of its public API
   */
  case class MsgRunAction(actionData: ActionData)
//  case class MsgQueryState()
  case class MsgStop()

  /**
   * prviate messages
   */
  private case class MsgSetVM(avm:VM, actionData: ActionData)


  /**The states of the VMActor state machine */
  object RunActionActorState extends Enumeration {
    type State = Value
    val Blank, WaitingForVM, RunningSCP, RunningScripts, Configured = Value
  }
}


