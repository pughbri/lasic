package com.lasic.model

/**
 *   Utility for resolving paths to private DNS names in "Script" statements of the lasic script
 * @author Brian Pugh
 */

object ScriptResolver {
       /**
   * args(0) = variable name args(1) = value
   */
  private def resolveScriptArguments(pathable: Pathable, args: Map[String, ScriptArgumentValue]): Map[String, List[String]] = {
    Map.empty ++ args.map {
      argTuple: Tuple2[String, ScriptArgumentValue] =>
        val values: List[String] = argTuple._2 match {
          case x: LiteralScriptArgumentValue => List(x.literal)
          case x: PathScriptArgumentValue => {
            val a = x.literal
            val b = pathable.findNodes(a)
            val c = b.map { _.vmPrivateDns }
            c
          }
        }
        (argTuple._1, values)
    }
  }

  def resolveScripts(pathable: Pathable, args: Map[String, Map[String, ScriptArgumentValue]]): Map[String, Map[String, List[String]]] = {
    Map.empty ++ args.map {
      scriptTuple => (scriptTuple._1, resolveScriptArguments(pathable, scriptTuple._2))
    }
  }
}