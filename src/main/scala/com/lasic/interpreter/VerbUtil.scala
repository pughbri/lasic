package com.lasic.interpreter

/**
 * 
 * @author Brian Pugh
 */

object VerbUtil {
  def showValue(x: Any) = x match {
    case Some(s) => s
    case None => "?"
    case y => y
  }
}