package com.lasic.util

import java.io.PrintStream

object PrintLine {

  var printStream: PrintStream = System.out
  
  def apply(obj: Any) = {
    printStream.println(obj)
  }
  
}
