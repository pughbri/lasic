package com.lasic.cloud

/**
 *
 * User: Brian Pugh
 * Date: May 14, 2010
 */

object MachineState extends Enumeration {
  val Unknown = Value("unknown")
  val Pending = Value("pending")
  val Running = Value("running")
  val ShuttingDown = Value("shutting-down")
  val Rebooting = Value("rebooting")
  val Terminated = Value("terminated")

  def string2State(s: String): MachineState.Value = {
    for (ms <- elements) {
      if (s.toLowerCase.equals(ms.toString.toLowerCase)) return ms
    }
    Unknown
  }
}