package com.lasic.cloud.ssh

/**
 *
 * @author Brian Pugh
 */

class AuthFailureException(message: String, t: Throwable) extends Exception(message, t) {
  def this() = this (null, null)
  def this(message: String) = this (message, null)
  def this(t: Throwable) = this (null, t)
}