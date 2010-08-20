package com.lasic.cloud.amazon

import com.lasic.{Cloud, VM}
import com.lasic.cloud.LaunchConfiguration
import java.io.File
import collection.immutable.Map
import java.lang.String
import com.lasic.cloud.ssh.BashPreparedScriptExecution

/**
 * User: Brian Pugh
 * Date: May 11, 2010
 */

class AmazonVM(val cloud: Cloud, val launchConfiguration: LaunchConfiguration, val timeout: Int) extends VM {
  def this(cloud: Cloud, launchConfiguration: LaunchConfiguration) = this (cloud, launchConfiguration, 10)

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


  def executeScript(scriptAbsPath: String, variables: Map[String, List[String]]) = {
    withSshSession(timeout) {
      session => new BashPreparedScriptExecution(session, scriptAbsPath, variables).execute()
    }
  }
}