package com.lasic.model

import com.lasic.cloud.VM
import com.lasic.values._

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
  var loadBalancers = List[ArgumentValue]()

  def resolveScripts(args: List[ScriptDefinition]): List[ResolvedScriptDefinition] = {
    ScriptResolver.resolveScripts(this, args)
  }
}