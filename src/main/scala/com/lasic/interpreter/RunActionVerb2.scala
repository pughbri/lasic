package com.lasic.interpreter

import java.net.URI
import se.scalablesolutions.akka.actor.Actor._
import java.io.File
import com.lasic.{VM, Cloud}
import collection.immutable.List
import com.lasic.model._
import com.lasic.util.Logging
import se.scalablesolutions.akka.actor.Actor._
import com.lasic.cloud._

/**
 *
 * @author Brian Pugh
 */

class RunActionVerb2(val actionName: String, val cloud: Cloud, val program: LasicProgram) extends Verb with Logging {
  private val nodes: List[NodeInstance] = program.find("//node[*][*]").map(x => x.asInstanceOf[NodeInstance])
  private val vmState: Map[VMHolder, VMState] = {
    Map.empty ++ (nodes.map {node => (node, new VMState)})
  }

  private def startAsyncRunAction {
    nodes.foreach {
      node =>
          val deployActions = node.parent.actions.filter(_.name == actionName)
          var allSCPs = Map[String, String]()
          var allScripts = Map[String, Map[String, ScriptArgumentValue]]()
          var allIps = Map[Int, String]()
          deployActions.foreach {
            action => {
              allSCPs = allSCPs ++ action.scpMap
              allScripts = allScripts ++ action.scriptMap
              allIps = allIps ++ action.ipMap
            }
          }

          var resolvedScripts = node.resolveScripts(allScripts)

          spawn {
            node.vm = setVM(LaunchConfiguration.build(node), node.boundInstanceId)
            allSCPs.foreach {
              tuple => node.vm.copyTo(build(new URI(tuple._1)), tuple._2)
            }
            vmState.synchronized(
              vmState(node).scpComplete = true
              )
            resolvedScripts.foreach {
              script =>
                val scriptName = script._1
                val argMap = script._2
                //vm.execute(scriptName)
                node.vm.executeScript(scriptName, argMap)
            }
            vmState.synchronized(
              vmState(node).scriptsComplete = true
            )
            allIps.foreach {
              ip => node.vm.associateAddressWith(ip._2)
            }
            vmState.synchronized(
              vmState(node).ipsComplete = true
            )
          }
    }
  }


  def build(uri: URI): File = {
    if (uri.isOpaque) new File(uri.toString.split(":")(1)) else new File(uri)
  }

  private def setVM(lc: LaunchConfiguration, instanceId: String):VM = {
    val vm = cloud.findVM(instanceId)
    if (vm.launchConfiguration != null) {
      vm.launchConfiguration.name = lc.name
      vm.launchConfiguration.userName = lc.userName
      vm.launchConfiguration.key = lc.key
    }
    vm
  }

  private def waitForVMState(vmHolders: List[VMHolder], test: VMHolder => Boolean, statusString: String) {
    var waiting = vmHolders.filter(t => test(t))
    while (waiting.size > 0) {
      val descriptions: List[String] = waiting.map(t => t.vmId + ":" + t.vmState)
      logger.info(statusString + descriptions)
      Thread.sleep(10000)
      waiting = vmHolders.filter(t => test(t))
    }
  }

  private def waitForAction {
    waitForVMState(nodes, {vmHolder => !(vmState(vmHolder).ipsComplete)}, "Waiting for action to finish: ")
  }

  def doit = {
    startAsyncRunAction
    waitForAction
  }

}
