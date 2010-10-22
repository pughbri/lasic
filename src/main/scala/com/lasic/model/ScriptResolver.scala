package com.lasic.model

/**
 *   Utility for resolving paths to private DNS names in "Script" statements of the lasic script
 * @author Brian Pugh
 */

object ScriptResolver {

  /**
   * args(0) = variable name, args(1) = value
   */
  private def resolveScriptArguments(pathable: Pathable, args: Map[String, ArgumentValue]): Map[String, List[String]] = {
    Map.empty ++ args.map {
      argTuple: Tuple2[String, ArgumentValue] =>
        val values: List[String] = argTuple._2 match {
          case x: LiteralArgumentValue => List(x.literal)
          case x: PathArgumentValue => {
            val path = x.literal
            val pathables = pathable.find(path)
            val value = pathables.map {
              targetPathable => {
                resolvePathable(targetPathable, path, pathable)
              }
            }
            value
          }
        }
        (argTuple._1, values)
    }
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
   * Resoves an Argument value.  If the argument value is a PathArgumentValue, it must
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
  def resolveScripts(pathable: Pathable, args: Map[String, Map[String, ArgumentValue]]): Map[String, Map[String, List[String]]] = {
    Map.empty ++ args.map {
      scriptTuple => (scriptTuple._1, resolveScriptArguments(pathable, scriptTuple._2))
    }
  }
}