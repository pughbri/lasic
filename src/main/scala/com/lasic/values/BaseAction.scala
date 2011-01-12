package com.lasic.values

import com.lasic.model.{ResolvedArgumentValue, ArgumentValue}

/**
 * 
 * @author Brian Pugh
 */

case class ScriptDefinition(scriptName: String, scriptArguments: List[ScriptArgument])
case class ScriptArgument(argName: String, argValue: ArgumentValue)

case class ResolvedScriptDefinition(scriptName: String, scriptArguments: List[ResolvedScriptArgument])
case class ResolvedScriptArgument(argName: String, argValues: List[ResolvedArgumentValue])

class BaseAction {
  var name: String = null
  var scpMap = Map[String,String]()
  var scriptDefinitions = List[ScriptDefinition]()
  var ipMap = Map[Int,String]()
}