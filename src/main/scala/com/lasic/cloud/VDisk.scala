package com.lasic

import cloud.ssh.{AuthFailureException, ConnectException, SshSession}
import cloud.{AttachmentInfo, VolumeInfo, MachineState, LaunchConfiguration}
import java.lang.String
import util.Logging

/**
 * User: Brian Pugh
 * Date: May 10, 2010
 */

trait VDisk extends Logging {
  protected val cloud: Cloud
  protected val size: Int
  protected val snap: String
  protected val zone: String

  val instanceId: String = "?"

}

object VDisk {
  object VDiskState extends Enumeration {
    type VDiskState = Value
    val Unknown = Value("unknown")
    val Available = Value("available")
    val InUse = Value("in-use")
    val Deleting = Value("deleting")

    def string2State(s: String): VDiskState.VDiskState = {
      for (ms <- iterator) {
        if (s.toLowerCase.equals(ms.toString.toLowerCase)) return ms
      }
      Unknown
    }
  }
}