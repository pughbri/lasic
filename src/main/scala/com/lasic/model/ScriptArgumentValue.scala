package com.lasic.model


trait ScriptArgumentValue {
  val literal:String
  def asCmdlineArgument(node:NodeInstance):String
}

case class LiteralScriptArgumentValue(val literal:String) extends ScriptArgumentValue {
  def asCmdlineArgument(node:NodeInstance):String = literal
}

object LiteralScriptArgumentValue {
  implicit def asString(l:LiteralScriptArgumentValue) = l.literal
  implicit def asLiteralScriptArgument(literal:String) = new LiteralScriptArgumentValue(literal)
}


case class PathScriptArgumentValue(val literal:String) extends ScriptArgumentValue {
  def asCmdlineArgument(node:NodeInstance):String = {
    
    "nothing"
  }
}

object PathScriptArgumentValue {
  implicit def asString(path:PathScriptArgumentValue):String = path.literal
}
