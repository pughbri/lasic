package com.lasic.model

import com.lasic.values.{BaseAction, NodeProperties, ScaleGroupProperties}
import com.lasic.cloud.VM

/**
 * 
 * @author Brian Pugh
 */

class ScaleGroupInstance extends Pathable with VMHolder {
  var parentSystemInstance:SystemInstance = null
  var configuration: ScaleGroupConfiguration = null

  def parent = parentSystemInstance
  def path = { parent.path +"/scale-group['" + localName+"']" }
  def children = List(configuration)

  var localName = ""
  var cloudName = ""
  var triggers: List[TriggerInstance]    = List()
  var volumes = List[Map[String,String]]()
  var actions = List[BaseAction]()

  def resolveScripts(args: Map[String, Map[String, ScriptArgumentValue]]): Map[String, Map[String, List[String]]] = {
    ScriptResolver.resolveScripts(this, args)
  }
}