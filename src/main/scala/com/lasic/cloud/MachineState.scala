package com.lasic.cloud

/**
 *
 * User: Brian Pugh
 * Date: May 14, 2010
 */

object MachineState extends Enumeration {
  type MachineState = Value
  val Unknown = Value("unknown")
  val Pending = Value("pending")
  val Running = Value("running")
  val ShuttingDown = Value("shutting-down")
  val Rebooting = Value("rebooting")
  val Terminated = Value("terminated")
}