package com.lasic.model

import com.lasic.parser.ast.NodeProperties

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 12, 2010
 * Time: 4:58:58 PM
 * To change this template use File | Settings | File Templates.
 */

class NodeGroup extends NodeProperties with Pathable {
  var parentSystemInstance:SystemInstance = null
  var instances:List[NodeInstance] = List()

  def parent = parentSystemInstance
  def path = { parent.path +"/" + name}
  def children = instances
  
}