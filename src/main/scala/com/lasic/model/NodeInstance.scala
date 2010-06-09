package com.lasic.model

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 12, 2010
 * Time: 4:59:14 PM
 * To change this template use File | Settings | File Templates.
 */

class NodeInstance(val parentGroup:NodeGroup,idx:Int) extends Pathable {
  var volumes:List[VolumeInstance] = List()
  def parent = parentGroup
  def path = { parentGroup.name + "[%d]".format(idx) }
  def children = volumes

}