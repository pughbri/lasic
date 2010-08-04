package com.lasic.model

import com.lasic.values.{NodeProperties, ScaleGroupProperties}

/**
 * 
 * @author Brian Pugh
 */

class ScaleGroupInstance extends NodeProperties  with ScaleGroupProperties with Pathable {
  var parentSystemInstance:SystemInstance = null

  def parent = parentSystemInstance
  def path = { parent.path +"/scale-group['" + name+"']" }
  def children = List()

  override def toString = this.getClass().getSimpleName() + ": " + name + children.mkString(", ")

  var triggers: List[TriggerInstance]    = List()
}