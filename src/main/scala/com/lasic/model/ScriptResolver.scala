package com.lasic.model

import com.lasic.values.{ResolvedScriptDefinition, ScriptArgument, ResolvedScriptArgument, ScriptDefinition}

/**
 *   Utility for resolving paths to private DNS names in "Script" statements of the lasic script
 * @author Brian Pugh
 */

object ScriptResolver {

  /**
   * args(0) = variable name, args(1) = value
   */
  //private def resolveScriptArguments(pathable: Pathable, args: Map[String, ArgumentValue]): Map[String, List[String]] = {
  private def resolveScriptArguments(pathable: Pathable, scriptArgs: List[ScriptArgument]): List[ResolvedScriptArgument] = {
    val resolvedArgs = scriptArgs map {
      scriptArg => {
        val resolvedArg: List[ResolvedArgumentValue] = scriptArg.argValue match  {
          case x: LiteralArgumentValue => List(ResolvedArgumentValue(x.literal))
          case x: PathArgumentValue => {
            val path = x.literal
            val pathables = pathable find(path)
            val value = pathables map {
              targetPathable => (ResolvedArgumentValue(resolvePathable(targetPathable, path, pathable)) )
            }
            value
          }
        }
        ResolvedScriptArgument(scriptArg.argName, resolvedArg)
      }
    }
    resolvedArgs
  }

  def resolvePathable(pathableToResolve: Pathable, path: String, sourcePathable: Pathable): String = {
    pathableToResolve match {
      case node: NodeInstance => node.vmPrivateDns
      case scaleGroup: ScaleGroupInstance => scaleGroup.cloudName
      case lb: LoadBalancerInstance => lb.cloudName
      case _ => throw new Exception("path [" + path + "] in declaration of [" + sourcePathable.path + "] does not resolve to a node, scale group or load balancer")
    }
  }

  /**
   * Resolves an Argument value.  If the argument value is a PathArgumentValue, it must
   * resolve to exactly 1 pathable.
   */
  def resolveArgumentValue(pathable: Pathable, argValue: ArgumentValue): String = {
    argValue match {
      case literalVal: LiteralArgumentValue => literalVal.literal
      case pathArgVal: PathArgumentValue => {
        val path = pathArgVal.literal
        val targetPathables = pathable.find(path)
        require(targetPathables.size == 1,
          "path [" + path + "] in declartion of [" + pathable.path + "] must resolve to a single node, scale group or load balancer.  Resolved to [" + targetPathables.mkString(", ") + "]")
        resolvePathable(targetPathables(0), path, pathable)
      }
    }
  }

  /**
   * return each map entry has the script name and map of variable name to resolved values
   */
  //def resolveScripts(pathable: Pathable, args: Map[String, Map[String, ArgumentValue]]): Map[String, Map[String, List[String]]] = {
  def resolveScripts(pathable: Pathable, args: List[ScriptDefinition]): List[ResolvedScriptDefinition] = {
    args map {
      //scriptTuple => (scriptTuple._1, resolveScriptArguments(pathable, scriptTuple._2))
      scriptTuple => {
        ResolvedScriptDefinition(scriptTuple.scriptName, resolveScriptArguments(pathable, scriptTuple.scriptArguments))
      }
    }
  }
}