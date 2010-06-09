package com.lasic.cloud.ssh

/**
 *
 * User: Brian Pugh
 * Date: May 19, 2010
 */

class ConnectException(message: String, t: Throwable) extends Exception(message, t) {
  def this() = this (null, null)
  def this(message: String) = this(message, null)
  def this(t: Throwable) = this(null, t)
}