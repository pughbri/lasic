package com.lasic.model

import com.lasic.values.{ResolvedScriptDefinition, ScriptDefinition, ScriptArgument}

/**
 * Created by IntelliJ IDEA.
 * User: lmonson
 * Date: May 12, 2010
 * Time: 4:59:14 PM
 * To change this template use File | Settings | File Templates.
 */

class NodeInstance(val parentGroup:NodeGroup,val idx:Int) extends Pathable  with VMHolder {

  var volumes:List[VolumeInstance] = List()
  var boundInstanceId: String = null
  def parent = parentGroup
  def path = {
    val result = parentGroup.path + "[%d]".format(idx)
    result
  }
  def children = volumes


  def resolveScripts(args: List[ScriptDefinition]): List[ResolvedScriptDefinition] = {
    val lasicEnvVars = ScriptArgument("NAME", LiteralArgumentValue(parent.name)) :: ScriptArgument("INDEX", LiteralArgumentValue(idx.toString)) :: Nil
    val argsWithEnvVars = args map (scriptDef => ScriptDefinition(scriptDef.scriptName, scriptDef.scriptArguments ::: lasicEnvVars))
    ScriptResolver.resolveScripts(this, argsWithEnvVars )
  }

}