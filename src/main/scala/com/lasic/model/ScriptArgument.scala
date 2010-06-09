package com.lasic.model


trait ScriptArgument {
  val literal:String
  def asCmdlineArgument(node:NodeInstance):String
}

case class LiteralScriptArgument(val literal:String) extends ScriptArgument {
  def asCmdlineArgument(node:NodeInstance):String = literal
}

object LiteralScriptArgument {
  implicit def asString(l:LiteralScriptArgument) = l.literal
  implicit def asLiteralScriptArgument(literal:String) = new LiteralScriptArgument(literal)
}


case class PathScriptArgument(val literal:String) extends ScriptArgument {
  def asCmdlineArgument(node:NodeInstance):String = {
    "nothing"
  }
}

object PathScriptArgument {
  implicit def asString(path:PathScriptArgument):String = path.literal
}
