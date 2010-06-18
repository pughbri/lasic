package com.lasic.values

import com.lasic.model.ScriptArgumentValue

/**
 * 
 * @author Brian Pugh
 */

class BaseAction {
  var name: String = null
  var scpMap = Map[String,String]()
  var scriptMap = Map[String,Map[String,ScriptArgumentValue]]()
}