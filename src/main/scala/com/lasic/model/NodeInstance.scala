package com.lasic.model

import com.lasic.interpreter.actors._
import com.lasic.interpreter.actors.DeployActor
import com.lasic.interpreter.actors.DeployActor._
import com.lasic.interpreter.actors.DeployActor.DeployActorState._
import com.lasic.VM
import se.scalablesolutions.akka.actor.ActorRef

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 12, 2010
 * Time: 4:59:14 PM
 * To change this template use File | Settings | File Templates.
 */

class NodeInstance(val parentGroup:NodeGroup,idx:Int) extends Pathable with VMActorUtil {
//  var actor:ActorRef = null

  var volumes:List[VolumeInstance] = List()
  def parent = parentGroup
  def path = {
    val result = parentGroup.path + "[%d]".format(idx)
    result
  }
  def children = volumes

  /**
   * args(0) = variable name args(1) = value
   */
  private def resolveScriptArguments(args: Map[String, ScriptArgumentValue]): Map[String, List[String]] = {
    Map.empty ++ args.map {
      argTuple: Tuple2[String, ScriptArgumentValue] =>
        val values: List[String] = argTuple._2 match {
          case x: LiteralScriptArgumentValue => List(x.literal)
          case x: PathScriptArgumentValue => {
            val a = x.literal
            val b = findNodes(a)
            val c = b.map { _.privateDNS }
            c
          }
        }
        (argTuple._1, values)
    }
  }

  def resolveScripts(args: Map[String, Map[String, ScriptArgumentValue]]): Map[String, Map[String, List[String]]] = {
    Map.empty ++ args.map {
      scriptTuple => (scriptTuple._1, resolveScriptArguments(scriptTuple._2))
    }
  }

}