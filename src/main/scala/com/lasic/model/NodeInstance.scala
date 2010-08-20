package com.lasic.model

//import com.lasic.interpreter.actors._
//import com.lasic.interpreter.actors.DeployActor
//import com.lasic.interpreter.actors.DeployActor._
//import com.lasic.interpreter.actors.DeployActor.DeployActorState._
import com.lasic.VM
import se.scalablesolutions.akka.actor.ActorRef
import com.lasic.cloud.MachineState

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 12, 2010
 * Time: 4:59:14 PM
 * To change this template use File | Settings | File Templates.
 */

//todo: need to get rid of VMActorUtil once the original DeployVerb is gone
class NodeInstance(val parentGroup:NodeGroup,idx:Int) extends Pathable  with VMHolder {

//  var actor:ActorRef = null

  var volumes:List[VolumeInstance] = List()
  var boundInstanceId: String = null
  def parent = parentGroup
  def path = {
    val result = parentGroup.path + "[%d]".format(idx)
    result
  }
  def children = volumes


  def resolveScripts(args: Map[String, Map[String, ScriptArgumentValue]]): Map[String, Map[String, List[String]]] = {
    ScriptResolver.resolveScripts(this, args)
  }

}