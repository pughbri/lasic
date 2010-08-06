package com.lasic.model

import com.lasic.values.{BaseAction, NodeProperties, ScaleGroupProperties}

/**
 * 
 * @author Brian Pugh
 */

class ScaleGroupInstance extends Pathable {
  var parentSystemInstance:SystemInstance = null
  var configurations:List[ScaleGroupConfiguration] = List()

  def parent = parentSystemInstance
  def path = { parent.path +"/scale-group['" + name+"']" }
  def children = configurations

  override def toString = this.getClass().getSimpleName() + ": " + name + children.mkString(", ")

  var name = ""
  var triggers: List[TriggerInstance]    = List()
  var volumes = List[Map[String,String]]()
  var actions = List[BaseAction]()
}