package com.lasic.model

import collection.mutable.ListBuffer
import com.lasic.parser.ast.ASTSystem
import collection.immutable.HashMap
import com.lasic.values.SystemProperties


/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 12, 2010
 * Time: 4:57:46 PM
 * To change this template use File | Settings | File Templates.
 */


class SystemGroup(val systemOrProgramParent:Pathable) extends SystemProperties with Pathable {
  // var systemOrProgramParent:Any = null;

  def parent:Pathable = systemOrProgramParent

  var instances = List[SystemInstance]()

  def children:List[SystemInstance] = instances

  def path:String = {
    parent match {
      case prog:LasicProgram => prog.path + "system['%s']".format(name)
      case group:SystemInstance => group.path + "/system['%s']".format(name)
      case _ => name
    }
  }

}