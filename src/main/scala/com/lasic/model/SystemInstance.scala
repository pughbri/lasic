package com.lasic.model

import collection.mutable.ListBuffer

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 12, 2010
 * Time: 4:58:07 PM
 * To change this template use File | Settings | File Templates.
 */

class SystemInstance(_parent:SystemGroup, index:Int) {
  val parent:SystemGroup = _parent 
  var nodegroups = List[NodeGroup]() 

}