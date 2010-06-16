package com.lasic.util

import org.slf4j._


/**
 *
 * User: Brian Pugh
 * Date: Jun 8, 2010
 *
 * pulled from http://scalax.scalaforge.org
 */
trait Logging {
  val logger = Logging.getLogger(this)
}

object Logging {
  def loggerNameForClass(className: String) = {
    if (className endsWith "$") className.substring(0, className.length - 1)
    else className
  }

  def getLogger(logging: AnyRef) = LoggerFactory.getLogger(loggerNameForClass(logging.getClass.getName))
}