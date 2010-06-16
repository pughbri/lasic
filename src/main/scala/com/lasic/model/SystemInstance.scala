package com.lasic.model

import collection.mutable.ListBuffer

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 12, 2010
 * Time: 4:58:07 PM
 * To change this template use File | Settings | File Templates.
 */

class SystemInstance(var parent:SystemGroup, index:Int) extends Pathable {
  var nodegroups = List[NodeGroup]()
  var subsystems = List[SystemGroup]()
  
  def path = { parent.path +"[%d]".format(index)}
  def children =  nodegroups ::: subsystems

  override def toString = this.getClass().getSimpleName() + ": " + path + children.mkString(", ")

}