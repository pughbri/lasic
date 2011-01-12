package com.lasic.model


trait ArgumentValue {
  val literal:String
}

case class LiteralArgumentValue(val literal:String) extends ArgumentValue
case class PathArgumentValue(val literal:String) extends ArgumentValue
case class ResolvedArgumentValue(val literal:String) extends ArgumentValue
