package com.lasic.interpreter


import actors.RunActionActor
import actors.RunActionActor._
import com.lasic.util.Logging
import com.lasic.Cloud
import se.scalablesolutions.akka.actor.Actor
import com.lasic.model.{ScriptArgumentValue, NodeInstance, LasicProgram}
import RunActionActor.RunActionActorState._
import com.lasic.interpreter.VerbUtil._
import com.lasic.cloud.LaunchConfiguration

/**
 *
 * @author Brian Pugh
 */

class RunActionVerb(val actionName: String, val cloud: Cloud, val program: LasicProgram) extends Verb with Logging {
  private val nodes: List[NodeInstance] = program.find("//node[*][*]").map(x => x.asInstanceOf[NodeInstance])

  private def startAllActors {
    nodes.foreach {_.actor = Actor.actorOf(new RunActionActor(cloud)).start}
  }

  private def stopAllActors {
    nodes.foreach {_.actor ! MsgStop}
  }


  private def startAsyncRunAction {
    nodes.foreach {
      node =>
        val deployActions = node.parent.actions.filter(_.name == actionName)
        var allSCPs = Map[String, String]()
        var allScripts = Map[String, Map[String, ScriptArgumentValue]]()
        deployActions.foreach {
          action => {
            allSCPs = allSCPs ++ action.scpMap
            allScripts = allScripts ++ action.scriptMap
          }
        }

        val configData = new ActionData(new LaunchConfiguration(node), node.boundInstanceId, allSCPs, node.resolveScripts(allScripts))
        node.actor ! MsgRunAction(configData)
    }
  }

  private def waitForActionActorState(state: State, statusString: String) {
    var waiting = nodes.filter(t => !t.isInState(state))
    while (waiting.size > 0) {
      val descriptions: List[String] = waiting.map(t => showValue(t.instanceID) + ":" + showValue(t.nodeState))
      logger.info(statusString + descriptions)
      Thread.sleep(5000)
      waiting = nodes.filter(t => !t.isInState(state))
    }
  }


  def doit = {
    startAllActors
    startAsyncRunAction
    waitForActionActorState(RunActionActor.RunActionActorState.Configured, "Waiting for machines to be configured: ")
    stopAllActors
  }

}