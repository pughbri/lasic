package com.lasic.values

import com.lasic.model.ArgumentValue

/**
 * 
 * @author Brian Pugh
 */

class BaseAction {
  var name: String = null
  var scpMap = Map[String,String]()
  var scriptMap = Map[String,Map[String,ArgumentValue]]()
  var ipMap = Map[Int,String]()
}