package com.lasic.model

import collection.mutable.ListBuffer
import com.lasic.parser.ast.ASTSystem
import collection.immutable.HashMap


/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 12, 2010
 * Time: 4:57:46 PM
 * To change this template use File | Settings | File Templates.
 */


class SystemGroup extends SystemProperties {
  var parent:SystemGroup = null
  var instances = List[SystemInstance]()
  var subsystems = List[SystemGroup]()



}