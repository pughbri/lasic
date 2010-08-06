package com.lasic.model

import com.lasic.values.{NodeProperties, ScaleGroupProperties}

/**
 *
 * @author Brian Pugh
 */

class ScaleGroupConfiguration extends NodeProperties with ScaleGroupProperties with Pathable {
  var parentSystemInstance: ScaleGroupInstance = null

  def parent = parentSystemInstance
  def path = {parent.path + "/scale-group-configuration['" + name + "']"}
  def children = List()

  override def toString = this.getClass().getSimpleName() + ": " + name + children.mkString(", ")


}