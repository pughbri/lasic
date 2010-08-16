package com.lasic.cloud

/**
 * 
 * @author Brian Pugh
 */

object ImageState extends Enumeration {
  type ImageState = Value
  val Unknown = Value("unknown")
  val Pending = Value("pending")
  val Available = Value("available")
}