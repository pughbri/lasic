package com.lasic.model

import com.lasic.values.{NodeProperties, ScaleGroupProperties}

/**
 *
 * The is the configuration that will be used to create the "prototype" VM which will then be used as the image for
 * the scaling group.
 * @author Brian Pugh
 */

class ScaleGroupConfiguration extends NodeProperties with ScaleGroupProperties with Pathable {
  var parentScaleGroupInstance: ScaleGroupInstance = null

  def parent = parentScaleGroupInstance
  def path = {parent.path + "/configuration['" + name + "']"}
  def children = List()

  var cloudName = ""

}