package com.lasic.cloud.amazon

import com.lasic.{Cloud, VM}
import com.lasic.cloud.LaunchConfiguration
import com.jcraft.jsch.Session
import java.io.File
import com.lasic.cloud.ssh.SshSession

/**
 * User: Brian Pugh
 * Date: May 11, 2010
 */

class AmazonVM(val cloudInst: Cloud, val lc: LaunchConfiguration) extends VM {
  val cloud: Cloud = cloudInst
  val launchConfiguration: LaunchConfiguration = lc

  override def copyTo(sourceFile: File, destinationAbsPath: String) {
    withSshSession{
      session => session.sendFile(sourceFile, destinationAbsPath)
    }
  }

  override def execute(executableAbsPath: String) {
     withSshSession{
      session => session.sendCommand(executableAbsPath)
    }
  }


}