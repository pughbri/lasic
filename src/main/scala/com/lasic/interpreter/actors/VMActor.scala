package com.lasic.interpreter.actors

import se.scalablesolutions.akka.actor.Actor._
import java.net.URI
import com.lasic.{VM, Cloud}
import java.io.File
import VMActor._
import se.scalablesolutions.akka.actor.{ActorRef, Actor}

/**
 *
 * @author Brian Pugh
 */

trait VMActor extends Actor {
  protected val cloud: Cloud
  protected var vm: VM
  protected var nodeState: Any

  def vmOperation(op: VM => Any) {
    if (vm == null) {
      self.senderFuture.foreach(_.completeWithResult(None))
    } else {
      val senderFuture = self.senderFuture
      spawn {
        val result = op(vm)
        senderFuture.foreach(_.completeWithResult(result))
      }
    }
  }

  /**
   * Send back a reply of the VM vmId, if there is one, otherwise null
   */
  def replyWithVMId {
    var result: String = "?"
    if (vm != null && vm.instanceId != null)
      result = vm.instanceId
    self.senderFuture.foreach(_.completeWithResult(result))
  }

  /**
   * Stop this actor .  This will unconditionally stop
   * regardless of current state
   */
  def stopEverything {
     self.stop
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


  def build(uri: URI): File = {
    if (uri.isOpaque) new File(uri.toString.split(":")(1)) else new File(uri)
  }


}

object VMActor {
  /**Configuation information sent to setup a VM */
  class ConfigureData(val scp: Map[String, String], val scripts: Map[String, Map[String, List[String]]])


  /**
   * These are public messages, which cause state transitions, that can be sent to the VMACtor as part
   * of its public API
   */
  case class MsgVMOperation(operation: VM => Any)
  case class MsgQueryState()


  // Private messages sent to ourselves
  private[actors] case class MsgSCPCompleted(val cd: ConfigureData)
  private[actors] case class MsgScriptsCompleted(val cd: ConfigureData)


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
    x match {
      //case Some(y) => y.asInstanceOf[DeployActorState.State]
      case Some(y) => y
//      case _ => DeployActorState.Blank
      case x => x
    }
  }

  def isInState(x: Any) = {
    val y = actor !! MsgQueryState
    val result: Boolean =
    y match {
      case Some(something) => something == x
      case x => false
    }

    result

  }

}