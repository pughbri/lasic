package com.lasic

import cloud.ssh.SshSession
import cloud.{MachineDescription, LaunchConfiguration}
import java.io.File
import java.lang.String

/**
 * User: Brian Pugh
 * Date: May 10, 2010
 */

trait VM {
  val cloud: Cloud
  val launchConfiguration: LaunchConfiguration
  var machineDescription: MachineDescription = null

  /** lasic configuration directory.  Key files should be in this directory **/
  var baseLasicDir: String = "~"

  def start() {
    cloud.start(Array(this))
  }

  def reboot() {
    cloud.reboot(Array(this))
  }

  def shutdown() {
    cloud.terminate(Array(this))
  }

  def copyTo(sourceFile: File, destinationAbsPath: String)
  
  def execute(executableAbsPath: String)

  protected def createSshSession: SshSession = {
    new SshSession()
  }

  def withSshSession(callback: SshSession => Unit): Unit = {
    val session: SshSession = createSshSession
    try {
      if (machineDescription == null) {
        throw new IllegalStateException("VM hasn't been launched yet.  Cannot operate on it")
      }
      session.connect(machineDescription.publicDNS, launchConfiguration.userName, new File(baseLasicDir, launchConfiguration.key))
      callback(session)
    }
    finally {
      //todo: if session is connected
      session.disconnect
    }

  }

}