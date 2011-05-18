package com.lasic

import interpreter.Verb
import com.lasic.util._

/**
 *
 * @author Brian Pugh
 */

class ShutdownThread(val verb: Verb) extends Thread {
  private var callTerminate = true
  private val lock = "lock"

  override def run() {
    lock synchronized {
      if (callTerminate) {
        PrintLine("Abnormally Terminating LASIC. Giving verb a change to cleanup...");
        verb.terminate
        PrintLine("LASIC terminated");
      }
    }
  }

  def verbCompleted {
    lock synchronized {
      callTerminate = false
    }
  }


}