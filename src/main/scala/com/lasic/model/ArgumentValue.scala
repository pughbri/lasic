package com.lasic.model


trait ArgumentValue {
  val literal:String
  def asCmdlineArgument(node:NodeInstance):String
}

case class LiteralArgumentValue(val literal:String) extends ArgumentValue {
  def asCmdlineArgument(node:NodeInstance):String = literal
}

object LiteralArgumentValue {
  implicit def asString(l:LiteralArgumentValue) = l.literal
  implicit def asLiteralScriptArgument(literal:String) = new LiteralArgumentValue(literal)
}


case class PathArgumentValue(val literal:String) extends ArgumentValue {
  def asCmdlineArgument(node:NodeInstance):String = {
    
    "nothing"
  }
}

object PathArgumentValue {
  implicit def asString(path:PathArgumentValue):String = path.literal
}
