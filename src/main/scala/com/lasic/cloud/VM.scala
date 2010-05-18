package com.lasic

import cloud.ssh.SshSession
import cloud.{MachineState, MachineDescription, LaunchConfiguration}
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

  /** lasic configuration directory.  Key files should be in this directory fixed to home dire but oveerridable with prop**/
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

  def getState(): MachineState.Value = {
     cloud.getState(this)
  }

  def copyTo(sourceFile: File, destinationAbsPath: String)
  
  def execute(executableAbsPath: String)

  protected def createSshSession: SshSession = {
    new SshSession()
  }

  def withSshSession(callback: SshSession => Unit): Unit = {
    val session: SshSession = createSshSession
    try {
      if (!(getState == MachineState.Running)) {
        throw new IllegalStateException("VM is in state " + getState + ".  Cannot operate on it unless it is Running")
      }
       if (machineDescription == null) {
         throw new IllegalStateException("VM in unexpected state " + getState + " with no public DNS name.")
       }
      session.connect(machineDescription.publicDNS, launchConfiguration.userName, new File(baseLasicDir, launchConfiguration.key))
      callback(session)
    }
    finally {
      //todo: if session is connected, then disconnect
      session.disconnect
    }

  }

}