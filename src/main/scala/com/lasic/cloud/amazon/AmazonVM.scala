package com.lasic.cloud.amazon

import com.lasic.{Cloud, VM}
import com.lasic.cloud.LaunchConfiguration
import java.io.File
/**
 * User: Brian Pugh
 * Date: May 11, 2010
 */

class AmazonVM(val cloudInst: Cloud, val lc: LaunchConfiguration, val timeout: Int) extends VM {
  def this(cloudInst: Cloud, lc: LaunchConfiguration) = this (cloudInst, lc, 10)

  val cloud: Cloud = cloudInst
  val launchConfiguration: LaunchConfiguration = lc

  override def copyTo(sourceFile: File, destinationAbsPath: String) {
    withSshSession(timeout) {
      session => session.sendFile(sourceFile, destinationAbsPath)
    }
  }

  override def execute(executableAbsPath: String) {
    withSshSession(timeout) {
      session => session.sendCommand(executableAbsPath)
    }
  }


}