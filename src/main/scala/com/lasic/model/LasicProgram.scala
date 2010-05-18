package com.lasic.model

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 13, 2010
 * Time: 3:24:50 PM
 * To change this template use File | Settings | File Templates.
 */

class LasicProgram extends Pathable {
  var rootGroup:SystemGroup = null  
  def path = "/"
  def parent = null
  def children:List[SystemGroup] = List(rootGroup)
}