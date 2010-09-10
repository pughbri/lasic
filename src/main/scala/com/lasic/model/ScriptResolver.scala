package com.lasic.model

/**
 *   Utility for resolving paths to private DNS names in "Script" statements of the lasic script
 * @author Brian Pugh
 */

object ScriptResolver {
  /**
   * args(0) = variable name, args(1) = value
   */
  private def resolveScriptArguments(pathable: Pathable, args: Map[String, ScriptArgumentValue]): Map[String, List[String]] = {
    Map.empty ++ args.map {
      argTuple: Tuple2[String, ScriptArgumentValue] =>
        val values: List[String] = argTuple._2 match {
          case x: LiteralScriptArgumentValue => List(x.literal)
          case x: PathScriptArgumentValue => {
            val path = x.literal
            val pathables = pathable.find(path)
            val value = pathables.map {
              targetPathable => {
                targetPathable match {
                  case node: NodeInstance => node.vmPrivateDns
                  case scaleGroup: ScaleGroupInstance => scaleGroup.cloudName
                  case _ => throw new Exception("path [" + path + "] in declaration of [" + pathable.path + "] does not resolve to a node or scale group")
                }
              }
            }
            value
          }
        }
        (argTuple._1, values)
    }
  }

  /**
   * return each map entry has the script name and map of variable name to resolved values
   */
  def resolveScripts(pathable: Pathable, args: Map[String, Map[String, ScriptArgumentValue]]): Map[String, Map[String, List[String]]] = {
    Map.empty ++ args.map {
      scriptTuple => (scriptTuple._1, resolveScriptArguments(pathable, scriptTuple._2))
    }
  }
}