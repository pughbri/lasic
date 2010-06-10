package com.lasic.interpreter.actors

import com.lasic.{Cloud, VM}
import se.scalablesolutions.akka.actor.Actor._
import com.lasic.cloud.LaunchConfiguration
import se.scalablesolutions.akka.actor.Actor
import com.lasic.Cloud
import VMActor._
import java.io.File
import com.lasic.util.Logging
import com.lasic.model.{ScriptArgumentValue, NodeInstance, LasicProgram}
import java.net.URI

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

  /** The private DNS of the VM we are manipulating */
  var privateDNS:String = null

  /**The actor which does all the blocking operations */
  var sleeper = actorOf[Sleeper].start

  /**
   * Send back a reply of the VM id, if there is one, otherwise null
   */
  def replyWithVMId {
    if (vm != null) {
      val id = vm.instanceId
      if (id != null)
        self.reply(id)
      else
        self.reply(null)
    } else self.reply(null)
  }


  /**
   * Stop this actor and the sleeper actor from running.  This will unconditionally stop
   * both actors regardless of their current state
   */
  def stopEverything { sleeper.stop; self.stop }
  def startAsyncLaunch(lc: LaunchConfiguration) {  sleeper ! MsgSleeperCreateVM(lc,cloud) }
  def startAsyncSCP(configData:ConfigureData) { sleeper ! MsgSleeperSCP(vm,configData) }
  def startAsyncScripts(configData:ConfigureData) { sleeper ! MsgSleeperScripts(vm,configData) }
  def startAsyncBootWait       { sleeper ! MsgSleeperBootWait(vm) }

  /**
   * The message receiver / dispatcher for this actor
   */
  def receive = { case x => respondToMessage(x) }

  import VMActorState._

  /**
   * This is the heart of the state machine -- the transitions from one state to another is accomplished here
   * (and only here!).
   */
  private def respondToMessage(msg:Any) {
    nodeState =
      (nodeState,msg) match {
        case (_,              MsgQueryID)               =>  { replyWithVMId;                              nodeState       }
        case (_,              MsgQueryState)            =>  { self.reply(nodeState);                      nodeState       }
        case (_,              MsgQueryPrivateDNS)       =>  { self.reply(privateDNS);                     nodeState       }
        case (_,              MsgStop)                  =>  { stopEverything;                             Froggy          }
        case (Blank,          MsgLaunch(lc))            =>  { startAsyncLaunch(lc);                       WaitingForVM    }
        case (Booted,         MsgConfigure(config))     =>  { startAsyncSCP(config);                      RunningSCP      }
        case (RunningSCP,     MsgSCPCompleted(config))  =>  { startAsyncScripts(config);                  RunningScripts  }
        case (RunningScripts, MsgScriptsCompleted(x))   =>  {                                             Configured      }
        case (WaitingForBoot, MsgSetBootState(false))   =>  { startAsyncBootWait;                         WaitingForBoot  }
        case (WaitingForBoot, MsgSetBootState(true))    =>  {                                             Booted          }
        case (WaitingForVM,   MsgSetVM(avm,dns))        =>  { vm=avm; privateDNS=dns; startAsyncBootWait; WaitingForBoot  }
        case _                                          =>  {                                             nodeState       }
      }
  }
}

/**
 *  A VMActor is a
 */
object VMActor {
  object VMActorState extends Enumeration {
    type VMActorState = Value
    val Blank, WaitingForVM, WaitingForBoot, Booted, RunningSCP, RunningScripts, Configured, Froggy = Value
  }

  class ConfigureData(val scp: Map[String, String], val scripts: Map[String, Map[String, List[String]]])

  /**
   * These are public commands, which cause state transitions, that can be sent to the NodeActor as part
   * of its public API
   */
  case class MsgConfigure(configData: ConfigureData)
  case class MsgLaunch(lc: LaunchConfiguration)
  case class MsgQueryState()
  case class MsgQueryID()
  case class MsgQueryPrivateDNS()
//  case class MsgQueryVM()
  case class MsgStop()

  // Messages involving the Sleeper

  // Messages sent *TO* the sleeper
  private case class MsgSleeperSCP(vm:VM, configureData: ConfigureData)
  private case class MsgSleeperScripts(vm:VM, configureData: ConfigureData)
  private case class MsgSleeperBootWait(vm: VM)
  private case class MsgSleeperCreateVM(lc: LaunchConfiguration, cloud: Cloud)
  private case class MsgSleeperStop

  // Messages sent *FROM* the sleeper
  private case class MsgSCPCompleted(val cd: ConfigureData)
  private case class MsgScriptsCompleted(val cd: ConfigureData)
  private case class MsgSetVM(vm: VM, privateDNS:String)
  private case class MsgSetBootState(isInitialized: Boolean)


  /**
   *  A private actor which performs all the blocking operations on a node.   A NodeActor delegates all blocking
   * operations to an instance of this actor -- enabling the NodeActor to 1) appear to complete all operations
   * asynchronously and 2) allow the NodeActor to be queried for status while long running (and blocking)
   * operations are occurring.
   */
  private class Sleeper extends Actor {
    def receive = {
      case MsgSleeperBootWait(vm) => {
        Thread.sleep(2000);
        self.reply(MsgSetBootState(vm.isInitialized))
      }

      case MsgSleeperCreateVM(lc, cloud) => {
        val vm = cloud.createVM(lc, true)
        val dns = vm.getPrivateDns
        self.reply(MsgSetVM(vm,dns))
      }

      case MsgSleeperSCP(vm,configData) => {
        configData.scp.foreach {
          foo =>
            vm.copyTo(build(new URI(foo._1)), foo._2)
        }
        self.reply(MsgSCPCompleted(configData))
      }

      case MsgSleeperScripts(vm,configData) => {
        configData.scripts.foreach {
          script =>
            val scriptName = script._1
            val argMap = script._2
            //vm.execute(scriptName)
          vm.executeScript(scriptName, argMap)
        }
        self.reply(MsgScriptsCompleted(configData))
      }

      case MsgSleeperStop => self.stop

    }

    def build(uri: URI): File = {
      if (uri.isOpaque) new File(uri.toString.split(":")(1)) else new File(uri)
    }
  }

}