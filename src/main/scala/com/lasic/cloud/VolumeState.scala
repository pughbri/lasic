package com.lasic.cloud

import com.lasic.util._
/**
 *
 * User: Brian Pugh
 * Date: May 14, 2010
 */

object VolumeState extends Enumeration {
  type VolumeState = Value
  val Unknown = Value("unknown")
  val Available = Value("available")
  val InUse = Value("in-use")
  val Deleting = Value("deleting")
  
  def string2State(s: String): VolumeState.Value = {
    PrintLine( "State scanned is: " + s)
    for (ms <- iterator) {
      if (s.toLowerCase.equals(ms.toString.toLowerCase)) return ms
    }
    Unknown
  }
}