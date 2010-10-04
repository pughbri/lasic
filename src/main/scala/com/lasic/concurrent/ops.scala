package com.lasic.concurrent

import com.lasic.util.Logging

/**
 *
 * @author Brian Pugh
 */

object ops extends Logging {
  def spawn(task: String)(p: => Unit): Unit = {
    concurrent.ops.spawn {
      try {
        p
      }
      catch {
        case t: Throwable => logger.error("Error performing task [" + task + "]", t)
      }
    }
  }
}