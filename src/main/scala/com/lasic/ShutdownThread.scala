package com.lasic

import interpreter.Verb

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
        System.out.println("Abnormally Terminating LASIC. Giving verb a change to cleanup...");
        verb.terminate
        System.out.println("LASIC terminated");
      }
    }
  }

  def verbCompleted {
    lock synchronized {
      callTerminate = false
    }
  }


}